package vn.ttcs.vrp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import vn.ttcs.vrp.enums.RouteStopStatus;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class RouteStopResponse {
    private Long id;
    private Integer stopOrders;
    private String customerName;
    private String customerPhone;
    private String address;
    private Double lat;
    private Double lng;
    private LocalDateTime estimateArrival;
    private LocalDateTime actualArrival;
    private RouteStopStatus status;
    private String proofImageUrl;
    private String note;
    private Double weightKg;
}
