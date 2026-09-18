package com.clearing.netting.adapter.out.persistence.entity;

import com.clearing.netting.domain.model.GateCheckType;
import com.clearing.netting.domain.model.GateItemStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "gate_check_results")
public class GateCheckResultJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private GateRunJpaEntity run;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 48)
    private GateCheckType checkType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GateItemStatus status;

    @Column(nullable = false, length = 1024)
    private String message;

    @Column(nullable = false, name = "sort_index")
    private int position;

    @OneToMany(mappedBy = "result", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<GateFindingJpaEntity> findings = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GateRunJpaEntity getRun() {
        return run;
    }

    public void setRun(GateRunJpaEntity run) {
        this.run = run;
    }

    public GateCheckType getCheckType() {
        return checkType;
    }

    public void setCheckType(GateCheckType checkType) {
        this.checkType = checkType;
    }

    public GateItemStatus getStatus() {
        return status;
    }

    public void setStatus(GateItemStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public List<GateFindingJpaEntity> getFindings() {
        return findings;
    }

    public void setFindings(List<GateFindingJpaEntity> findings) {
        this.findings = findings;
    }
}
