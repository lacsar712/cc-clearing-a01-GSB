package com.clearing.netting.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 单条门禁问题明细，定位到具体义务 / 批次 / 会员，便于工作台逐项展示与修复。
 */
public class GateFinding {

    /** OBLIGATION / NETTING_RUN / MEMBER */
    private final String targetType;
    private final String targetId;
    private final String memberId;
    private final String memberName;
    private final String currency;
    private final BigDecimal amount;
    private final LocalDate settleDate;
    private final String detail;

    public GateFinding(
            String targetType,
            String targetId,
            String memberId,
            String memberName,
            String currency,
            BigDecimal amount,
            LocalDate settleDate,
            String detail) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.memberId = memberId;
        this.memberName = memberName;
        this.currency = currency;
        this.amount = amount;
        this.settleDate = settleDate;
        this.detail = detail;
    }

    public static GateFinding obligation(TradeObligation o, String memberId, String memberName, String detail) {
        return new GateFinding(
                "OBLIGATION",
                o.getObligationId(),
                memberId,
                memberName,
                o.getCurrency(),
                o.getAmount(),
                o.getSettleDate(),
                detail);
    }

    public static GateFinding run(NettingRun run, String detail) {
        return new GateFinding(
                "NETTING_RUN",
                run.getRunId(),
                null,
                null,
                run.getCurrency(),
                null,
                run.getSettleDate(),
                detail);
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getSettleDate() {
        return settleDate;
    }

    public String getDetail() {
        return detail;
    }
}
