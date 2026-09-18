package com.clearing.netting.domain.model;

/**
 * 检查项开关配置。默认四项全部启用。
 */
public class GateCheckConfig {

    private final GateCheckType checkType;
    private boolean enabled;

    public GateCheckConfig(GateCheckType checkType, boolean enabled) {
        this.checkType = checkType;
        this.enabled = enabled;
    }

    public static GateCheckConfig enabledByDefault(GateCheckType type) {
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
