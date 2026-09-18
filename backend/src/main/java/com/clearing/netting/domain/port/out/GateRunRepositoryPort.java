package com.clearing.netting.domain.port.out;

import com.clearing.netting.domain.model.GateRun;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GateRunRepositoryPort {

    GateRun save(GateRun run);

    Optional<GateRun> findById(String runId);

    List<GateRun> findAllOrderByCreatedAtDesc();

    List<GateRun> findBySettleDateOrderByCreatedAtDesc(LocalDate settleDate);

    Optional<GateRun> findLatestBySettleDate(LocalDate settleDate);
}
