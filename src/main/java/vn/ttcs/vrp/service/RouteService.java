package vn.ttcs.vrp.service;

public interface RouteService {

    // Admin gán tài xế cho tuyến đường
    void assginDriverToRoute(Long routeId, Long driverId);
}
