package com.clearing.netting.adapter.out.persistence.repo;

import com.clearing.netting.adapter.out.persistence.entity.GateCheckConfigJpaEntity;
import com.clearing.netting.domain.model.GateCheckType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GateCheckConfigJpaRepository extends JpaRepository<GateCheckConfigJpaEntity, GateCheckType> {
}
