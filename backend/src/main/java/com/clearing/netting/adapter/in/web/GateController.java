package com.clearing.netting.adapter.in.web;

import com.clearing.netting.adapter.in.web.auth.AuthContext;
import com.clearing.netting.adapter.in.web.auth.AuthUser;
import com.clearing.netting.application.GateApplicationService;
import com.clearing.netting.domain.model.GateCheckConfig;
import com.clearing.netting.domain.model.GateCheckResult;
import com.clearing.netting.domain.model.GateCheckType;
import com.clearing.netting.domain.model.GateRun;
import com.clearing.netting.domain.model.GateRunStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 日终门禁：检查单开关、发起检查运行、结果与历史查询。
 * 写操作（开关、发起运行）限操作员，查询对登录用户开放。
 */
@RestController
@RequestMapping("/api/eod-gate")
public class GateController {

    private final GateApplicationService gateService;

    public GateController(GateApplicationService gateService) {
        this.gateService = gateService;
    }

    @GetMapping("/checks")
    public List<CheckConfigResponse> checks() {
        AuthContext.require();
        return gateService.listChecks().stream()
                .map(CheckConfigResponse::from)
                .collect(Collectors.toList());
    }

    @PutMapping("/checks/{type}")
    public CheckConfigResponse updateCheck(
            @PathVariable("type") GateCheckType type,
            @Valid @RequestBody UpdateCheckRequest request) {
        AuthContext.requireOperator();
        return CheckConfigResponse.from(gateService.updateCheck(type, request.enabled()));
    }

    @PostMapping("/runs")
    public GateRunResponse run(@Valid @RequestBody RunGateRequest request) {
        AuthContext.requireOperator();
        AuthUser user = AuthContext.require();
        return GateRunResponse.from(gateService.run(request.businessDate(), user.username()));
    }

    @GetMapping("/runs")
    public List<GateRunResponse> listRuns() {
        AuthContext.require();
        return gateService.listRuns().stream()
                .map(GateRunResponse::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/runs/{id}")
    public GateRunResponse getRun(@PathVariable("id") String id) {
        AuthContext.require();
        return GateRunResponse.from(gateService.getRun(id));
    }

    @GetMapping("/status")
    public GateStatusResponse status(
            @RequestParam("businessDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate) {
        AuthContext.require();
        return gateService.latestFor(businessDate)
                .map(run -> new GateStatusResponse(
                        businessDate,
                        run.getStatus(),
                        run.getGateRunId(),
                        run.getCreatedBy(),
                        run.getCreatedAt()))
                .orElseGet(() -> new GateStatusResponse(businessDate, null, null, null, null));
    }

    public record UpdateCheckRequest(@NotNull Boolean enabled) {
    }

    public record RunGateRequest(@NotNull LocalDate businessDate) {
    }

    public record CheckConfigResponse(String type, String label, String description, boolean enabled) {
        static CheckConfigResponse from(GateCheckConfig c) {
            return new CheckConfigResponse(
                    c.getCheckType().name(),
                    c.getCheckType().getLabel(),
                    c.getCheckType().getDescription(),
                    c.isEnabled());
        }
    }

    public record CheckResultResponse(
            String type, String label, boolean enabled, boolean passed, List<String> details) {
        static CheckResultResponse from(GateCheckResult r) {
            return new CheckResultResponse(
                    r.getCheckType().name(),
                    r.getCheckType().getLabel(),
                    r.isEnabled(),
                    r.isPassed(),
                    r.getDetails());
        }
    }

    public record GateRunResponse(
            String gateRunId,
            LocalDate businessDate,
            GateRunStatus status,
            String createdBy,
            Instant createdAt,
            long failedCount,
            List<CheckResultResponse> results) {
        static GateRunResponse from(GateRun r) {
            return new GateRunResponse(
                    r.getGateRunId(),
                    r.getBusinessDate(),
                    r.getStatus(),
                    r.getCreatedBy(),
                    r.getCreatedAt(),
                    r.failedCount(),
                    r.getResults().stream().map(CheckResultResponse::from).collect(Collectors.toList()));
        }
    }

    public record GateStatusResponse(
            LocalDate businessDate,
            GateRunStatus status,
            String gateRunId,
            String createdBy,
            Instant createdAt) {
    }
}
