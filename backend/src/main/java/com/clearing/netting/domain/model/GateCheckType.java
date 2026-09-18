package com.clearing.netting.domain.model;

/**
 * 日终门禁检查项。固定四类，可通过配置开关，不扩展为通用规则引擎。
 */
public enum GateCheckType {
    SUSPENDED_MEMBER_OPEN("停用会员仍挂 OPEN 义务", "交割日当日 OPEN 义务的付款/收款方不得为已停用会员"),
    MIXED_CURRENCY_SAME_DAY("同日多币种混用", "同一交割日的 OPEN 义务只允许单一币种"),
    NON_POSITIVE_AMOUNT("金额非正", "交割日当日 OPEN 义务金额必须为正数"),
    UNRESOLVED_FAILED_RUN("当日 FAILED 批次未处理", "交割日当日不得残留 FAILED 状态的轧差批次");

    private final String label;
    private final String description;

    GateCheckType(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
