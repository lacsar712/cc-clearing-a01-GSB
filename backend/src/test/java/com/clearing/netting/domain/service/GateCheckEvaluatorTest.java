package com.clearing.netting.domain.service;

import com.clearing.netting.domain.model.GateCheckResult;
import com.clearing.netting.domain.model.GateCheckType;
import com.clearing.netting.domain.model.GateItemStatus;
import com.clearing.netting.domain.model.Member;
import com.clearing.netting.domain.model.MemberStatus;
import com.clearing.netting.domain.model.NettingRun;
import com.clearing.netting.domain.model.ObligationStatus;
import com.clearing.netting.domain.model.TradeObligation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GateCheckEvaluatorTest {

    private GateCheckEvaluator evaluator;
    private LocalDate date;
    private Member a;
    private Member b;
    private Member c;

    @BeforeEach
    void setUp() {
        evaluator = new GateCheckEvaluator();
        date = LocalDate.of(2026, 9, 17);
        a = new Member("A", "Bank A", MemberStatus.ACTIVE);
        b = new Member("B", "Bank B", MemberStatus.ACTIVE);
        c = new Member("C", "Bank C", MemberStatus.ACTIVE);
    }

    @Test
    void allFourFailOnDirtyData() {
        Member suspended = new Member("C", "Bank C", MemberStatus.SUSPENDED);
        List<TradeObligation> opens = List.of(
                obligation("A", "C", "USD", "100", date),    // suspended member
                obligation("B", "A", "EUR", "50", date),      // mixed currency vs dominant USD
                obligation("A", "B", "USD", "-5", date)       // non-positive
        );
        Map<String, Member> members = Map.of("A", a, "B", b, "C", suspended);
        NettingRun failed = NettingRun.create(date, "USD");
        failed.markFailed("boom");
        GateCheckEvaluator.GateData data =
                new GateCheckEvaluator.GateData(opens, members, List.of(failed));

        List<GateCheckResult> results = evaluator.evaluate(data, allEnabled());

        Map<GateCheckType, GateCheckResult> byType = index(results);
        for (GateCheckType t : GateCheckType.values()) {
            assertEquals(GateItemStatus.FAIL, byType.get(t).getStatus(),
                    () -> t + " should FAIL, message=" + byType.get(t).getMessage());
            assertFalse(byType.get(t).getFindings().isEmpty(), () -> t + " should carry findings");
        }
    }

    @Test
    void cleanDataPassesAll() {
        List<TradeObligation> opens = List.of(
                obligation("A", "B", "USD", "100", date),
                obligation("B", "C", "USD", "40", date)
        );
        Map<String, Member> members = Map.of("A", a, "B", b, "C", c);
        GateCheckEvaluator.GateData data =
                new GateCheckEvaluator.GateData(opens, members, List.of());

        List<GateCheckResult> results = evaluator.evaluate(data, allEnabled());

        for (GateCheckResult r : results) {
            assertEquals(GateItemStatus.PASS, r.getStatus(), () -> r.getCheckType() + " should PASS");
        }
    }

    @Test
    void acknowledgedFailedRunPassesBatchCheck() {
        NettingRun failed = NettingRun.create(date, "USD");
        failed.markFailed("boom");
        failed.acknowledgeFailure();
        GateCheckEvaluator.GateData data =
                new GateCheckEvaluator.GateData(List.of(), Map.of(), List.of(failed));

        List<GateCheckResult> results = evaluator.evaluate(data, allEnabled());

        assertEquals(GateItemStatus.PASS,
                index(results).get(GateCheckType.FAILED_BATCH_UNRESOLVED).getStatus());
    }

    @Test
    void disabledCheckIsSkipped() {
        EnumMap<GateCheckType, Boolean> flags = allEnabled();
        flags.put(GateCheckType.NON_POSITIVE_AMOUNT, false);
        List<TradeObligation> opens = List.of(obligation("A", "B", "USD", "-1", date));
        GateCheckEvaluator.GateData data =
                new GateCheckEvaluator.GateData(opens, Map.of("A", a, "B", b), List.of());

        List<GateCheckResult> results = evaluator.evaluate(data, flags);

        assertEquals(GateItemStatus.SKIPPED,
                index(results).get(GateCheckType.NON_POSITIVE_AMOUNT).getStatus());
    }

    @Test
    void mixedCurrencyOnlyFlagsNonDominantObligations() {
        List<TradeObligation> opens = List.of(
                obligation("A", "B", "USD", "100", date),
                obligation("B", "C", "USD", "40", date),
                obligation("C", "A", "EUR", "10", date)
        );
        GateCheckEvaluator.GateData data =
                new GateCheckEvaluator.GateData(opens, Map.of("A", a, "B", b, "C", c), List.of());

        GateCheckResult result = index(evaluator.evaluate(data, allEnabled()))
                .get(GateCheckType.SAME_DAY_MIXED_CURRENCY);

        assertEquals(GateItemStatus.FAIL, result.getStatus());
        assertEquals(1, result.getFindings().size());
        assertEquals("EUR", result.getFindings().get(0).getCurrency());
        assertTrue(result.getMessage().contains("USD"));
    }

    private EnumMap<GateCheckType, Boolean> allEnabled() {
        EnumMap<GateCheckType, Boolean> flags = new EnumMap<>(GateCheckType.class);
        for (GateCheckType t : GateCheckType.values()) {
            flags.put(t, true);
        }
        return flags;
    }

    private Map<GateCheckType, GateCheckResult> index(List<GateCheckResult> results) {
        Map<GateCheckType, GateCheckResult> map = new EnumMap<>(GateCheckType.class);
        for (GateCheckResult r : results) {
            map.put(r.getCheckType(), r);
        }
        return map;
    }

    private TradeObligation obligation(String payer, String payee, String ccy, String amount, LocalDate settle) {
        return new TradeObligation(
                UUID.randomUUID().toString(),
                payer, payee, ccy, new BigDecimal(amount),
                settle.minusDays(1), settle, ObligationStatus.OPEN, null);
    }
}
