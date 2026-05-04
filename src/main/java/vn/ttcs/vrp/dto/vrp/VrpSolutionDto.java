package vn.ttcs.vrp.dto.vrp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO tạm trong RAM - chứa toàn bộ kết quả của 1 lần chạy thuật toán
 * VrpSolver trả về cái này, Engine nhận vào và lưu DB
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VrpSolutionDto {

    private List<PlannedRouteDto> routes;

    private int unassignedOrderCount;

    private double totalDistanceMeters;

    /**
     * Tổng chi phí vận hành = Σ(fixedCost + costPerKm × km) của tất cả các tuyến
     */
    private double totalCostVnd;

    // ═══════════════════════════════════════════════════════════════════
    // ISSUE #11: CAPACITY UTILIZATION METRICS
    // ═══════════════════════════════════════════════════════════════════
    // Đo hiệu suất sử dụng tải trọng và dung tích xe.
    // Giá trị thấp → xe chạy non tải → lãng phí chi phí cố định.
    // Mục tiêu: > 70% trung bình là tốt cho logistics.
    // ═══════════════════════════════════════════════════════════════════

    /** Trung bình % tải trọng đã sử dụng trên tất cả xe (0.0 - 100.0) */
    @Builder.Default
    private double avgWeightUtilizationPercent = 0;

    /** Trung bình % dung tích đã sử dụng trên tất cả xe (0.0 - 100.0) */
    @Builder.Default
    private double avgVolumeUtilizationPercent = 0;

    /** Xe có utilization thấp nhất (%) — cảnh báo non-tải */
    @Builder.Default
    private double minWeightUtilizationPercent = 0;

    /** Xe có utilization cao nhất (%) */
    @Builder.Default
    private double maxWeightUtilizationPercent = 0;
}
