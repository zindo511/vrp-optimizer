package vn.ttcs.vrp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.ttcs.vrp.dto.ApiResponse;
import vn.ttcs.vrp.dto.response.DriverPerformanceResponse;
import vn.ttcs.vrp.dto.response.ReportSummaryResponse;
import vn.ttcs.vrp.service.ReportService;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ReportSummaryResponse>> getSummary() {
        ReportSummaryResponse summary = reportService.getSummary();
        return ResponseEntity.ok(ApiResponse.success("Báo cáo tổng hợp", summary));
    }

    @GetMapping("/drivers")
    public ResponseEntity<ApiResponse<List<DriverPerformanceResponse>>> getDriverPerformance() {
        List<DriverPerformanceResponse> data = reportService.getDriverPerformance();
        return ResponseEntity.ok(ApiResponse.success("Hiệu suất tài xế", data));
    }
}
