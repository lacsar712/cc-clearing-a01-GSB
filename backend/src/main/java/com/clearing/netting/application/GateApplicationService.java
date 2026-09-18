package com.clearing.netting.application;

import com.clearing.netting.domain.exception.DomainException;
import com.clearing.netting.domain.model.GateCheckConfig;
import com.clearing.netting.domain.model.GateCheckResult;
import com.clearing.netting.domain.model.GateCheckType;
import com.clearing.netting.domain.model.GateRun;
import com.clearing.netting.domain.model.Member;
import com.clearing.netting.domain.model.NettingRun;
import com.clearing.netting.domain.model.TradeObligation;
import com.clearing.netting.domain.port.out.GateCheckConfigRepositoryPort;
import com.clearing.netting.domain.port.out.GateRunRepositoryPort;
import com.clearing.netting.domain.port.out.MemberRepositoryPort;
import com.clearing.netting.domain.port.out.NettingRunRepositoryPort;
import com.clearing.netting.domain.port.out.ObligationRepositoryPort;
import com.clearing.netting.domain.service.GateCheckEvaluator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GateApplicationService {

    private final GateRunRepositoryPort runRepository;
    private final GateCheckConfigRepositoryPort configRepository;
    private final ObligationRepositoryPort obligationRepository;
    private final MemberRepositoryPort memberRepository;
    private final NettingRunRepositoryPort nettingRunRepository;
    private final GateCheckEvaluator evaluator = new GateCheckEvaluator();

    public GateApplicationService(
            GateRunRepositoryPort runRepository,
            GateCheckConfigRepositoryPort configRepository,
            ObligationRepositoryPort obligationRepository,
            MemberRepositoryPort memberRepository,
            NettingRunRepositoryPort nettingRunRepository) {
        this.runRepository = runRepository;
        this.configRepository = configRepository;
        this.obligationRepository = obligationRepository;
        this.memberRepository = memberRepository;
        this.nettingRunRepository = nettingRunRepository;
    }

    @Transactional(readOnly = true)
    public List<GateCheckConfig> listConfigs() {
        Map<GateCheckType, GateCheckConfig> stored = new EnumMap<>(GateCheckType.class);
        for (GateCheckConfig c : configRepository.findAll()) {
            stored.put(c.getCheckType(), c);
        }
        List<GateCheckConfig> result = new ArrayList<>();
        for (GateCheckType type : GateCheckType.values()) {
            result.add(stored.getOrDefault(type, GateCheckConfig.enabledByDefault(type)));
        }
        return result;
    }

    @Transactional
    public GateCheckConfig setEnabled(GateCheckType type, boolean enabled) {
        GateCheckConfig config = configRepository.findByCheckType(type)
                .orElseGet(() -> GateCheckConfig.enabledByDefault(type));
        config.setEnabled(enabled);
        return configRepository.save(config);
    }

    @Transactional
    public GateRun runCheck(LocalDate settleDate, String operator) {
        if (settleDate == null) {
            throw new DomainException("INVALID_DATE", "settleDate is required");
        }

        Map<GateCheckType, Boolean> enabledByType = new EnumMap<>(GateCheckType.class);
        for (GateCheckConfig c : listConfigs()) {
            enabledByType.put(c.getCheckType(), c.isEnabled());
        }

        List<TradeObligation> opens = obligationRepository.findOpenBySettleDate(settleDate);
        List<NettingRun> runs = nettingRunRepository.findBySettleDateOrderByCreatedAtDesc(settleDate);

        Set<String> memberIds = new HashSet<>();
        for (TradeObligation o : opens) {
            memberIds.add(o.getPayerMemberId());
            memberIds.add(o.getPayeeMemberId());
        }
        Map<String, Member> membersById = new java.util.HashMap<>();
        for (Member m : memberRepository.findByIds(memberIds)) {
            membersById.put(m.getMemberId(), m);
        }

        GateCheckEvaluator.GateData data = new GateCheckEvaluator.GateData(opens, membersById, runs);
        List<GateCheckResult> results = evaluator.evaluate(data, enabledByType);

        GateRun run = GateRun.start(settleDate, operator);
        run.complete(results);
        return runRepository.save(run);
    }

    @Transactional(readOnly = true)
    public List<GateRun> listRuns() {
        return runRepository.findAllOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public GateRun getRun(String runId) {
        return runRepository.findById(runId)
                .orElseThrow(() -> new DomainException("GATE_RUN_NOT_FOUND", "gate run not found: " + runId));
    }

    /** 轧差页门禁状态：该交割日最近一次门禁是否通过（无运行记录视为未通过）。 */
    @Transactional(readOnly = true)
    public GateStatus statusFor(LocalDate settleDate) {
        if (settleDate == null) {
            return GateStatus.none();
        }
        return runRepository.findLatestBySettleDate(settleDate)
                .map(r -> new GateStatus(r.passed(), r.getRunId(), r.getStatus().name(), r.getCreatedAt()))
                .orElseGet(GateStatus::none);
    }

    public record GateStatus(boolean passed, String runId, String status, java.time.Instant createdAt) {
        static GateStatus none() {
            return new GateStatus(false, null, "NOT_RUN", null);
        }

        public boolean hasRun() {
            return runId != null;
        }
    }
}
