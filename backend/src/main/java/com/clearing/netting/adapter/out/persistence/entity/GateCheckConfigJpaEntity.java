package com.clearing.netting.adapter.out.persistence.entity;

import com.clearing.netting.domain.model.GateCheckType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "eod_gate_check_configs")
public class GateCheckConfigJpaEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 48)
    private GateCheckType checkType;

    @Column(nullable = false)
    private boolean enabled;

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
}
