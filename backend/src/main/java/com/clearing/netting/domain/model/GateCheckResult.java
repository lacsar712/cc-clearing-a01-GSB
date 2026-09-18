package com.clearing.netting.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 单项检查结果。enabled=false 表示该检查项已关闭，本次运行跳过并视为通过。
 */
public class GateCheckResult {
    private final String resultId;
    private final GateCheckType checkType;
    private final boolean enabled;
    private final boolean passed;
    private final List<String> details;

    public GateCheckResult(
            String resultId,
            GateCheckType checkType,
            boolean enabled,
            boolean passed,
            List<String> details) {
        this.resultId = Objects.requireNonNull(resultId);
        this.checkType = Objects.requireNonNull(checkType);
        this.enabled = enabled;
        this.passed = passed;
        this.details = List.copyOf(Objects.requireNonNull(details));
    }

    public static GateCheckResult evaluated(GateCheckType type, List<String> failures) {
        return new GateCheckResult(
                UUID.randomUUID().toString(),
                type,
                true,
                failures.isEmpty(),
                failures);
    }

    public static GateCheckResult disabled(GateCheckType type) {
        return new GateCheckResult(UUID.randomUUID().toString(), type, false, true, List.of());
    }

    public String getResultId() {
        return resultId;
    }

    public GateCheckType getCheckType() {
        return checkType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isPassed() {
        return passed;
    }

    public List<String> getDetails() {
        return details;
    }
}
