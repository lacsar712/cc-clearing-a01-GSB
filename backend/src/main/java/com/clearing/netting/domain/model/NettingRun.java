package com.clearing.netting.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class NettingRun {
    private final String runId;
    private final LocalDate settleDate;
    private final String currency;
    private NettingRunStatus status;
    private final Instant createdAt;
    private String failureReason;
    private boolean acknowledged;

    public NettingRun(
            String runId,
            LocalDate settleDate,
            String currency,
            NettingRunStatus status,
            Instant createdAt,
            String failureReason) {
        this(runId, settleDate, currency, status, createdAt, failureReason, false);
    }

    public NettingRun(
            String runId,
            LocalDate settleDate,
            String currency,
            NettingRunStatus status,
            Instant createdAt,
            String failureReason,
            boolean acknowledged) {
        this.runId = Objects.requireNonNull(runId);
        this.settleDate = Objects.requireNonNull(settleDate);
        this.currency = Objects.requireNonNull(currency).toUpperCase();
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.failureReason = reasonOrNull(failureReason);
        this.acknowledged = acknowledged;
    }

    private static String reasonOrNull(String reason) {
        return reason == null || reason.isBlank() ? null : reason;
    }

    public static NettingRun create(LocalDate settleDate, String currency) {
        return new NettingRun(
                UUID.randomUUID().toString(),
                settleDate,
                currency,
                NettingRunStatus.CREATED,
                Instant.now(),
                null,
                false);
    }

    public void markRunning() {
        this.status = NettingRunStatus.RUNNING;
    }

    public void markCompleted() {
        this.status = NettingRunStatus.COMPLETED;
        this.failureReason = null;
    }

    public void markFailed(String reason) {
        this.status = NettingRunStatus.FAILED;
        this.failureReason = reason;
        this.acknowledged = false;
    }

    /** 操作员确认已处理该 FAILED 批次，门禁不再拦截。 */
    public void acknowledgeFailure() {
        if (this.status != NettingRunStatus.FAILED) {
            throw new IllegalStateException("only FAILED runs can be acknowledged");
        }
        this.acknowledged = true;
    }

    public String getRunId() {
        return runId;
    }

    public LocalDate getSettleDate() {
        return settleDate;
    }

    public String getCurrency() {
        return currency;
    }

    public NettingRunStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }
}
