package vn.ttcs.vrp.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ttcs.vrp.dto.request.StopStatusRequest;
import vn.ttcs.vrp.dto.response.MyRouteResponse;
import vn.ttcs.vrp.dto.response.RouteStopResponse;
import vn.ttcs.vrp.enums.OrderStatus;
import vn.ttcs.vrp.enums.RouteStatus;
import vn.ttcs.vrp.enums.RouteStopStatus;
import vn.ttcs.vrp.exception.ResourceNotFoundException;
import vn.ttcs.vrp.model.Driver;
import vn.ttcs.vrp.model.Route;
import vn.ttcs.vrp.model.RouteStop;
import vn.ttcs.vrp.model.User;
import vn.ttcs.vrp.repository.DriverRepository;
import vn.ttcs.vrp.repository.RouteRepository;
import vn.ttcs.vrp.repository.RouteStopRepository;
import vn.ttcs.vrp.repository.UserRepository;
import vn.ttcs.vrp.service.DriverOperationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "DRIVER-OPERATION-SERVICE")
public class DriverOperationServiceImpl implements DriverOperationService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;

    // API: tài xế lấy lộ trình hôm nay
    @Override
    @Transactional(readOnly = true)
    public MyRouteResponse getMyTodayRoutes() {
        Driver driver = getCurrentUser();

        Route route = routeRepository.findByDriverAndRouteDate(driver, LocalDate.now())
                .orElseThrow(() -> new ResourceNotFoundException("Bạn chưa có lộ trình nào ngày hôm này: " + LocalDate.now()));

        log.info("Tài xế {} lấy lộ trình ngày {}", driver.getUser().getEmail(), LocalDate.now());

        List<RouteStopResponse> stops = route.getRouteStops().stream()
                .sorted(Comparator.comparing(RouteStop::getStopOrder))
                .map(this::routeStopResponse)
                .toList();

        return MyRouteResponse.builder()
                .routeId(route.getId())
                .routeDate(route.getRouteDate())
                .vehicleLicensePlate(route.getVehicle().getLicensePlate())
                .routeStatus(route.getStatus())
                .totalDistanceKm(route.getTotalDistanceMeters().doubleValue() / 1000)
                .totalStops(stops.size())
                .stops(stops)
                .build();
    }

    // API: cập nhật trạng thái giao hàng
    @Override
    @Transactional
    public void updateStopStatus(Long id, StopStatusRequest request) {
        Driver driver = getCurrentUser();

        RouteStop stop = routeStopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dừng id: " + id));

        // khác với driver -> ko cho phép
        if (!stop.getRoute().getDriver().getId().equals(driver.getId())) {
            throw new IllegalStateException("Bạn không có quyền truy cập điểm dừng này");
        }

        stop.setActualArrival(LocalDateTime.now());
        stop.setStatus(request.getStopStatus());
        stop.setProofImageUrl(request.getProofImageUrl());
        stop.setNote(request.getNote());
        stop.setFailureReason(request.getFailureReason());
        routeStopRepository.save(stop);

        // cập nhật trạng thái đơn hàng
        if (stop.getOrder() != null) {
            OrderStatus newStatus = (request.getStopStatus() == RouteStopStatus.COMPLETED)
                    ? OrderStatus.COMPLETED : OrderStatus.FAILED;
            stop.getOrder().setStatus(newStatus);
        }

        Route route = stop.getRoute();
        if (route.getStatus() == RouteStatus.PLANNED)
            route.setStatus(RouteStatus.IN_PROGRESS);


        boolean allConfirmed = route.getRouteStops().stream()
                        .allMatch(s -> s.getStatus() == RouteStopStatus.COMPLETED || s.getStatus() == RouteStopStatus.FAILED);

        if (allConfirmed) {
            route.setStatus(RouteStatus.COMPLETED);
            log.info("Tuyến đường: {} đã hoàn thành", route.getId());
        }

        log.info("Stop: {}, tình trạng: {} bởi tài xế: {}", id, request.getStopStatus(), driver.getUser().getEmail());
    }


    // ===== PRIVATE HEPLPER =====
    private Driver getCurrentUser() {
        String email = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user: " + email));
        return driverRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản này chưa được gắn với tài xế nào"));
    }

    private RouteStopResponse routeStopResponse(RouteStop stop) {
        return RouteStopResponse.builder()
                .id(stop.getId())
                .stopOrder(stop.getStopOrder())
                .customerName(stop.getOrder() != null ? stop.getOrder().getCustomerName() : null)
                .customerPhone(stop.getOrder() != null ? stop.getOrder().getCustomerPhone() : null)
                .address(stop.getLocation().getAddress())
                .lat(stop.getLocation().getLatitude().doubleValue())
                .lng(stop.getLocation().getLongitude().doubleValue())
                .estimatedArrival(stop.getEstimatedArrival())
                .actualArrival(stop.getActualArrival())
                .status(stop.getStatus())
                .proofImageUrl(stop.getProofImageUrl())
                .note(stop.getNote())
                .weightKg(stop.getOrder() != null ? stop.getOrder().getTotalWeightKg().doubleValue() : null)
                .build();
    }
}
