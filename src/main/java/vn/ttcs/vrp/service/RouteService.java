package vn.ttcs.vrp.service;

import vn.ttcs.vrp.dto.response.RouteResponse;
import vn.ttcs.vrp.enums.RouteStatus;

import java.time.LocalDate;
import java.util.List;

public interface RouteService {

    // Admin gán tài xế cho tuyến đường
    void assignDriverToRoute(Long routeId, Long driverId);

    // Lấy route theo id
    RouteResponse getRouteById(Long routeId);

    // Lấy route theo date
    List<RouteResponse> getRouteByDate(LocalDate date, RouteStatus status);

    // Cập nhật trạng thái tuyến
    RouteResponse updateRouteByStatus(Long routeId, RouteStatus status);
}
