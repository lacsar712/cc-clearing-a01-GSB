package com.clearing.netting.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 端到端：登录 → 门禁跑出四类失败 → 轧差被拦截 → 修复数据 → 重跑通过 → 轧差成功；只读账号写操作被拒。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GateEndToEndTest {

    private static final LocalDate DATE = LocalDate.of(2026, 6, 15);

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    com.clearing.netting.domain.port.out.MemberRepositoryPort memberRepository;
    @Autowired
    com.clearing.netting.domain.port.out.ObligationRepositoryPort obligationRepository;
    @Autowired
    com.clearing.netting.domain.port.out.NettingRunRepositoryPort runRepository;

    private String api(String path) {
        return "http://localhost:" + port + "/api" + path;
    }

    private String login(String user, String password) {
        String body = "{\"username\":\"" + user + "\",\"password\":\"" + password + "\"}";
        ResponseEntity<String> resp = rest.postForEntity(api("/auth/login"), new HttpEntity<>(body, jsonHeaders()), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode(), "login failed: " + resp.getBody());
        return (String) read(resp).get("token").asText();
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.set("Content-Type", "application/json");
        return h;
    }

    private HttpHeaders auth(String token) {
        HttpHeaders h = jsonHeaders();
        h.setBearerAuth(token);
        return h;
    }

    private JsonNode read(ResponseEntity<String> resp) {
        try {
            return mapper.readTree(resp.getBody());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private ResponseEntity<String> post(String path, String token, String json) {
        return rest.exchange(api(path), HttpMethod.POST, new HttpEntity<>(json, auth(token)), String.class);
    }

    private ResponseEntity<String> get(String path, String token) {
        return rest.exchange(api(path), HttpMethod.GET, new HttpEntity<>(auth(token)), String.class);
    }

    @Test
    void gateFailFixPassThenNet() {
        String operator = login("operator", "op123456");
        String viewer = login("viewer", "view123456");

        // ---- 直接经仓储构造 API 无法造出的脏数据：停用会员 + EUR 混用 + 负金额 + FAILED 批次 ----
        var a = memberRepository.save(com.clearing.netting.domain.model.Member.create("E2E Alpha"));
        var b = memberRepository.save(com.clearing.netting.domain.model.Member.create("E2E Beta"));
        var d = memberRepository.save(com.clearing.netting.domain.model.Member.create("E2E Suspended"));
        d.suspend();
        d = memberRepository.save(d);

        String oSuspended = newRawOpen(a.getMemberId(), d.getMemberId(), "USD", "1000");
        String oEur = newRawOpen(b.getMemberId(), a.getMemberId(), "EUR", "500");
        String oNegative = newRawOpen(a.getMemberId(), b.getMemberId(), "USD", "-3");

        var failed = com.clearing.netting.domain.model.NettingRun.create(DATE, "USD");
        failed.markFailed("NO_OBLIGATIONS");
        runRepository.save(failed);

        // ---- 首次门禁：四项全部失败 ----
        JsonNode first = read(post("/gate/runs", operator, "{\"settleDate\":\"" + DATE + "\"}"));
        assertFalse(first.get("passed").asBoolean());
        JsonNode results = first.get("results");
        assertEquals(4, results.size());
        for (JsonNode r : results) {
            assertEquals("FAIL", r.get("status").asText(),
                    r.get("checkType").asText() + " expected FAIL but: " + r.get("message").asText());
        }

        // 状态端点反映未通过
        JsonNode status = read(get("/gate/status?settleDate=" + DATE, operator));
        assertTrue(status.get("hasRun").asBoolean());
        assertFalse(status.get("passed").asBoolean());

        // ---- 未通过门禁，轧差被拦截 ----
        ResponseEntity<String> blocked = post("/netting-runs", operator,
                "{\"settleDate\":\"" + DATE + "\",\"currency\":\"USD\"}");
        assertEquals(HttpStatus.BAD_REQUEST, blocked.getStatusCode());
        assertEquals("GATE_NOT_PASSED", read(blocked).get("code").asText());

        // ---- 修复：取消三笔问题义务 + 确认 FAILED 批次 ----
        assertEquals(HttpStatus.OK, post("/obligations/" + oSuspended + "/cancel", operator, "").getStatusCode());
        assertEquals(HttpStatus.OK, post("/obligations/" + oEur + "/cancel", operator, "").getStatusCode());
        assertEquals(HttpStatus.OK, post("/obligations/" + oNegative + "/cancel", operator, "").getStatusCode());
        assertEquals(HttpStatus.OK, post("/netting-runs/" + failed.getRunId() + "/acknowledge", operator, "").getStatusCode());

        // ---- 数据修好后重跑：全部通过 ----
        JsonNode second = read(post("/gate/runs", operator, "{\"settleDate\":\"" + DATE + "\"}"));
        assertTrue(second.get("passed").asBoolean(), "gate should pass after remediation: " + second);
        for (JsonNode r : second.get("results")) {
            assertEquals("PASS", r.get("status").asText(), r.get("checkType").asText());
        }

        // 录入一笔干净的同币种义务后执行轧差
        String clean = "{\"payerMemberId\":\"" + a.getMemberId() + "\","
                + "\"payeeMemberId\":\"" + b.getMemberId() + "\","
                + "\"currency\":\"USD\",\"amount\":1000.00000000,"
                + "\"tradeDate\":\"" + DATE + "\",\"settleDate\":\"" + DATE + "\"}";
        assertEquals(HttpStatus.OK, post("/obligations", operator, clean).getStatusCode());

        ResponseEntity<String> net = post("/netting-runs", operator,
                "{\"settleDate\":\"" + DATE + "\",\"currency\":\"USD\"}");
        assertEquals(HttpStatus.OK, net.getStatusCode(), "netting should succeed after gate passed");
        JsonNode netBody = read(net);
        assertEquals("COMPLETED", netBody.get("run").get("status").asText());
        assertEquals(0, new BigDecimal(netBody.get("sumNetAmount").asText()).compareTo(BigDecimal.ZERO));

        // ---- 只读账号：可看结果，不能发起/改开关 ----
        assertEquals(HttpStatus.OK, get("/gate/runs", viewer).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,
                post("/gate/runs", viewer, "{\"settleDate\":\"" + DATE + "\"}").getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,
                post("/gate/configs/NON_POSITIVE_AMOUNT", viewer, "{\"enabled\":false}").getStatusCode());
    }

    private String newRawOpen(String payer, String payee, String ccy, String amount) {
        var o = new com.clearing.netting.domain.model.TradeObligation(
                java.util.UUID.randomUUID().toString(),
                payer, payee, ccy, new BigDecimal(amount),
                DATE.minusDays(1), DATE,
                com.clearing.netting.domain.model.ObligationStatus.OPEN, null);
        return obligationRepository.save(o).getObligationId();
    }
}
