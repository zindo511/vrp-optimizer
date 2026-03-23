package vn.ttcs.vrp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import vn.ttcs.vrp.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String customerName;
    private String customerPhone;
    private LocationResponse location;
    private BigDecimal totalWeightKg;
    private BigDecimal totalVolumeM3;
    private String note;
    private LocalTime timeWindowFrom;
    private LocalTime timeWindowTo;
    private Integer serviceTimeMinutes;
    private OrderStatus status;
    private Long createdBy;
    private LocalDateTime createdAt;
}
