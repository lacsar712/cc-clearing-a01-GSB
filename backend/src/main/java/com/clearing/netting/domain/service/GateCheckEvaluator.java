package com.clearing.netting.domain.service;

import com.clearing.netting.domain.model.GateCheckResult;
import com.clearing.netting.domain.model.GateCheckType;
import com.clearing.netting.domain.model.GateFinding;
import com.clearing.netting.domain.model.Member;
import com.clearing.netting.domain.model.MemberStatus;
import com.clearing.netting.domain.model.NettingRun;
import com.clearing.netting.domain.model.NettingRunStatus;
import com.clearing.netting.domain.model.TradeObligation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 日终门禁纯领域检查器，无 IO 依赖：输入当日数据快照，输出四项检查结果。
 */
public class GateCheckEvaluator {

    public List<GateCheckResult> evaluate(
            GateData data,
            Map<GateCheckType, Boolean> enabledByType) {

        List<GateCheckResult> results = new ArrayList<>();
        for (GateCheckType type : GateCheckType.values()) {
            boolean enabled = enabledByType.getOrDefault(type, Boolean.TRUE);
            if (!enabled) {
                results.add(GateCheckResult.skipped(type));
                continue;
            }
            results.add(switch (type) {
                case SUSPENDED_MEMBER_OPEN_OBLIGATION -> checkSuspendedMember(data);
                case SAME_DAY_MIXED_CURRENCY -> checkMixedCurrency(data);
                case NON_POSITIVE_AMOUNT -> checkNonPositiveAmount(data);
                case FAILED_BATCH_UNRESOLVED -> checkFailedBatches(data);
            });
        }
        return results;
    }

    private GateCheckResult checkSuspendedMember(GateData data) {
        List<GateFinding> findings = new ArrayList<>();
        for (TradeObligation o : data.openObligations()) {
            findings.addAll(suspendedSides(o, data));
        }
        if (findings.isEmpty()) {
            return GateCheckResult.pass(
                    GateCheckType.SUSPENDED_MEMBER_OPEN_OBLIGATION,
                    "当日 OPEN 义务涉及的会员均为 ACTIVE");
        }
        return GateCheckResult.fail(
                GateCheckType.SUSPENDED_MEMBER_OPEN_OBLIGATION,
                "存在 " + findings.size() + " 笔停用会员挂账的 OPEN 义务，请取消义务或重新启用会员",
                findings);
    }

    private List<GateFinding> suspendedSides(TradeObligation o, GateData data) {
        List<GateFinding> list = new ArrayList<>();
        addIfSuspended(list, o, o.getPayerMemberId(), "付款方", data);
        addIfSuspended(list, o, o.getPayeeMemberId(), "收款方", data);
        return list;
    }

    private void addIfSuspended(List<GateFinding> findings, TradeObligation o, String memberId, String role, GateData data) {
        Member m = data.membersById().get(memberId);
        if (m != null && m.getStatus() == MemberStatus.SUSPENDED) {
            findings.add(GateFinding.obligation(
                    o, memberId, m.getName(),
                    role + "会员「" + m.getName() + "」已停用（SUSPENDED），仍有 OPEN 义务"));
        }
    }

    private GateCheckResult checkMixedCurrency(GateData data) {
        Set<String> currencies = new LinkedHashSet<>();
        Map<String, Integer> countByCurrency = new LinkedHashMap<>();
        for (TradeObligation o : data.openObligations()) {
            currencies.add(o.getCurrency());
            countByCurrency.merge(o.getCurrency(), 1, Integer::sum);
        }
        if (currencies.size() <= 1) {
            return GateCheckResult.pass(
                    GateCheckType.SAME_DAY_MIXED_CURRENCY,
                    currencies.isEmpty() ? "当日无 OPEN 义务" : "当日 OPEN 义务币种唯一：" + currencies.iterator().next());
        }

        // 笔数最多的视为本次轧差主币种，其余币种的义务作为问题明细（并列时取字母序最早者，保证确定性）。
        String dominant = null;
        int dominantCount = -1;
        for (Map.Entry<String, Integer> e : countByCurrency.entrySet()) {
            if (e.getValue() > dominantCount
                    || (e.getValue() == dominantCount && (dominant == null || e.getKey().compareTo(dominant) < 0))) {
                dominant = e.getKey();
                dominantCount = e.getValue();
            }
        }

        List<GateFinding> findings = new ArrayList<>();
        for (TradeObligation o : data.openObligations()) {
            if (!o.getCurrency().equals(dominant)) {
                Member payer = data.membersById().get(o.getPayerMemberId());
                findings.add(GateFinding.obligation(
                        o,
                        o.getPayerMemberId(),
                        payer == null ? null : payer.getName(),
                        "本次轧差主币种为 " + dominant + "，该义务币种为 " + o.getCurrency() + "，请取消或改日处理"));
            }
        }
        return GateCheckResult.fail(
                GateCheckType.SAME_DAY_MIXED_CURRENCY,
                "同一交割日混用多个币种：" + String.join("、", currencies)
                        + "；请移除主币种 " + dominant + " 之外的 " + findings.size() + " 笔义务",
                findings);
    }

    private GateCheckResult checkNonPositiveAmount(GateData data) {
        List<GateFinding> findings = new ArrayList<>();
        for (TradeObligation o : data.openObligations()) {
            BigDecimal amount = o.getAmount();
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                Member payer = data.membersById().get(o.getPayerMemberId());
                findings.add(GateFinding.obligation(
                        o,
                        o.getPayerMemberId(),
                        payer == null ? null : payer.getName(),
                        "金额 " + (amount == null ? "为空" : amount.toPlainString()) + "，必须大于 0"));
            }
        }
        if (findings.isEmpty()) {
            return GateCheckResult.pass(GateCheckType.NON_POSITIVE_AMOUNT, "当日 OPEN 义务金额均为正数");
        }
        return GateCheckResult.fail(
                GateCheckType.NON_POSITIVE_AMOUNT,
                "存在 " + findings.size() + " 笔金额小于等于 0 的义务，请取消后重新录入",
                findings);
    }

    private GateCheckResult checkFailedBatches(GateData data) {
        List<GateFinding> findings = new ArrayList<>();
        for (NettingRun run : data.nettingRuns()) {
            if (run.getStatus() == NettingRunStatus.FAILED && !run.isAcknowledged()) {
                findings.add(GateFinding.run(
                        run,
                        "FAILED 批次尚未确认处理："
                                + (run.getFailureReason() == null ? "无失败原因" : run.getFailureReason())));
            }
        }
        if (findings.isEmpty()) {
            return GateCheckResult.pass(
                    GateCheckType.FAILED_BATCH_UNRESOLVED,
                    "当日无未处理的 FAILED 批次");
        }
        return GateCheckResult.fail(
                GateCheckType.FAILED_BATCH_UNRESOLVED,
                "存在 " + findings.size() + " 个未确认处理的 FAILED 批次，请在批次详情确认后再轧差",
                findings);
    }

    /**
     * 门禁检查所需的当日数据快照。
     */
    public record GateData(
            List<TradeObligation> openObligations,
            Map<String, Member> membersById,
            List<NettingRun> nettingRuns) {
    }
}
