package vn.ttcs.vrp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import vn.ttcs.vrp.enums.RouteStatus;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class MyRouteResponse {
    private Long routeId;
    private LocalDate routeDate;
    private String vehicleLicensePlate;
    private RouteStatus routeStatus;
    private double totalDistanceKm;
    private int totalStops;
    private List<RouteStopResponse> stops;
}
