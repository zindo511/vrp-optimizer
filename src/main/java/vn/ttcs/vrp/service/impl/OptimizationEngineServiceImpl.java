package vn.ttcs.vrp.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import vn.ttcs.vrp.dto.request.OptimizationRequest;
import vn.ttcs.vrp.dto.vrp.PlannedRouteDto;
import vn.ttcs.vrp.dto.vrp.VrpSolutionDto;
import vn.ttcs.vrp.enums.OrderStatus;
import vn.ttcs.vrp.enums.VehicleStatus;
import vn.ttcs.vrp.exception.ResourceNotFoundException;
import vn.ttcs.vrp.model.*;
import vn.ttcs.vrp.repository.*;
import vn.ttcs.vrp.service.DistanceMatrixService;
import vn.ttcs.vrp.service.OptimizationEngineService;
import vn.ttcs.vrp.solver.VrpSolver;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "OPTIMIZATION-ENGINE")
public class OptimizationEngineServiceImpl implements OptimizationEngineService {

    private final AlgorithmConfigRepository algorithmConfigRepository;
    private final DepotRepository depotRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;
    private final OptimizationResultRepository optimizationResultRepository;
    private final DistanceMatrixService distanceMatrixService;
    private final Map<String, VrpSolver> solverMap;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;

    @Override
    public OptimizationResult runOptimization(OptimizationRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Bắt đầu chạy phân tuyến — Depot: {}, Ngày: {}", request.getDepotId(), request.getRouteDate());

        // ===== Lấy cấu hình =====
        AlgorithmConfig config = algorithmConfigRepository.findById(request.getAlgorithmConfigId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy AlgorithmConfig id: " + request.getAlgorithmConfigId()));

        Depot depot = depotRepository.findById(request.getDepotId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Depot id: " + request.getDepotId()));

        String username = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        User currentUser = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user: " + username));

        // ==== Thu thập dữ liệu =====
        List<Order> pendingOrders = orderRepository.findAllByStatus(OrderStatus.PENDING);
        List<Vehicle> availableVehicles = vehicleRepository.findAllByStatus(VehicleStatus.AVAILABLE);

        if (pendingOrders.isEmpty()) {
            log.warn("Không có đơn hàng PENDING nào để phân tuyến");
            return saveFailedResult(config, currentUser, "Không có đơn hàng PENDING", startTime);
        }

        if (availableVehicles.isEmpty()) {
            log.warn("Không có xe AVAILABLE nào");
            return saveFailedResult(config, currentUser, "Không có xe khả dạng", startTime);
        }

        log.info("{} đơn PENDIND | {} xe AVAILABLE", pendingOrders.size(), availableVehicles.size());

        // ===== Chuẩn bị Ma trận khoảng cách  =====
        Location depotLocation = depot.getLocation();
        List<Location> allLocations = new ArrayList<>();
        allLocations.add(depotLocation);
        pendingOrders.forEach(order -> allLocations.add(order.getLocation()));

        List<DistanceMatrix> matrixList = distanceMatrixService.getDistanceMatrix(allLocations);

        // chuyển thành hashMap để tra cứu cho nhanh
        Map<String, DistanceMatrix> distanceMap = matrixList.stream()
                .collect(Collectors.toMap(
                        d -> d.getOrigin().getId() + "-" + d.getDestination().getId(),
                        d -> d,
                        (existing, replacement) -> existing // giữ cái cũ nếu trùng key
                ));

        // ===== Chọn và chạy thuật toán =====
        VrpSolver solver = selectSolver(config);
        VrpSolutionDto solution = solver.solve(pendingOrders, availableVehicles, depotLocation, distanceMap, config);

        // ===== Lưu kết quả xuống DB =====
        double totalDist = solution.getTotalDistanceMeters();
        OptimizationResult result = OptimizationResult.builder()
                .config(config)
                .user(currentUser)
                .totalOrders(pendingOrders.size() - solution.getUnassignedOrderCount())
                .totalVehicles(solution.getRoutes().size())
                .totalDistance(BigDecimal.valueOf(totalDist / 1000)) // km
                .executionTimeMs(System.currentTimeMillis() - startTime)
                .status("SUCCESS")
                .build();

        // lưu route và routeStop cho từng xe
        for (PlannedRouteDto planned : solution.getRoutes()) {
            Route route = Route.builder()
                    .result(result)
                    .startDepot(depot)
                    .vehicle(planned.getVehicle())
                    .totalDistanceMeters(BigDecimal.valueOf(planned.getTotalDistanceMeters()))
                    .totalDurationSeconds(planned.getTotalDurationSeconds())
                    .totalWeightKg(BigDecimal.valueOf(planned.getTotalWeightKg()))
                    .build();
            routeRepository.save(route);

            // lưu routeStop theo stop_order
            List<RouteStop> stops = new ArrayList<>();
            int stopOrder = 1;
            LocalTime depotStartTime = depot.getStartTime() != null
                    ? depot.getStartTime() : LocalTime.of(7, 0);
            LocalDateTime estimatedTime = route.getRouteDate().atTime(depotStartTime);

            for (Order order : planned.getOrderStops()) {
                // Ước tính thời gian ở vị trí stop trước đến vị trí tiếp theo cần tới
                String key = (stopOrder == 1 ? depotLocation.getId()
                        : planned.getOrderStops().get(stopOrder - 2).getLocation().getId())
                        + "-" + order.getLocation().getId();
                DistanceMatrix dm = distanceMap.get(key);
                if (dm != null) {
                    estimatedTime = estimatedTime.plusSeconds(dm.getDurationSeconds());
                }

                stops.add(RouteStop.builder()
                                .route(route)
                                .order(order)
                                .location(order.getLocation())
                                .stopOrder(stopOrder++)
                                .estimatedArrival(estimatedTime)
                        .build());

                // cộng thêm thời gian phục vụ
                estimatedTime = estimatedTime.plusMinutes(
                        order.getServiceTimeMinutes() != null
                        ?  order.getServiceTimeMinutes() : 15
                );
            }
            routeStopRepository.saveAll(stops);

            // cập nhật trạng thái xe -> IN_USE
            planned.getVehicle().setStatus(VehicleStatus.IN_USE);
            vehicleRepository.save(planned.getVehicle());
        }

        // cập nhật trạng thái đơn hàng đã được phân -> ASSIGNED
        List<Order> assignedOrders = solution.getRoutes().stream()
                .flatMap(r -> r.getOrderStops().stream())
                .toList();
        assignedOrders.forEach(o -> o.setStatus(OrderStatus.ASSIGNED));
        orderRepository.saveAll(assignedOrders);

        log.info("Phân tuyến hoàn tất! {} tuyến, {} đơn đã phân, {} đơn chưa phân. Tổng: {}km — {}ms",
                solution.getRoutes().size(),
                assignedOrders.size(),
                solution.getUnassignedOrderCount(),
                totalDist / 1000,
                System.currentTimeMillis() - startTime);

        return result;
    }

    // ===== PRIVATE HEPLER =====

    private VrpSolver selectSolver(AlgorithmConfig config) { // sau nhiều thuật toán thì dùng config.getName()
        VrpSolver solver = solverMap.get("nearestNeightborSolver");
        if (solver == null) {
            throw new ResourceNotFoundException("Solver không tìm thấy");
        }
        log.info("Sử dụng solver: nearestNeightborSolver");
        return solver;
    }

    private OptimizationResult saveFailedResult(
            AlgorithmConfig config,
            User user,
            String errorMessage,
            long startTime
    ) {
        OptimizationResult failed = OptimizationResult.builder()
                .config(config)
                .user(user)
                .totalOrders(0)
                .totalVehicles(0)
                .executionTimeMs(System.currentTimeMillis() - startTime)
                .status("FAILED")
                .errorMessage(errorMessage)
                .build();
        return optimizationResultRepository.save(failed);
    }
}
