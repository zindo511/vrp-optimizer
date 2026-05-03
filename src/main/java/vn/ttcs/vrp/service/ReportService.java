package vn.ttcs.vrp.service;

import vn.ttcs.vrp.dto.response.DriverPerformanceResponse;
import vn.ttcs.vrp.dto.response.OptimizationHistoryResponse;
import vn.ttcs.vrp.dto.response.ReportSummaryResponse;

import java.util.List;

public interface ReportService {

    ReportSummaryResponse getSummary();

    List<OptimizationHistoryResponse> getOptimizationHistory();

    List<DriverPerformanceResponse> getDriverPerformance();
}
