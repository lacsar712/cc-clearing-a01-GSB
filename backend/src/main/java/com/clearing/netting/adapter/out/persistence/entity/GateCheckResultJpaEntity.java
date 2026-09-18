package com.clearing.netting.adapter.out.persistence.entity;

import com.clearing.netting.domain.model.GateCheckType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "eod_gate_check_results")
public class GateCheckResultJpaEntity {

    @Id
    @Column(length = 64)
    private String resultId;

    @Column(nullable = false, length = 64)
    private String gateRunId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 48)
    private GateCheckType checkType;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private boolean passed;

    /** 失败原因明细，换行分隔 */
    @Column(length = 4000)
    private String details;

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public String getGateRunId() {
        return gateRunId;
    }

    public void setGateRunId(String gateRunId) {
        this.gateRunId = gateRunId;
    }

    public GateCheckType getCheckType() {
        return checkType;
    }

    public void setCheckType(GateCheckType checkType) {
        this.checkType = checkType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
