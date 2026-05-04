package vn.ttcs.vrp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import vn.ttcs.vrp.dto.ApiResponse;
import vn.ttcs.vrp.dto.request.OptimizationRequest;
import vn.ttcs.vrp.dto.response.OptimizationHistoryResponse;
import vn.ttcs.vrp.dto.response.OptimizationJobResponse;
import vn.ttcs.vrp.model.OptimizationResult;
import vn.ttcs.vrp.service.OptimizationEngineService;
import vn.ttcs.vrp.service.ReportService;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/optimization")
@RequiredArgsConstructor
@Slf4j(topic = "OPTIMIZATION-CONTROLLER")
public class OptimizationController {

    private final OptimizationEngineService optimizationEngineService;
    private final ReportService reportService;
    private final Environment environment;

    // ═══════════════════════════════════════════════════════════════════
    // ASYNC ENDPOINTS — Production-ready
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Submit optimization job — trả về jobId ngay lập tức (không chờ kết quả).
     * Client poll trạng thái qua GET /api/optimization/status/{jobId}
     */
    @PostMapping("/run")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<OptimizationJobResponse>> runOptimization(
            @Valid @RequestBody OptimizationRequest request) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Nhận lệnh phân tuyến từ {} — Depot: {}, Ngày: {}",
                username, request.getDepotId(), request.getRouteDate());

        String jobId = optimizationEngineService.submitOptimizationJob(request, username);

        OptimizationJobResponse response = OptimizationJobResponse.builder()
                .jobId(jobId)
                .status("QUEUED")
                .message("Đã gửi yêu cầu tối ưu hoá. Sử dụng jobId để theo dõi tiến trình.")
                .progressPercent(0)
                .elapsedMs(0L)
                .build();

        return ResponseEntity.accepted().body(ApiResponse.success("Đã gửi job phân tuyến!", response));
    }

    /**
     * Poll trạng thái của optimization job.
     *
     * Response status flow: QUEUED → RUNNING → SUCCESS / FAILED
     * Khi SUCCESS, response chứa resultId để fetch kết quả chi tiết.
     */
    @GetMapping("/status/{jobId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<OptimizationJobResponse>> getJobStatus(
            @PathVariable String jobId) {

        OptimizationJobResponse status = optimizationEngineService.getJobStatus(jobId);
        return ResponseEntity.ok(ApiResponse.success("Trạng thái job", status));
    }

    // ═══════════════════════════════════════════════════════════════════
    // HISTORY — xem lịch sử phân tuyến
    // ═══════════════════════════════════════════════════════════════════

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<List<OptimizationHistoryResponse>>> getHistory() {
        List<OptimizationHistoryResponse> data = reportService.getOptimizationHistory();
        return ResponseEntity.ok(ApiResponse.success("Lịch sử phân tuyến", data));
    }

    // ═══════════════════════════════════════════════════════════════════
    // RESET — chỉ khả dụng khi KHÔNG chạy profile "prod"
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Reset toàn bộ dữ liệu tối ưu hoá (routes, orders → PENDING, vehicles → AVAILABLE).
     *
     * ⚠️ GUARD: Bị chặn hoàn toàn khi spring.profiles.active=prod
     * ⚠️ GUARD: Yêu cầu role ADMIN
     */
    @PostMapping("/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> resetTestingData() {
        // ── Kiểm tra profile production ──────────────────────────────────
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isProduction = Arrays.stream(activeProfiles)
                .anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"));

        if (isProduction) {
            log.error("⛔ Từ chối reset dữ liệu: đang chạy trên môi trường PRODUCTION");
            return ResponseEntity.status(403)
                    .body(ApiResponse.error(403,
                            "Chức năng reset dữ liệu bị vô hiệu hoá trên môi trường production.",
                            "/api/optimization/reset"));
        }
        // ─────────────────────────────────────────────────────────────────

        log.warn("⚠️ ADMIN đang reset dữ liệu test — profiles: {}", Arrays.toString(activeProfiles));
        optimizationEngineService.resetTestingData();
        return ResponseEntity.ok(ApiResponse.success("Đã reset dữ liệu: Đơn hàng -> PENDING, Xe -> AVAILABLE", null));
    }
}
