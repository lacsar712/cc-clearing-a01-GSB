package com.clearing.netting.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 一次日终门禁运行：针对某个交割日逐项执行检查单，全部启用项通过则 PASSED。
 */
public class GateRun {
    private final String gateRunId;
    private final LocalDate businessDate;
    private final GateRunStatus status;
    private final String createdBy;
    private final Instant createdAt;
    private final List<GateCheckResult> results;

    public GateRun(
            String gateRunId,
            LocalDate businessDate,
            GateRunStatus status,
            String createdBy,
            Instant createdAt,
            List<GateCheckResult> results) {
        this.gateRunId = Objects.requireNonNull(gateRunId);
        this.businessDate = Objects.requireNonNull(businessDate);
        this.status = Objects.requireNonNull(status);
        this.createdBy = Objects.requireNonNull(createdBy);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.results = List.copyOf(Objects.requireNonNull(results));
    }

    public static GateRun create(LocalDate businessDate, String createdBy, List<GateCheckResult> results) {
        boolean passed = results.stream()
                .filter(GateCheckResult::isEnabled)
                .allMatch(GateCheckResult::isPassed);
        return new GateRun(
                UUID.randomUUID().toString(),
                businessDate,
                passed ? GateRunStatus.PASSED : GateRunStatus.FAILED,
                createdBy,
                Instant.now(),
                results);
    }

    public long failedCount() {
        return results.stream().filter(GateCheckResult::isEnabled).filter(r -> !r.isPassed()).count();
    }

    public String getGateRunId() {
        return gateRunId;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public GateRunStatus getStatus() {
        return status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<GateCheckResult> getResults() {
        return results;
    }
}
