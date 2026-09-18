package com.clearing.netting.domain.service;

import com.clearing.netting.domain.model.GateCheckResult;
import com.clearing.netting.domain.model.GateCheckType;
import com.clearing.netting.domain.model.Member;
import com.clearing.netting.domain.model.MemberStatus;
import com.clearing.netting.domain.model.NettingRun;
import com.clearing.netting.domain.model.TradeObligation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 纯领域服务：日终门禁检查单。针对某个交割日逐项执行固定四类检查，
 * 输入由应用层按 businessDate 预取（OPEN 义务、相关会员、当日 FAILED 批次）。
 */
public class EndOfDayGateService {

    public List<GateCheckResult> runChecks(
            Set<GateCheckType> enabledChecks,
            List<TradeObligation> openObligations,
            Map<String, Member> membersById,
            List<NettingRun> failedRuns) {

        List<GateCheckResult> results = new ArrayList<>();
        for (GateCheckType type : GateCheckType.values()) {
            if (!enabledChecks.contains(type)) {
                results.add(GateCheckResult.disabled(type));
                continue;
            }
            List<String> failures = switch (type) {
                case SUSPENDED_MEMBER_OPEN -> suspendedMemberFailures(openObligations, membersById);
                case MIXED_CURRENCY_SAME_DAY -> mixedCurrencyFailures(openObligations);
                case NON_POSITIVE_AMOUNT -> nonPositiveAmountFailures(openObligations);
                case UNRESOLVED_FAILED_RUN -> failedRunFailures(failedRuns);
            };
            results.add(GateCheckResult.evaluated(type, failures));
        }
        return results;
    }

    private List<String> suspendedMemberFailures(
            List<TradeObligation> opens, Map<String, Member> membersById) {
        // 同一会员同一义务只报一次：按 义务ID+会员ID 去重
        Map<String, String> lines = new LinkedHashMap<>();
        for (TradeObligation o : opens) {
            checkSide(lines, o, o.getPayerMemberId(), "付款方", membersById);
            checkSide(lines, o, o.getPayeeMemberId(), "收款方", membersById);
        }
        return new ArrayList<>(lines.values());
    }

    private void checkSide(
            Map<String, String> lines,
            TradeObligation o,
            String memberId,
            String side,
            Map<String, Member> membersById) {
        Member member = membersById.get(memberId);
        if (member == null) {
            lines.put(o.getObligationId() + memberId,
                    "义务 " + o.getObligationId() + " 的" + side + "会员档案缺失: " + memberId);
        } else if (member.getStatus() == MemberStatus.SUSPENDED) {
            lines.put(o.getObligationId() + memberId,
                    "义务 " + o.getObligationId() + " 的" + side + "会员 " + member.getName()
                            + "(" + memberId + ") 已停用，仍挂 OPEN 义务 " + o.getAmount() + " " + o.getCurrency());
        }
    }

    private List<String> mixedCurrencyFailures(List<TradeObligation> opens) {
        Map<String, Long> countByCurrency = new TreeMap<>();
        for (TradeObligation o : opens) {
            countByCurrency.merge(o.getCurrency(), 1L, Long::sum);
        }
        if (countByCurrency.size() <= 1) {
            return List.of();
        }
        StringBuilder sb = new StringBuilder("同一交割日 OPEN 义务涉及 ")
                .append(countByCurrency.size()).append(" 个币种: ");
        countByCurrency.forEach((ccy, n) ->
                sb.append(ccy).append("(").append(n).append(" 笔) "));
        sb.append("—— 单币种轧差前请先拆分");
        return List.of(sb.toString().trim());
    }

    private List<String> nonPositiveAmountFailures(List<TradeObligation> opens) {
        List<String> failures = new ArrayList<>();
        for (TradeObligation o : opens) {
            BigDecimal amount = o.getAmount();
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                failures.add("义务 " + o.getObligationId() + " 金额非正: "
                        + (amount == null ? "null" : amount.toPlainString()) + " " + o.getCurrency());
            }
        }
        return failures;
    }

    private List<String> failedRunFailures(List<NettingRun> failedRuns) {
        List<String> failures = new ArrayList<>();
        for (NettingRun run : failedRuns) {
            String reason = run.getFailureReason() == null ? "" : "，原因: " + run.getFailureReason();
            failures.add("FAILED 批次 " + run.getRunId() + " (" + run.getCurrency() + ") 未处理" + reason);
        }
        return failures;
    }
}
