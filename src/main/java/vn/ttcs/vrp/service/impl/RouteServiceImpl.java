package vn.ttcs.vrp.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ttcs.vrp.exception.ResourceNotFoundException;
import vn.ttcs.vrp.model.Driver;
import vn.ttcs.vrp.model.Route;
import vn.ttcs.vrp.repository.DriverRepository;
import vn.ttcs.vrp.repository.RouteRepository;
import vn.ttcs.vrp.service.RouteService;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ROUTE-SERVICE")
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final DriverRepository driverRepository;

    @Override
    @Transactional
    public void assginDriverToRoute(Long routeId, Long driverId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tuyến đường với id: " + routeId));

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài xế với id: " + driverId));
        if (route.getDriver() == null)
            route.setDriver(driver);

        log.info("Gán thành công tuyến đường: {} cho tài xế: {}", routeId, driverId);
    }
}
