package vn.ttcs.vrp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Tổng hợp các chỉ số vận hành chính cho trang Reports
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummaryResponse {

    // ── Đơn hàng ─────────────────────────────────
    private long totalOrders;
    private Map<String, Long> ordersByStatus;  // PENDING: 10, COMPLETED: 45, ...

    // ── Khoảng cách & chi phí ────────────────────
    private BigDecimal totalDistanceKm;        // tổng km tất cả lần optimization
    private BigDecimal totalCostVnd;           // tổng chi phí vận hành

    // ── Tài xế ───────────────────────────────────
    private long totalDrivers;
    private long activeDrivers;

    // ── Xe ────────────────────────────────────────
    private long totalVehicles;
    private long availableVehicles;
    private long maintenanceVehicles;

    // ── Optimization ─────────────────────────────
    private long totalOptimizationRuns;
    private long successfulRuns;
}
