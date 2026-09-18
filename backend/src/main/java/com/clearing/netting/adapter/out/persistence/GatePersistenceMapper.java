package com.clearing.netting.adapter.out.persistence;

import com.clearing.netting.adapter.out.persistence.entity.GateCheckConfigJpaEntity;
import com.clearing.netting.adapter.out.persistence.entity.GateCheckResultJpaEntity;
import com.clearing.netting.adapter.out.persistence.entity.GateFindingJpaEntity;
import com.clearing.netting.adapter.out.persistence.entity.GateRunJpaEntity;
import com.clearing.netting.domain.model.GateCheckConfig;
import com.clearing.netting.domain.model.GateCheckResult;
import com.clearing.netting.domain.model.GateFinding;
import com.clearing.netting.domain.model.GateRun;

import java.util.ArrayList;
import java.util.List;

/**
 * 门禁聚合（run -> results -> findings）的持久化映射，维护父子反向引用。
 */
final class GatePersistenceMapper {

    private GatePersistenceMapper() {
    }

    static GateRunJpaEntity toEntity(GateRun run) {
        GateRunJpaEntity root = new GateRunJpaEntity();
        root.setRunId(run.getRunId());
        root.setSettleDate(run.getSettleDate());
        root.setOperator(run.getOperator());
        root.setStatus(run.getStatus());
        root.setCreatedAt(run.getCreatedAt());

        List<GateCheckResultJpaEntity> resultEntities = new ArrayList<>();
        List<GateCheckResult> results = run.getResults();
        for (int i = 0; i < results.size(); i++) {
            GateCheckResult r = results.get(i);
            GateCheckResultJpaEntity re = new GateCheckResultJpaEntity();
            re.setRun(root);
            re.setCheckType(r.getCheckType());
            re.setStatus(r.getStatus());
            re.setMessage(trim(r.getMessage(), 1024));
            re.setPosition(i);

            List<GateFindingJpaEntity> findingEntities = new ArrayList<>();
            List<GateFinding> findings = r.getFindings();
            for (int j = 0; j < findings.size(); j++) {
                findingEntities.add(toEntity(findings.get(j), re, j));
            }
            re.setFindings(findingEntities);
            resultEntities.add(re);
        }
        root.setResults(resultEntities);
        return root;
    }

    private static GateFindingJpaEntity toEntity(GateFinding f, GateCheckResultJpaEntity parent, int position) {
        GateFindingJpaEntity e = new GateFindingJpaEntity();
        e.setResult(parent);
        e.setTargetType(f.getTargetType());
        e.setTargetId(f.getTargetId());
        e.setMemberId(f.getMemberId());
        e.setMemberName(trim(f.getMemberName(), 128));
        e.setCurrency(f.getCurrency());
        e.setAmount(f.getAmount());
        e.setSettleDate(f.getSettleDate());
        e.setDetail(trim(f.getDetail(), 1024));
        e.setPosition(position);
        return e;
    }

    static GateRun toDomain(GateRunJpaEntity root) {
        List<GateCheckResult> results = new ArrayList<>();
        for (GateCheckResultJpaEntity re : root.getResults()) {
            List<GateFinding> findings = new ArrayList<>();
            for (GateFindingJpaEntity fe : re.getFindings()) {
                findings.add(new GateFinding(
                        fe.getTargetType(),
                        fe.getTargetId(),
                        fe.getMemberId(),
                        fe.getMemberName(),
                        fe.getCurrency(),
                        fe.getAmount(),
                        fe.getSettleDate(),
                        fe.getDetail()));
            }
            results.add(new GateCheckResult(
                    re.getCheckType(),
                    re.getStatus(),
                    re.getMessage(),
                    findings));
        }
        return new GateRun(
                root.getRunId(),
                root.getSettleDate(),
                root.getOperator(),
                root.getCreatedAt(),
                root.getStatus(),
                results);
    }

    static GateCheckConfig toDomain(GateCheckConfigJpaEntity e) {
        return new GateCheckConfig(e.getCheckType(), e.isEnabled());
    }

    static GateCheckConfigJpaEntity toEntity(GateCheckConfig c) {
        GateCheckConfigJpaEntity e = new GateCheckConfigJpaEntity();
        e.setCheckType(c.getCheckType());
        e.setEnabled(c.isEnabled());
        return e;
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
