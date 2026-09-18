package com.clearing.netting.adapter.in.bootstrap;

import com.clearing.netting.domain.model.GateCheckResult;
import com.clearing.netting.domain.model.GateCheckType;
import com.clearing.netting.domain.model.GateRun;
import com.clearing.netting.domain.model.Member;
import com.clearing.netting.domain.model.NettingRun;
import com.clearing.netting.domain.model.ObligationStatus;
import com.clearing.netting.domain.model.TradeObligation;
import com.clearing.netting.domain.port.out.GateRunRepositoryPort;
import com.clearing.netting.domain.port.out.MemberRepositoryPort;
import com.clearing.netting.domain.port.out.NettingRunRepositoryPort;
import com.clearing.netting.domain.port.out.ObligationRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 日终门禁演示数据：保证全新环境下，操作员首次跑门禁即可看到四类失败项。
 * 仅依赖自身的独立会员，幂等（以标记会员是否存在为准），与 seed.sh 的基础数据互不影响。
 * 非正金额义务与未确认的 FAILED 批次无法经 API 造出，因此直接经仓储写入，模拟脏数据。
 */
@Component
@Order(20)
public class GateDemoDataBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GateDemoDataBootstrap.class);
    private static final String SUSPENDED_MARKER = "Zeta Gate Demo（停用）";

    private final MemberRepositoryPort memberRepository;
    private final ObligationRepositoryPort obligationRepository;
    private final NettingRunRepositoryPort runRepository;
    private final GateRunRepositoryPort gateRunRepository;

    public GateDemoDataBootstrap(
            MemberRepositoryPort memberRepository,
            ObligationRepositoryPort obligationRepository,
            NettingRunRepositoryPort runRepository,
            GateRunRepositoryPort gateRunRepository) {
        this.memberRepository = memberRepository;
        this.obligationRepository = obligationRepository;
        this.runRepository = runRepository;
        this.gateRunRepository = gateRunRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean exists = memberRepository.findAll().stream()
                .anyMatch(m -> SUSPENDED_MARKER.equals(m.getName()));
        if (exists) {
            return;
        }

        LocalDate today = LocalDate.now();

        Member epsilon = memberRepository.save(Member.create("Epsilon Gate Demo"));
        Member eta = memberRepository.save(Member.create("Eta Gate Demo"));
        Member zeta = memberRepository.save(Member.create(SUSPENDED_MARKER));

        // 1) 停用会员仍挂 OPEN 义务（USD）
        obligationRepository.save(rawOpen(
                epsilon.getMemberId(), zeta.getMemberId(), "USD", new BigDecimal("12000"), today));
        // 2) 同日多币种混用：当日除 USD 外再混入一笔 EUR
        obligationRepository.save(rawOpen(
                eta.getMemberId(), epsilon.getMemberId(), "EUR", new BigDecimal("8000"), today));
        // 3) 金额非正（直接走构造器，绕过 open() 的正数校验，模拟脏数据）
        obligationRepository.save(rawOpen(
                epsilon.getMemberId(), eta.getMemberId(), "USD", new BigDecimal("-500"), today));

        // 4) 当日 FAILED 批次且未确认处理
        NettingRun failed = NettingRun.create(today, "USD");
        failed.markFailed("NO_OBLIGATIONS: no OPEN obligations for settleDate/currency");
        runRepository.save(failed);

        // 停用 Zeta，使其挂账的 OPEN 义务触发停用会员检查
        zeta.suspend();
        memberRepository.save(zeta);

        // 前一交割日一笔已通过的门禁记录，保证「门禁历史」页打开即有内容（与当日拦截互不影响）。
        LocalDate priorDay = today.minusDays(1);
        List<GateCheckResult> allPass = new ArrayList<>();
        for (GateCheckType t : GateCheckType.values()) {
            allPass.add(GateCheckResult.pass(t, t.getTitle() + "（演示历史记录）"));
        }
        GateRun priorRun = GateRun.start(priorDay, "operator");
        priorRun.complete(allPass);
        gateRunRepository.save(priorRun);

        log.info("Gate demo data ensured for settleDate {} (4 failing checks on first run)", today);
    }

    private TradeObligation rawOpen(
            String payer, String payee, String currency, BigDecimal amount, LocalDate date) {
        return new TradeObligation(
                java.util.UUID.randomUUID().toString(),
                payer,
                payee,
                currency,
                amount,
                date,
                date,
                ObligationStatus.OPEN,
                null);
    }
}
