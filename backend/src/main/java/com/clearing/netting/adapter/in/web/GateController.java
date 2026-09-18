package com.clearing.netting.adapter.in.web;

import com.clearing.netting.adapter.in.web.auth.AuthContext;
import com.clearing.netting.application.GateApplicationService;
import com.clearing.netting.domain.model.GateCheckConfig;
import com.clearing.netting.domain.model.GateCheckResult;
import com.clearing.netting.domain.model.GateCheckType;
import com.clearing.netting.domain.model.GateFinding;
import com.clearing.netting.domain.model.GateItemStatus;
import com.clearing.netting.domain.model.GateRun;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/gate")
public class GateController {

    private final GateApplicationService gateService;

    public GateController(GateApplicationService gateService) {
        this.gateService = gateService;
    }

    @GetMapping("/configs")
    public List<ConfigResponse> configs() {
        AuthContext.require();
        return gateService.listConfigs().stream().map(ConfigResponse::from).collect(Collectors.toList());
    }

    @PostMapping("/configs/{type}")
    public ConfigResponse toggle(@PathVariable("type") GateCheckType type, @Valid @RequestBody ToggleRequest request) {
        AuthContext.requireOperator();
        return ConfigResponse.from(gateService.setEnabled(type, request.enabled()));
    }

    @PostMapping("/runs")
    public GateRunResponse run(@Valid @RequestBody RunRequest request) {
        AuthContext.requireOperator();
        String operator = AuthContext.get().username();
        return GateRunResponse.from(gateService.runCheck(request.settleDate(), operator));
    }

    @GetMapping("/runs")
    public List<GateRunSummary> runs() {
        AuthContext.require();
        return gateService.listRuns().stream().map(GateRunSummary::from).collect(Collectors.toList());
    }

    @GetMapping("/runs/{id}")
    public GateRunResponse get(@PathVariable("id") String id) {
        AuthContext.require();
        return GateRunResponse.from(gateService.getRun(id));
    }

    @GetMapping("/status")
    public StatusResponse status(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate settleDate) {
        AuthContext.require();
        GateApplicationService.GateStatus s = gateService.statusFor(settleDate);
        return new StatusResponse(s.passed(), s.hasRun(), s.runId(), s.status(), s.createdAt());
    }

    public record ToggleRequest(@NotNull Boolean enabled) {
    }

    public record RunRequest(@NotNull LocalDate settleDate) {
    }

    public record ConfigResponse(GateCheckType checkType, String title, String description, boolean enabled) {
        static ConfigResponse from(GateCheckConfig c) {
            return new ConfigResponse(
                    c.getCheckType(),
                    c.getCheckType().getTitle(),
                    c.getCheckType().getDescription(),
                    c.isEnabled());
        }
    }

    public record FindingResponse(
            String targetType,
            String targetId,
            String memberId,
            String memberName,
            String currency,
            String amount,
            LocalDate settleDate,
            String detail) {
        static FindingResponse from(GateFinding f) {
            return new FindingResponse(
                    f.getTargetType(),
                    f.getTargetId(),
                    f.getMemberId(),
                    f.getMemberName(),
                    f.getCurrency(),
                    f.getAmount() == null ? null : f.getAmount().toPlainString(),
                    f.getSettleDate(),
                    f.getDetail());
        }
    }

    public record CheckResponse(
            GateCheckType checkType,
            String title,
            GateItemStatus status,
            String message,
            List<FindingResponse> findings) {
        static CheckResponse from(GateCheckResult r) {
            return new CheckResponse(
                    r.getCheckType(),
                    r.getCheckType().getTitle(),
                    r.getStatus(),
                    r.getMessage(),
                    r.getFindings().stream().map(FindingResponse::from).collect(Collectors.toList()));
        }
    }

    public record GateRunSummary(
            String runId,
            LocalDate settleDate,
            String operator,
            String status,
            Instant createdAt,
            int passCount,
            int failCount,
            int skipCount) {
        static GateRunSummary from(GateRun r) {
            int pass = 0;
            int fail = 0;
            int skip = 0;
            for (GateCheckResult x : r.getResults()) {
                switch (x.getStatus()) {
                    case PASS -> pass++;
                    case FAIL -> fail++;
                    case SKIPPED -> skip++;
                }
            }
            return new GateRunSummary(
                    r.getRunId(),
                    r.getSettleDate(),
                    r.getOperator(),
                    r.getStatus().name(),
                    r.getCreatedAt(),
                    pass, fail, skip);
        }
    }

    public record GateRunResponse(
            String runId,
            LocalDate settleDate,
            String operator,
            String status,
            Instant createdAt,
            boolean passed,
            List<CheckResponse> results) {
        static GateRunResponse from(GateRun r) {
            return new GateRunResponse(
                    r.getRunId(),
                    r.getSettleDate(),
                    r.getOperator(),
                    r.getStatus().name(),
                    r.getCreatedAt(),
                    r.passed(),
                    r.getResults().stream().map(CheckResponse::from).collect(Collectors.toList()));
        }
    }

    public record StatusResponse(boolean passed, boolean hasRun, String runId, String status, Instant createdAt) {
    }
}
