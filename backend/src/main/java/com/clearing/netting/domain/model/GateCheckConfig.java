package com.clearing.netting.domain.model;

import java.util.Objects;

/**
 * 检查项开关配置，每种类型一行，持久化保存。
 */
public class GateCheckConfig {
    private final GateCheckType checkType;
    private boolean enabled;

    public GateCheckConfig(GateCheckType checkType, boolean enabled) {
        this.checkType = Objects.requireNonNull(checkType);
        this.enabled = enabled;
    }

    public static GateCheckConfig defaultOf(GateCheckType type) {
        return new GateCheckConfig(type, true);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public GateCheckType getCheckType() {
        return checkType;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
