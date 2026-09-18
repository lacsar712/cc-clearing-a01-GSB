package com.clearing.netting.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 一次门禁运行中单个检查项的结果。
 */
public class GateCheckResult {

    private final GateCheckType checkType;
    private final GateItemStatus status;
    private final String message;
    private final List<GateFinding> findings;

    public GateCheckResult(GateCheckType checkType, GateItemStatus status, String message, List<GateFinding> findings) {
        this.checkType = checkType;
        this.status = status;
        this.message = message;
        this.findings = findings == null ? new ArrayList<>() : findings;
    }

    public static GateCheckResult skipped(GateCheckType type) {
        return new GateCheckResult(type, GateItemStatus.SKIPPED, "该检查项已停用，跳过", new ArrayList<>());
    }

    public static GateCheckResult pass(GateCheckType type, String message) {
        return new GateCheckResult(type, GateItemStatus.PASS, message, new ArrayList<>());
    }

    public static GateCheckResult fail(GateCheckType type, String message, List<GateFinding> findings) {
        return new GateCheckResult(type, GateItemStatus.FAIL, message, findings);
    }

    public boolean isFailure() {
        return status == GateItemStatus.FAIL;
    }

    public GateCheckType getCheckType() {
        return checkType;
    }

    public GateItemStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public List<GateFinding> getFindings() {
        return findings;
    }
}
