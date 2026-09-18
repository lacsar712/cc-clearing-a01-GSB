package com.clearing.netting.application;

import com.clearing.netting.domain.exception.DomainException;
import com.clearing.netting.domain.model.GateCheckConfig;
import com.clearing.netting.domain.model.GateCheckResult;
import com.clearing.netting.domain.model.GateCheckType;
import com.clearing.netting.domain.model.GateRun;
import com.clearing.netting.domain.model.Member;
import com.clearing.netting.domain.model.NettingRun;
import com.clearing.netting.domain.model.NettingRunStatus;
import com.clearing.netting.domain.model.ObligationStatus;
import com.clearing.netting.domain.model.TradeObligation;
import com.clearing.netting.domain.port.out.GateCheckConfigRepositoryPort;
import com.clearing.netting.domain.port.out.GateRunRepositoryPort;
import com.clearing.netting.domain.port.out.MemberRepositoryPort;
import com.clearing.netting.domain.port.out.NettingRunRepositoryPort;
import com.clearing.netting.domain.port.out.ObligationRepositoryPort;
import com.clearing.netting.domain.service.EndOfDayGateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GateApplicationService {

    private final GateRunRepositoryPort gateRunRepository;
    private final GateCheckConfigRepositoryPort configRepository;
    private final ObligationRepositoryPort obligationRepository;
    private final MemberRepositoryPort memberRepository;
    private final NettingRunRepositoryPort nettingRunRepository;
    private final EndOfDayGateService gateService;

    public GateApplicationService(
            GateRunRepositoryPort gateRunRepository,
            GateCheckConfigRepositoryPort configRepository,
            ObligationRepositoryPort obligationRepository,
            MemberRepositoryPort memberRepository,
            NettingRunRepositoryPort nettingRunRepository) {
        this.gateRunRepository = gateRunRepository;
        this.configRepository = configRepository;
        this.obligationRepository = obligationRepository;
        this.memberRepository = memberRepository;
        this.nettingRunRepository = nettingRunRepository;
        this.gateService = new EndOfDayGateService();
    }

    @Transactional(readOnly = true)
    public List<GateCheckConfig> listChecks() {
        Map<GateCheckType, GateCheckConfig> byType = configRepository.findAll().stream()
                .collect(Collectors.toMap(GateCheckConfig::getCheckType, c -> c));
        // 按枚举固定顺序返回，缺失的按默认开启兜底
        return java.util.Arrays.stream(GateCheckType.values())
                .map(t -> byType.getOrDefault(t, GateCheckConfig.defaultOf(t)))
                .collect(Collectors.toList());
    }

    @Transactional
    public GateCheckConfig updateCheck(GateCheckType type, boolean enabled) {
        GateCheckConfig config = configRepository.findByType(type)
                .orElse(GateCheckConfig.defaultOf(type));
        config.setEnabled(enabled);
        return configRepository.save(config);
    }

    @Transactional
    public GateRun run(LocalDate businessDate, String operator) {
        if (businessDate == null) {
            throw new DomainException("INVALID_DATE", "businessDate is required");
        }
        Set<GateCheckType> enabled = listChecks().stream()
                .filter(GateCheckConfig::isEnabled)
                .map(GateCheckConfig::getCheckType)
                .collect(Collectors.toCollection(HashSet::new));

        List<TradeObligation> opens =
                obligationRepository.findByFilters(null, businessDate, ObligationStatus.OPEN);

        Set<String> memberIds = new HashSet<>();
        for (TradeObligation o : opens) {
            memberIds.add(o.getPayerMemberId());
            memberIds.add(o.getPayeeMemberId());
        }
        Map<String, Member> members = new HashMap<>();
        for (Member m : memberRepository.findByIds(memberIds)) {
            members.put(m.getMemberId(), m);
        }

        List<NettingRun> failedRuns = nettingRunRepository.findBySettleDate(businessDate).stream()
                .filter(r -> r.getStatus() == NettingRunStatus.FAILED)
                .sorted(Comparator.comparing(NettingRun::getCreatedAt))
                .collect(Collectors.toList());

        List<GateCheckResult> results = gateService.runChecks(enabled, opens, members, failedRuns);
        GateRun run = GateRun.create(businessDate, operator, results);
        return gateRunRepository.save(run);
    }

    @Transactional(readOnly = true)
    public List<GateRun> listRuns() {
        return gateRunRepository.findAllOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public GateRun getRun(String gateRunId) {
        return gateRunRepository.findById(gateRunId)
                .orElseThrow(() -> new DomainException("GATE_RUN_NOT_FOUND", "gate run not found: " + gateRunId));
    }

    @Transactional(readOnly = true)
    public Optional<GateRun> latestFor(LocalDate businessDate) {
        return gateRunRepository.findLatestByBusinessDate(businessDate);
    }
}
