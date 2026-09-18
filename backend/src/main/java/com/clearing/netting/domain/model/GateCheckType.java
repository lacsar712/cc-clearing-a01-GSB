package com.clearing.netting.domain.model;

/**
 * 日终门禁检查项。正式轧差前逐项检查，任一项失败则门禁不通过。
 */
public enum GateCheckType {

    SUSPENDED_MEMBER_OPEN_OBLIGATION("停用会员挂 OPEN 义务", "停用（SUSPENDED）会员当日仍存在 OPEN 义务"),
    SAME_DAY_MIXED_CURRENCY("同日多币种混用", "同一交割日的 OPEN 义务出现多个币种"),
    NON_POSITIVE_AMOUNT("金额非正", "存在金额小于等于 0 的义务"),
    FAILED_BATCH_UNRESOLVED("FAILED 批次未处理", "当日仍有未确认处理的 FAILED 轧差批次");

    private final String title;
    private final String description;

    GateCheckType(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
