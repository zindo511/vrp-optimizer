package vn.ttcs.vrp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import vn.ttcs.vrp.enums.RouteStatus;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class RouteResponse {

    private Long id;
    private String vehicleLicensePlate;
    private String vehicleTypeName;
    private String driverName;
    private String driverPhone;
    private RouteStatus status;
    private Double totalDistanceKm;
    private Long totalDurationMinutes;
    private Double totalWeightKg;
    private List<RouteStopResponse> routeStops;

}
