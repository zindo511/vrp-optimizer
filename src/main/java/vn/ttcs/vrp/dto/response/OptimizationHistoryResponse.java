package vn.ttcs.vrp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO cho mỗi dòng trong lịch sử optimization
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationHistoryResponse {

    private Long id;
    private LocalDateTime runDate;
    private String algorithmName;
    private String runByEmail;
    private Integer totalOrders;
    private Integer totalVehicles;
    private BigDecimal totalDistanceKm;
    private BigDecimal totalCost;
    private Long executionTimeMs;
    private String status;
    private String errorMessage;
}
