package com.clearing.netting.adapter.out.persistence.repo;

import com.clearing.netting.adapter.out.persistence.entity.GateCheckResultJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface GateCheckResultJpaRepository extends JpaRepository<GateCheckResultJpaEntity, String> {

    List<GateCheckResultJpaEntity> findByGateRunId(String gateRunId);

    List<GateCheckResultJpaEntity> findByGateRunIdIn(Collection<String> gateRunIds);
}
