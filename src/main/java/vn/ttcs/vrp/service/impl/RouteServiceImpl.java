package vn.ttcs.vrp.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ttcs.vrp.dto.response.RouteResponse;
import vn.ttcs.vrp.enums.RouteStatus;
import vn.ttcs.vrp.exception.ResourceNotFoundException;
import vn.ttcs.vrp.mapper.RouteMapper;
import vn.ttcs.vrp.model.Driver;
import vn.ttcs.vrp.model.Route;
import vn.ttcs.vrp.repository.DriverRepository;
import vn.ttcs.vrp.repository.RouteRepository;
import vn.ttcs.vrp.service.RouteService;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ROUTE-SERVICE")
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final DriverRepository driverRepository;
    private final RouteMapper routeMapper;

    @Override
    @Transactional
    public void assignDriverToRoute(Long routeId, Long driverId) {
        Route route = findRouteById(routeId);

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài xế với id: " + driverId));
        if (route.getDriver() == null)
            route.setDriver(driver);

        log.info("Gán thành công tuyến đường: {} cho tài xế: {}", routeId, driverId);
    }

    @Override
    @Transactional(readOnly = true)
    public RouteResponse getRouteById(Long routeId) {

        Route route = findRouteById(routeId);
        return routeMapper.toRouteResponse(route);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteResponse> getRouteByDate(LocalDate date, RouteStatus status) {

        List<Route> route = routeRepository.findAllByRouteDateAndStatus(date, status);
        return route.stream().map(routeMapper::toRouteResponse).toList();
    }

    @Override
    @Transactional
    public RouteResponse updateRouteByStatus(Long routeId, RouteStatus status) {
        Route route = findRouteById(routeId);
        if (status != null) {
            route.setStatus(status);
        }

        return routeMapper.toRouteResponse(route);
    }


    // ===== PRIVATE HELPER =====
    private Route findRouteById(Long routeId) {
        return routeRepository.findByIdWithDetails(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tuyến đường với id: " + routeId));
    }
}
