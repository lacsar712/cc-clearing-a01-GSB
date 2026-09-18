package com.clearing.netting.adapter.out.persistence;

import com.clearing.netting.adapter.out.persistence.repo.GateRunJpaRepository;
import com.clearing.netting.domain.model.GateRun;
import com.clearing.netting.domain.port.out.GateRunRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class GateRunRepositoryAdapter implements GateRunRepositoryPort {

    private final GateRunJpaRepository repository;

    public GateRunRepositoryAdapter(GateRunJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public GateRun save(GateRun run) {
        return GatePersistenceMapper.toDomain(repository.save(GatePersistenceMapper.toEntity(run)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GateRun> findById(String runId) {
        return repository.findById(runId).map(GatePersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GateRun> findAllOrderByCreatedAtDesc() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(GatePersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GateRun> findBySettleDateOrderByCreatedAtDesc(LocalDate settleDate) {
        return repository.findBySettleDateOrderByCreatedAtDesc(settleDate).stream()
                .map(GatePersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GateRun> findLatestBySettleDate(LocalDate settleDate) {
        return repository.findFirstBySettleDateOrderByCreatedAtDesc(settleDate)
                .map(GatePersistenceMapper::toDomain);
    }
}
