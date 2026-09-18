package com.clearing.netting.adapter.out.persistence;

import com.clearing.netting.adapter.out.persistence.entity.GateCheckResultJpaEntity;
import com.clearing.netting.adapter.out.persistence.entity.GateRunJpaEntity;
import com.clearing.netting.adapter.out.persistence.repo.GateCheckResultJpaRepository;
import com.clearing.netting.adapter.out.persistence.repo.GateRunJpaRepository;
import com.clearing.netting.domain.model.GateCheckResult;
import com.clearing.netting.domain.model.GateRun;
import com.clearing.netting.domain.port.out.GateRunRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class GateRunRepositoryAdapter implements GateRunRepositoryPort {

    private final GateRunJpaRepository runRepository;
    private final GateCheckResultJpaRepository resultRepository;

    public GateRunRepositoryAdapter(
            GateRunJpaRepository runRepository,
            GateCheckResultJpaRepository resultRepository) {
        this.runRepository = runRepository;
        this.resultRepository = resultRepository;
    }

    @Override
    public GateRun save(GateRun run) {
        runRepository.save(PersistenceMapper.toEntity(run));
        List<GateCheckResultJpaEntity> entities = run.getResults().stream()
                .map(r -> PersistenceMapper.toEntity(run.getGateRunId(), r))
                .collect(Collectors.toList());
        resultRepository.saveAll(entities);
        return run;
    }

    @Override
    public Optional<GateRun> findById(String gateRunId) {
        return runRepository.findById(gateRunId)
                .map(e -> PersistenceMapper.toDomain(e, resultsOf(List.of(e.getGateRunId()))));
    }

    @Override
    public List<GateRun> findAllOrderByCreatedAtDesc() {
        List<GateRunJpaEntity> runs = runRepository.findAllByOrderByCreatedAtDesc();
        Map<String, List<GateCheckResult>> resultsByRun = resultsOf(
                runs.stream().map(GateRunJpaEntity::getGateRunId).collect(Collectors.toList()));
        return runs.stream()
                .map(e -> PersistenceMapper.toDomain(e, resultsByRun))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<GateRun> findLatestByBusinessDate(LocalDate businessDate) {
        return runRepository.findTopByBusinessDateOrderByCreatedAtDesc(businessDate)
                .map(e -> PersistenceMapper.toDomain(e, resultsOf(List.of(e.getGateRunId()))));
    }

    private Map<String, List<GateCheckResult>> resultsOf(List<String> gateRunIds) {
        if (gateRunIds.isEmpty()) {
            return Map.of();
        }
        return resultRepository.findByGateRunIdIn(gateRunIds).stream()
                .sorted(Comparator.comparing(r -> r.getCheckType().ordinal()))
                .collect(Collectors.groupingBy(
                        GateCheckResultJpaEntity::getGateRunId,
                        Collectors.mapping(PersistenceMapper::toDomain, Collectors.toCollection(ArrayList::new))));
    }
}
