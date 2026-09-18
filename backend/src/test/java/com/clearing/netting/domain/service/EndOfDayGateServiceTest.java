package com.clearing.netting.domain.service;

import com.clearing.netting.domain.model.GateCheckResult;
import com.clearing.netting.domain.model.GateCheckType;
import com.clearing.netting.domain.model.GateRun;
import com.clearing.netting.domain.model.GateRunStatus;
import com.clearing.netting.domain.model.Member;
import com.clearing.netting.domain.model.MemberStatus;
import com.clearing.netting.domain.model.NettingRun;
import com.clearing.netting.domain.model.NettingRunStatus;
import com.clearing.netting.domain.model.ObligationStatus;
import com.clearing.netting.domain.model.TradeObligation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndOfDayGateServiceTest {

    private EndOfDayGateService service;
    private LocalDate businessDate;
    private Member activeA;
    private Member activeB;
    private Member suspendedC;

    @BeforeEach
    void setUp() {
        service = new EndOfDayGateService();
        businessDate = LocalDate.of(2026, 9, 17);
        activeA = new Member("A", "Alpha Bank", MemberStatus.ACTIVE);
        activeB = new Member("B", "Beta Securities", MemberStatus.ACTIVE);
        suspendedC = new Member("C", "Gamma Clearing", MemberStatus.SUSPENDED);
    }

    @Test
    void allChecksPassOnCleanData() {
        List<GateCheckResult> results = service.runChecks(
                EnumSet.allOf(GateCheckType.class),
                List.of(obligation("o1", "A", "B", "USD", "100")),
                Map.of("A", activeA, "B", activeB),
                List.of());

        assertTrue(results.stream().allMatch(GateCheckResult::isPassed));
        GateRun run = GateRun.create(businessDate, "operator", results);
        assertEquals(GateRunStatus.PASSED, run.getStatus());
        assertEquals(0, run.failedCount());
    }

    @Test
    void flagsSuspendedMemberWithOpenObligation() {
        List<GateCheckResult> results = service.runChecks(
                EnumSet.allOf(GateCheckType.class),
                List.of(obligation("o1", "A", "C", "USD", "100")),
                Map.of("A", activeA, "C", suspendedC),
                List.of());

        GateCheckResult r = resultOf(results, GateCheckType.SUSPENDED_MEMBER_OPEN);
        assertFalse(r.isPassed());
        assertEquals(1, r.getDetails().size());
        assertTrue(r.getDetails().get(0).contains("Gamma Clearing"));
        assertTrue(r.getDetails().get(0).contains("o1"));
    }

    @Test
    void flagsMissingMemberProfile() {
        List<GateCheckResult> results = service.runChecks(
                EnumSet.allOf(GateCheckType.class),
                List.of(obligation("o1", "A", "X", "USD", "100")),
                Map.of("A", activeA),
                List.of());

        GateCheckResult r = resultOf(results, GateCheckType.SUSPENDED_MEMBER_OPEN);
        assertFalse(r.isPassed());
        assertTrue(r.getDetails().get(0).contains("会员档案缺失"));
    }

    @Test
    void flagsMixedCurrencyOnSameDay() {
        List<GateCheckResult> results = service.runChecks(
                EnumSet.allOf(GateCheckType.class),
                List.of(
                        obligation("o1", "A", "B", "USD", "100"),
                        obligation("o2", "B", "A", "EUR", "50")),
                Map.of("A", activeA, "B", activeB),
                List.of());

        GateCheckResult r = resultOf(results, GateCheckType.MIXED_CURRENCY_SAME_DAY);
        assertFalse(r.isPassed());
        assertTrue(r.getDetails().get(0).contains("USD"));
        assertTrue(r.getDetails().get(0).contains("EUR"));
    }

    @Test
    void singleCurrencyPassesMixedCheck() {
        List<GateCheckResult> results = service.runChecks(
                EnumSet.allOf(GateCheckType.class),
                List.of(
                        obligation("o1", "A", "B", "USD", "100"),
                        obligation("o2", "B", "A", "USD", "50")),
                Map.of("A", activeA, "B", activeB),
                List.of());

        assertTrue(resultOf(results, GateCheckType.MIXED_CURRENCY_SAME_DAY).isPassed());
    }

    @Test
    void flagsNonPositiveAmount() {
        List<GateCheckResult> results = service.runChecks(
                EnumSet.allOf(GateCheckType.class),
                List.of(obligation("o1", "A", "B", "USD", "0")),
                Map.of("A", activeA, "B", activeB),
                List.of());

        GateCheckResult r = resultOf(results, GateCheckType.NON_POSITIVE_AMOUNT);
        assertFalse(r.isPassed());
        assertTrue(r.getDetails().get(0).contains("o1"));
    }

    @Test
    void flagsUnresolvedFailedRun() {
        NettingRun failed = new NettingRun(
                "run-1", businessDate, "USD", NettingRunStatus.FAILED, Instant.now(), "boom");
        List<GateCheckResult> results = service.runChecks(
                EnumSet.allOf(GateCheckType.class),
                List.of(),
                Map.of(),
                List.of(failed));

        GateCheckResult r = resultOf(results, GateCheckType.UNRESOLVED_FAILED_RUN);
        assertFalse(r.isPassed());
        assertTrue(r.getDetails().get(0).contains("run-1"));
        assertTrue(r.getDetails().get(0).contains("boom"));
    }

    @Test
    void disabledCheckIsSkippedAndTreatedAsPassed() {
        Set<GateCheckType> enabled = EnumSet.allOf(GateCheckType.class);
        enabled.remove(GateCheckType.SUSPENDED_MEMBER_OPEN);

        List<GateCheckResult> results = service.runChecks(
                enabled,
                List.of(obligation("o1", "A", "C", "USD", "100")),
                Map.of("A", activeA, "C", suspendedC),
                List.of());

        GateCheckResult r = resultOf(results, GateCheckType.SUSPENDED_MEMBER_OPEN);
        assertFalse(r.isEnabled());
        assertTrue(r.isPassed());
        GateRun run = GateRun.create(businessDate, "operator", results);
        assertEquals(GateRunStatus.PASSED, run.getStatus());
    }

    @Test
    void runFailsWhenAnyEnabledCheckFails() {
        List<GateCheckResult> results = service.runChecks(
                EnumSet.allOf(GateCheckType.class),
                List.of(obligation("o1", "A", "C", "USD", "100")),
                Map.of("A", activeA, "C", suspendedC),
                List.of());

        GateRun run = GateRun.create(businessDate, "operator", results);
        assertEquals(GateRunStatus.FAILED, run.getStatus());
        assertEquals(1, run.failedCount());
    }

    private GateCheckResult resultOf(List<GateCheckResult> results, GateCheckType type) {
        return results.stream()
                .filter(r -> r.getCheckType() == type)
                .findFirst()
                .orElseThrow();
    }

    private TradeObligation obligation(String id, String payer, String payee, String currency, String amount) {
        return new TradeObligation(
                id,
                payer,
                payee,
                currency,
                new BigDecimal(amount),
                businessDate.minusDays(1),
                businessDate,
                ObligationStatus.OPEN,
                null);
    }
}
