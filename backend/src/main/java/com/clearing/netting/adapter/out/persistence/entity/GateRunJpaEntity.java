package com.clearing.netting.adapter.out.persistence.entity;

import com.clearing.netting.domain.model.GateRunStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "eod_gate_runs")
public class GateRunJpaEntity {

    @Id
    @Column(length = 64)
    private String gateRunId;

    @Column(nullable = false)
    private LocalDate businessDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GateRunStatus status;

    @Column(nullable = false, length = 64)
    private String createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    public String getGateRunId() {
        return gateRunId;
    }

    public void setGateRunId(String gateRunId) {
        this.gateRunId = gateRunId;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public GateRunStatus getStatus() {
        return status;
    }

    public void setStatus(GateRunStatus status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
