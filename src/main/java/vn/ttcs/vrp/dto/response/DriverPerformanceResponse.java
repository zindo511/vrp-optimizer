package vn.ttcs.vrp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Hiệu suất từng tài xế: đếm số stop đã giao, thất bại, tổng tuyến
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverPerformanceResponse {

    private Long driverId;
    private String driverName;
    private String phone;
    private String status;

    private long totalRoutes;       // tổng số tuyến được phân
    private long completedStops;    // số stop giao thành công (COMPLETED)
    private long failedStops;       // số stop giao thất bại (FAILED)
    private long totalStops;        // tổng số stop = completed + failed + waiting + ...

    /**
     * Tỉ lệ giao thành công = completedStops / totalStops × 100
     */
    private double successRate;
}
