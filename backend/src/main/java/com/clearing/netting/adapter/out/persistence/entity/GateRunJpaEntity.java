package com.clearing.netting.adapter.out.persistence.entity;

import com.clearing.netting.domain.model.GateRunStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "gate_runs")
public class GateRunJpaEntity {

    @Id
    @Column(length = 64)
    private String runId;

    @Column(nullable = false)
    private LocalDate settleDate;

    @Column(length = 64)
    private String operator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GateRunStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<GateCheckResultJpaEntity> results = new ArrayList<>();

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public LocalDate getSettleDate() {
        return settleDate;
    }

    public void setSettleDate(LocalDate settleDate) {
        this.settleDate = settleDate;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public GateRunStatus getStatus() {
        return status;
    }

    public void setStatus(GateRunStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<GateCheckResultJpaEntity> getResults() {
        return results;
    }

    public void setResults(List<GateCheckResultJpaEntity> results) {
        this.results = results;
    }
}
