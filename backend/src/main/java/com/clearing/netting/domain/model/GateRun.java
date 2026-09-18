package com.clearing.netting.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 一次日终门禁运行。对指定交割日，按当前开关逐项检查，汇总为 PASSED / FAILED。
 */
public class GateRun {

    private final String runId;
    private final LocalDate settleDate;
    private final String operator;
    private final Instant createdAt;
    private final List<GateCheckResult> results;
    private GateRunStatus status;

    public GateRun(
            String runId,
            LocalDate settleDate,
            String operator,
            Instant createdAt,
            GateRunStatus status,
            List<GateCheckResult> results) {
        this.runId = Objects.requireNonNull(runId);
        this.settleDate = Objects.requireNonNull(settleDate);
        this.operator = operator;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.status = Objects.requireNonNull(status);
        this.results = results == null ? new ArrayList<>() : results;
    }

    public static GateRun start(LocalDate settleDate, String operator) {
        return new GateRun(
                UUID.randomUUID().toString(),
                settleDate,
                operator,
                Instant.now(),
                GateRunStatus.FAILED,
                new ArrayList<>());
    }

    public void complete(List<GateCheckResult> checked) {
        this.results.clear();
        this.results.addAll(checked);
        boolean anyFail = checked.stream().anyMatch(GateCheckResult::isFailure);
        this.status = anyFail ? GateRunStatus.FAILED : GateRunStatus.PASSED;
    }

    public boolean passed() {
        return status == GateRunStatus.PASSED;
    }

    public String getRunId() {
        return runId;
    }

    public LocalDate getSettleDate() {
        return settleDate;
    }

    public String getOperator() {
        return operator;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public GateRunStatus getStatus() {
        return status;
    }

    public List<GateCheckResult> getResults() {
        return results;
    }
}
