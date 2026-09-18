package com.clearing.netting.adapter.out.persistence;

import com.clearing.netting.adapter.out.persistence.repo.GateCheckConfigJpaRepository;
import com.clearing.netting.domain.model.GateCheckConfig;
import com.clearing.netting.domain.model.GateCheckType;
import com.clearing.netting.domain.port.out.GateCheckConfigRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class GateCheckConfigRepositoryAdapter implements GateCheckConfigRepositoryPort {

    private final GateCheckConfigJpaRepository repository;

    public GateCheckConfigRepositoryAdapter(GateCheckConfigJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public GateCheckConfig save(GateCheckConfig config) {
        return GatePersistenceMapper.toDomain(repository.save(GatePersistenceMapper.toEntity(config)));
    }

    @Override
    public Optional<GateCheckConfig> findByCheckType(GateCheckType checkType) {
        return repository.findById(checkType).map(GatePersistenceMapper::toDomain);
    }

    @Override
    public List<GateCheckConfig> findAll() {
        return repository.findAll().stream()
                .map(GatePersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }
}
