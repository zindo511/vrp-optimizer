package vn.ttcs.vrp.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ttcs.vrp.dto.request.OptimizationRequest;
import vn.ttcs.vrp.dto.response.OptimizationJobResponse;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
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
        private final DriverRepository driverRepository;
        private final OptimizationResultRepository optimizationResultRepository;
        private final DistanceMatrixService distanceMatrixService;
        private final Map<String, VrpSolver> solverMap;
        private final RouteRepository routeRepository;
        private final RouteStopRepository routeStopRepository;

        // ═══════════════════════════════════════════════════════════════════
        // ASYNC JOB TRACKING
        // ═══════════════════════════════════════════════════════════════════
        // In-memory map lưu trạng thái job. Trong production lớn nên dùng Redis,
        // nhưng ConcurrentHashMap đủ cho single-instance deployment.
        // ═══════════════════════════════════════════════════════════════════
        private final ConcurrentHashMap<String, OptimizationJobResponse> jobStatusMap = new ConcurrentHashMap<>();

        /**
         * Mutex ngăn chặn 2 optimization job chạy đồng thời trên cùng dữ liệu.
         * Nếu 2 user submit cùng lúc → user thứ 2 phải chờ user thứ 1 xong.
         * Tránh race condition: cùng lấy PENDING orders → duplicate assignment.
         */
        private final ReentrantLock optimizationLock = new ReentrantLock();

        // ═══════════════════════════════════════════════════════════════════
        // PUBLIC: Submit async job
        // ═══════════════════════════════════════════════════════════════════
        @Override
        public String submitOptimizationJob(OptimizationRequest request, String username) {
                String jobId = UUID.randomUUID().toString();

                jobStatusMap.put(jobId, OptimizationJobResponse.builder()
                        .jobId(jobId)
                        .status("QUEUED")
                        .message("Đang chờ xử lý...")
                        .progressPercent(0)
                        .elapsedMs(0L)
                        .build());

                // Gọi method async — chạy trên thread pool "optimizationExecutor"
                executeOptimizationAsync(jobId, request, username);

                return jobId;
        }

        @Override
        public OptimizationJobResponse getJobStatus(String jobId) {
                OptimizationJobResponse status = jobStatusMap.get(jobId);
                if (status == null) {
                        return OptimizationJobResponse.builder()
                                .jobId(jobId)
                                .status("NOT_FOUND")
                                .message("Không tìm thấy job: " + jobId)
                                .build();
                }
                return status;
        }

        // ═══════════════════════════════════════════════════════════════════
        // ASYNC EXECUTION — chạy trên thread pool riêng
        // ═══════════════════════════════════════════════════════════════════
        @Async("optimizationExecutor")
        public void executeOptimizationAsync(String jobId, OptimizationRequest request, String username) {
                long startTime = System.currentTimeMillis();

                try {
                        // Cập nhật trạng thái → RUNNING
                        updateJobStatus(jobId, "RUNNING", "Đang chuẩn bị dữ liệu...", 5, startTime);

                        // Acquire lock — nếu job khác đang chạy, chờ tối đa 30s
                        if (!optimizationLock.tryLock(30, java.util.concurrent.TimeUnit.SECONDS)) {
                                updateJobStatus(jobId, "FAILED",
                                        "Hệ thống đang xử lý một yêu cầu tối ưu hoá khác. Vui lòng thử lại sau.", 0, startTime);
                                return;
                        }

                        try {
                                // Chạy optimization thực tế (đồng bộ bên trong)
                                OptimizationResult result = runOptimizationInternal(request, username, jobId, startTime);

                                if ("FAILED".equals(result.getStatus())) {
                                        updateJobStatus(jobId, "FAILED",
                                                result.getErrorMessage(), 100, startTime);
                                } else {
                                        jobStatusMap.put(jobId, OptimizationJobResponse.builder()
                                                .jobId(jobId)
                                                .status("SUCCESS")
                                                .message("Phân tuyến hoàn tất!")
                                                .progressPercent(100)
                                                .elapsedMs(System.currentTimeMillis() - startTime)
                                                .resultId(result.getId())
                                                .build());
                                }
                        } finally {
                                optimizationLock.unlock();
                        }

                } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        updateJobStatus(jobId, "FAILED", "Job bị hủy", 0, startTime);
                } catch (Exception e) {
                        log.error("Lỗi không xử lý được trong optimization job {}: {}", jobId, e.getMessage(), e);
                        updateJobStatus(jobId, "FAILED",
                                "Lỗi hệ thống: " + e.getMessage(), 0, startTime);
                }
        }

        private void updateJobStatus(String jobId, String status, String message, int progress, long startTime) {
                jobStatusMap.put(jobId, OptimizationJobResponse.builder()
                        .jobId(jobId)
                        .status(status)
                        .message(message)
                        .progressPercent(progress)
                        .elapsedMs(System.currentTimeMillis() - startTime)
                        .build());
        }

        // ═══════════════════════════════════════════════════════════════════
        // CORE LOGIC — chạy đồng bộ, có progress callback
        // ═══════════════════════════════════════════════════════════════════
        @Transactional
        public OptimizationResult runOptimizationInternal(
                OptimizationRequest request, String username, String jobId, long startTime) {

                log.info("Bắt đầu chạy phân tuyến — Depot: {}, Ngày: {}", request.getDepotId(), request.getRouteDate());

                // ===== Lấy cấu hình =====
                AlgorithmConfig config = algorithmConfigRepository.findById(request.getAlgorithmConfigId())
                                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy AlgorithmConfig id: "
                                                + request.getAlgorithmConfigId()));

                Depot depot = depotRepository.findById(request.getDepotId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Không tìm thấy Depot id: " + request.getDepotId()));

                User currentUser = userRepository.findByEmail(username)
                                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user: " + username));

                updateJobStatus(jobId, "RUNNING", "Đang thu thập dữ liệu đơn hàng & xe...", 10, startTime);

                // ==== Thu thập dữ liệu =====
                List<Order> pendingOrders = orderRepository.findAllByStatus(OrderStatus.PENDING);
                List<Vehicle> availableVehicles = vehicleRepository.findAllByStatus(VehicleStatus.AVAILABLE);

                if (pendingOrders.isEmpty()) {
                        log.warn("Không có đơn hàng PENDING nào để phân tuyến");
                        return saveFailedResult(config, currentUser, "Không có đơn hàng PENDING", startTime);
                }

                if (availableVehicles.isEmpty()) {
                        log.warn("Không có xe AVAILABLE nào");
                        return saveFailedResult(config, currentUser, "Không có xe khả dụng", startTime);
                }

                log.info("{} đơn PENDING | {} xe AVAILABLE", pendingOrders.size(), availableVehicles.size());

                // ===== Chuẩn bị Ma trận khoảng cách =====
                updateJobStatus(jobId, "RUNNING", "Đang tính ma trận khoảng cách (OSRM)...", 20, startTime);

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
                updateJobStatus(jobId, "RUNNING", "Đang chạy thuật toán tối ưu hoá...", 35, startTime);

                LocalTime depotStartTime = depot.getStartTime() != null
                                ? depot.getStartTime() : LocalTime.of(7, 0);
                LocalTime depotEndTime = depot.getEndTime(); // null = không giới hạn (Issue #15)

                VrpSolver solver = selectSolver(config);
                VrpSolutionDto solution = solver.solve(pendingOrders, availableVehicles, depotLocation, distanceMap,
                                config, request.getRouteDate(), depotStartTime, depotEndTime);

                // ===== Tính Capacity Utilization Metrics (Issue #11) =====
                updateJobStatus(jobId, "RUNNING", "Đang tính toán metrics...", 80, startTime);
                computeUtilizationMetrics(solution);

                // ===== Lưu kết quả xuống DB =====
                updateJobStatus(jobId, "RUNNING", "Đang lưu kết quả phân tuyến...", 85, startTime);

                double totalDist = solution.getTotalDistanceMeters();
                double totalCost = solution.getTotalCostVnd();
                OptimizationResult result = OptimizationResult.builder()
                                .config(config)
                                .user(currentUser)
                                .totalOrders(pendingOrders.size() - solution.getUnassignedOrderCount())
                                .totalVehicles(solution.getRoutes().size())
                                .totalDistance(BigDecimal.valueOf(totalDist / 1000)) // km
                                .totalCost(BigDecimal.valueOf(totalCost))            // VND
                                .executionTimeMs(System.currentTimeMillis() - startTime)
                                .status("SUCCESS")
                                .avgWeightUtilization(BigDecimal.valueOf(solution.getAvgWeightUtilizationPercent()))
                                .avgVolumeUtilization(BigDecimal.valueOf(solution.getAvgVolumeUtilizationPercent()))
                                .unassignedOrders(solution.getUnassignedOrderCount())
                                .build();
                // Phải persist OptimizationResult trước để có ID,
                // nếu không Route.result sẽ tham chiếu tới entity chưa lưu → lỗi TransientPropertyValueException
                result = optimizationResultRepository.save(result);

                // lưu route và routeStop cho từng xe
                for (PlannedRouteDto planned : solution.getRoutes()) {
                        // Tìm tài xế đang lái xe này
                        Driver assignedDriver = driverRepository.findByVehicle(planned.getVehicle())
                                        .orElse(null);
                        if (assignedDriver == null) {
                                log.warn("Xe {} chưa có tài xế được gán — route sẽ không có driver",
                                                planned.getVehicle().getLicensePlate());
                        }

                        Route route = Route.builder()
                                        .result(result)
                                        .startDepot(depot)
                                        .vehicle(planned.getVehicle())
                                        .driver(assignedDriver)
                                        .routeDate(request.getRouteDate())
                                        .totalDistanceMeters(BigDecimal.valueOf(planned.getTotalDistanceMeters()))
                                        .totalDurationSeconds(planned.getTotalDurationSeconds())
                                        .totalWeightKg(BigDecimal.valueOf(planned.getTotalWeightKg()))
                                        .build();
                        routeRepository.save(route);

                        // lưu routeStop theo stop_order
                        List<RouteStop> stops = new ArrayList<>();
                        int stopOrder = 1;
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
                                                                ? order.getServiceTimeMinutes()
                                                                : 15);
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

                log.info("Phân tuyến hoàn tất! {} tuyến, {} đơn đã phân, {} đơn chưa phân. Tổng: {}km — {}đ — {}ms",
                                solution.getRoutes().size(),
                                assignedOrders.size(),
                                solution.getUnassignedOrderCount(),
                                String.format("%.2f", totalDist / 1000),
                                String.format("%,.0f", totalCost),
                                System.currentTimeMillis() - startTime);

                // Issue #12: So sánh với lần chạy trước
                logSolutionComparison(result);

                return result;
        }

        // ═══════════════════════════════════════════════════════════════════
        // LEGACY SYNCHRONOUS — giữ cho backward compatibility
        // ═══════════════════════════════════════════════════════════════════
        @Override
        @Transactional
        public OptimizationResult runOptimization(OptimizationRequest request) {
                String username = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication())
                                .getName();
                return runOptimizationInternal(request, username, "sync-" + UUID.randomUUID(), System.currentTimeMillis());
        }

        // ===== PRIVATE HELPER =====

        /**
         * Chọn solver dựa trên AlgorithmConfig.name
         *
         * Tại sao dùng config.getName() thay vì hardcode?
         * → Cho phép user chọn thuật toán từ UI mà không cần thay đổi code
         * → Dễ mở rộng: thêm solver mới chỉ cần tạo @Component với đúng tên
         *
         * Mapping:
         *   - "NEAREST_NEIGHBOR" hoặc mặc định → nearestNeighborSolver
         *   - "GENETIC_ALGORITHM"               → geneticAlgorithmSolver
         */
        private VrpSolver selectSolver(AlgorithmConfig config) {
                String algorithmName = config.getName() != null
                        ? config.getName().toUpperCase().trim() : "";

                String beanName;
                switch (algorithmName) {
                        case "GENETIC_ALGORITHM":
                                beanName = "geneticAlgorithmSolver";
                                break;
                        case "NEAREST_NEIGHBOR":
                        default:
                                beanName = "nearestNeighborSolver";
                                break;
                }

                VrpSolver solver = solverMap.get(beanName);
                if (solver == null) {
                        // Fallback: thử tìm bất kỳ solver nào có sẵn
                        log.warn("Không tìm thấy solver [{}], thử fallback...", beanName);
                        solver = solverMap.values().stream().findFirst()
                                .orElseThrow(() -> new ResourceNotFoundException(
                                        "Không có solver nào trong hệ thống!"));
                }

                log.info("Sử dụng solver: {} (config.name='{}')", beanName, config.getName());
                return solver;
        }

        private OptimizationResult saveFailedResult(
                        AlgorithmConfig config,
                        User user,
                        String errorMessage,
                        long startTime) {
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

        @Override
        @Transactional
        public void resetTestingData() {
                // Delete all routes and related data
                routeRepository.deleteAll();
                optimizationResultRepository.deleteAll();
                
                // Reset orders
                List<Order> orders = orderRepository.findAll();
                orders.forEach(o -> o.setStatus(OrderStatus.PENDING));
                orderRepository.saveAll(orders);
                
                // Reset vehicles
                List<Vehicle> vehicles = vehicleRepository.findAll();
                vehicles.forEach(v -> v.setStatus(VehicleStatus.AVAILABLE));
                vehicleRepository.saveAll(vehicles);
                
                log.info("Đã reset dữ liệu test: Xóa toàn bộ Route/OptimizationResult, đặt lại Order về PENDING, Vehicle về AVAILABLE.");
        }

        // ═══════════════════════════════════════════════════════════════════
        // ISSUE #11: CAPACITY UTILIZATION METRICS
        // ═══════════════════════════════════════════════════════════════════
        /**
         * Tính capacity utilization cho mỗi tuyến và aggregate.
         * Giúp đánh giá hiệu quả sử dụng xe:
         *   - < 40%: xe chạy non tải, nên cân nhắc dùng xe nhỏ hơn
         *   - 40-70%: bình thường
         *   - > 70%: tốt, xe được tận dụng hiệu quả
         */
        private void computeUtilizationMetrics(VrpSolutionDto solution) {
                if (solution.getRoutes() == null || solution.getRoutes().isEmpty()) return;

                double sumWeightUtil = 0;
                double sumVolumeUtil = 0;
                double minWeightUtil = 100;
                double maxWeightUtil = 0;

                for (var route : solution.getRoutes()) {
                        double maxWeight = route.getVehicle().getVehicleType().getMaxWeightKg().doubleValue();
                        double maxVolume = route.getVehicle().getVehicleType().getMaxVolumeM3() != null
                                ? route.getVehicle().getVehicleType().getMaxVolumeM3().doubleValue() : 0;

                        double weightUtil = maxWeight > 0 ? (route.getTotalWeightKg() / maxWeight) * 100 : 0;
                        double volumeUtil = maxVolume > 0
                                ? (route.getOrderStops().stream()
                                        .mapToDouble(o -> o.getTotalVolumeM3() != null ? o.getTotalVolumeM3().doubleValue() : 0)
                                        .sum() / maxVolume) * 100
                                : 0;

                        sumWeightUtil += weightUtil;
                        sumVolumeUtil += volumeUtil;
                        minWeightUtil = Math.min(minWeightUtil, weightUtil);
                        maxWeightUtil = Math.max(maxWeightUtil, weightUtil);

                        log.debug("Xe [{}]: Weight {:.1f}%, Volume {:.1f}%"
                                .replace("{:.1f}", "%.1f"),
                                route.getVehicle().getLicensePlate(), weightUtil, volumeUtil);
                }

                int routeCount = solution.getRoutes().size();
                solution.setAvgWeightUtilizationPercent(sumWeightUtil / routeCount);
                solution.setAvgVolumeUtilizationPercent(sumVolumeUtil / routeCount);
                solution.setMinWeightUtilizationPercent(minWeightUtil);
                solution.setMaxWeightUtilizationPercent(maxWeightUtil);

                log.info("📊 Capacity Utilization: Avg Weight {:.1f}%, Avg Volume {:.1f}%, Min {:.1f}%, Max {:.1f}%"
                        .replace("{:.1f}", "%.1f"),
                        solution.getAvgWeightUtilizationPercent(),
                        solution.getAvgVolumeUtilizationPercent(),
                        solution.getMinWeightUtilizationPercent(),
                        solution.getMaxWeightUtilizationPercent());
        }

        // ═══════════════════════════════════════════════════════════════════
        // ISSUE #12: SOLUTION COMPARISON LOGGING
        // ═══════════════════════════════════════════════════════════════════
        /**
         * So sánh kết quả hiện tại với lần chạy thành công gần nhất.
         * Log ra delta để đánh giá cải tiến thuật toán.
         */
        private void logSolutionComparison(OptimizationResult current) {
                try {
                        List<OptimizationResult> history = optimizationResultRepository
                                .findAll().stream()
                                .filter(r -> "SUCCESS".equals(r.getStatus()) && !r.getId().equals(current.getId()))
                                .sorted((a, b) -> b.getRunDate().compareTo(a.getRunDate()))
                                .limit(1)
                                .toList();

                        if (history.isEmpty()) {
                                log.info("📈 Đây là lần chạy thành công đầu tiên — không có dữ liệu so sánh.");
                                return;
                        }

                        OptimizationResult prev = history.get(0);
                        double costDelta = current.getTotalCost().doubleValue() - prev.getTotalCost().doubleValue();
                        double distDelta = current.getTotalDistance().doubleValue() - prev.getTotalDistance().doubleValue();
                        int vehicleDelta = current.getTotalVehicles() - prev.getTotalVehicles();

                        String costSign = costDelta <= 0 ? "⬇️" : "⬆️";
                        String distSign = distDelta <= 0 ? "⬇️" : "⬆️";

                        log.info("📈 So sánh vs lần trước (ID={}): Cost {} {:.0f}đ | Dist {} {:.2f}km | Xe {}{}",
                                prev.getId(),
                                costSign, Math.abs(costDelta),
                                distSign, Math.abs(distDelta),
                                vehicleDelta >= 0 ? "+" : "", vehicleDelta);
                } catch (Exception e) {
                        log.debug("Không thể so sánh với lần chạy trước: {}", e.getMessage());
                }
        }
}
