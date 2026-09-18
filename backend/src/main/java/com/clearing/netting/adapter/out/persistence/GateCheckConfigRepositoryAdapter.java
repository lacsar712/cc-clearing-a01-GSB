package com.clearing.netting.adapter.out.persistence;

import com.clearing.netting.adapter.out.persistence.entity.GateCheckConfigJpaEntity;
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
    public List<GateCheckConfig> findAll() {
        return repository.findAll().stream()
                .map(PersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<GateCheckConfig> findByType(GateCheckType type) {
        return repository.findById(type).map(PersistenceMapper::toDomain);
    }

    @Override
    public GateCheckConfig save(GateCheckConfig config) {
        GateCheckConfigJpaEntity e = new GateCheckConfigJpaEntity();
        e.setCheckType(config.getCheckType());
        e.setEnabled(config.isEnabled());
        return PersistenceMapper.toDomain(repository.save(e));
    }
}
