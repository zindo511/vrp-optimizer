package vn.ttcs.vrp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import vn.ttcs.vrp.dto.ApiResponse;
import vn.ttcs.vrp.dto.request.OptimizationRequest;
import vn.ttcs.vrp.dto.response.OptimizationHistoryResponse;
import vn.ttcs.vrp.model.OptimizationResult;
import vn.ttcs.vrp.service.OptimizationEngineService;
import vn.ttcs.vrp.service.ReportService;

import java.util.List;

@RestController
@RequestMapping("/api/optimization")
@RequiredArgsConstructor
@Slf4j(topic = "OPTIMIZATION-CONTROLLER")
public class OptimizationController {

    private final OptimizationEngineService optimizationEngineService;
    private final ReportService reportService;

    @PostMapping("/run")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<OptimizationResult>> runOptimization(
            @Valid @RequestBody OptimizationRequest request) {
        log.info("Nhận lệnh phân tuyến từ Admin — Depot: {}, Ngày: {}", request.getDepotId(), request.getRouteDate());
        OptimizationResult result = optimizationEngineService.runOptimization(request);
        return ResponseEntity.ok(ApiResponse.success("Phân tuyến hoàn tất!", result));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<List<OptimizationHistoryResponse>>> getHistory() {
        List<OptimizationHistoryResponse> data = reportService.getOptimizationHistory();
        return ResponseEntity.ok(ApiResponse.success("Lịch sử phân tuyến", data));
    }
}
