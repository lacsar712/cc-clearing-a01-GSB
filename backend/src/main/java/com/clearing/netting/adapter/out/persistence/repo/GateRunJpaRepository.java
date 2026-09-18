package com.clearing.netting.adapter.out.persistence.repo;

import com.clearing.netting.adapter.out.persistence.entity.GateRunJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GateRunJpaRepository extends JpaRepository<GateRunJpaEntity, String> {

    List<GateRunJpaEntity> findAllByOrderByCreatedAtDesc();

    List<GateRunJpaEntity> findBySettleDateOrderByCreatedAtDesc(LocalDate settleDate);

    Optional<GateRunJpaEntity> findFirstBySettleDateOrderByCreatedAtDesc(LocalDate settleDate);
}
