package com.clearing.netting.domain.port.out;

import com.clearing.netting.domain.model.GateCheckConfig;
import com.clearing.netting.domain.model.GateCheckType;

import java.util.List;
import java.util.Optional;

public interface GateCheckConfigRepositoryPort {

    GateCheckConfig save(GateCheckConfig config);

    Optional<GateCheckConfig> findByCheckType(GateCheckType checkType);

    List<GateCheckConfig> findAll();
}
