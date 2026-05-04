package vn.ttcs.vrp.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ttcs.vrp.dto.response.DriverPerformanceResponse;
import vn.ttcs.vrp.dto.response.OptimizationHistoryResponse;
import vn.ttcs.vrp.dto.response.ReportSummaryResponse;
import vn.ttcs.vrp.enums.DriverStatus;
import vn.ttcs.vrp.enums.OrderStatus;
import vn.ttcs.vrp.enums.RouteStopStatus;
import vn.ttcs.vrp.enums.VehicleStatus;
import vn.ttcs.vrp.model.Driver;
import vn.ttcs.vrp.model.OptimizationResult;
import vn.ttcs.vrp.repository.*;
import vn.ttcs.vrp.service.ReportService;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "REPORT-SERVICE")
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final OrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final OptimizationResultRepository optimizationResultRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;

    // ═════════════════════════════════════════════════════════════════════════
    // GET /api/reports/summary
    // ═════════════════════════════════════════════════════════════════════════
    @Override
    public ReportSummaryResponse getSummary() {
        log.info("Tạo báo cáo tổng hợp...");

        // ── Đơn hàng theo status ─────────────────────────────────────────────
        Map<String, Long> ordersByStatus = new LinkedHashMap<>();
        long totalOrders = 0;
        for (OrderStatus status : OrderStatus.values()) {
            long count = orderRepository.countByStatus(status);
            ordersByStatus.put(status.name(), count);
            totalOrders += count;
        }

        // ── Tổng km từ bảng routes (chính xác hơn optimization_results) ────
        BigDecimal totalDistanceKm = routeRepository.findAll().stream()
                .map(r -> r.getTotalDistanceMeters())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(1000), 2, java.math.RoundingMode.HALF_UP);

        // ── Tổng chi phí từ optimization_results ─────────────────────────
        List<OptimizationResult> allResults = optimizationResultRepository.findAll();

        BigDecimal totalCostVnd = allResults.stream()
                .map(OptimizationResult::getTotalCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ── Tài xế ──────────────────────────────────────────────────────────
        long totalDrivers = driverRepository.count();
        long activeDrivers = driverRepository.countByStatus(DriverStatus.ACTIVE);

        // ── Xe ───────────────────────────────────────────────────────────────
        long totalVehicles = vehicleRepository.count();
        long availableVehicles = vehicleRepository.countByStatus(VehicleStatus.AVAILABLE);
        long maintenanceVehicles = vehicleRepository.countByStatus(VehicleStatus.MAINTENANCE);

        // ── Optimization history stats ───────────────────────────────────────
        long totalRuns = optimizationResultRepository.count();
        long successRuns = optimizationResultRepository.countByStatus("SUCCESS");

        return ReportSummaryResponse.builder()
                .totalOrders(totalOrders)
                .ordersByStatus(ordersByStatus)
                .totalDistanceKm(totalDistanceKm)
                .totalCostVnd(totalCostVnd)
                .totalDrivers(totalDrivers)
                .activeDrivers(activeDrivers)
                .totalVehicles(totalVehicles)
                .availableVehicles(availableVehicles)
                .maintenanceVehicles(maintenanceVehicles)
                .totalOptimizationRuns(totalRuns)
                .successfulRuns(successRuns)
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GET /api/optimization/history
    // ═════════════════════════════════════════════════════════════════════════
    @Override
    public List<OptimizationHistoryResponse> getOptimizationHistory() {
        log.info("Truy vấn lịch sử optimization...");

        List<OptimizationResult> results = optimizationResultRepository.findAllOrderByRunDateDesc();

        return results.stream().map(r -> OptimizationHistoryResponse.builder()
                .id(r.getId())
                .runDate(r.getRunDate())
                .algorithmName(r.getConfig() != null ? r.getConfig().getName() : "N/A")
                .runByEmail(r.getUser() != null ? r.getUser().getEmail() : "N/A")
                .totalOrders(r.getTotalOrders())
                .totalVehicles(r.getTotalVehicles())
                .totalDistanceKm(r.getTotalDistance())
                .totalCost(r.getTotalCost())
                .executionTimeMs(r.getExecutionTimeMs())
                .status(r.getStatus())
                .errorMessage(r.getErrorMessage())
                .build()
        ).collect(Collectors.toList());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GET /api/reports/drivers
    // ═════════════════════════════════════════════════════════════════════════
    // Trước: 4 query × N drivers = 4N queries (N+1 problem)
    // Sau:   2 aggregate queries + 1 findAll = 3 queries tổng cộng
    // ═════════════════════════════════════════════════════════════════════════
    @Override
    public List<DriverPerformanceResponse> getDriverPerformance() {
        log.info("Tạo báo cáo hiệu suất tài xế...");

        // ── Query 1: tất cả drivers ──────────────────────────────────────────
        List<Driver> allDrivers = driverRepository.findAll();

        // ── Query 2: stop counts per driver per status (1 query cho TẤT CẢ) ─
        // Result: [driverId, RouteStopStatus, count]
        List<Object[]> stopStats = routeStopRepository.countStopsGroupedByDriverAndStatus();

        // Build lookup: driverId → { status → count }
        Map<Long, Map<RouteStopStatus, Long>> stopMap = new HashMap<>();
        for (Object[] row : stopStats) {
            Long driverId = (Long) row[0];
            RouteStopStatus status = (RouteStopStatus) row[1];
            Long count = (Long) row[2];
            stopMap.computeIfAbsent(driverId, k -> new EnumMap<>(RouteStopStatus.class))
                   .put(status, count);
        }

        // ── Query 3: route counts per driver (1 query cho TẤT CẢ) ───────────
        // Result: [driverId, routeCount]
        List<Object[]> routeStats = routeStopRepository.countRoutesGroupedByDriver();
        Map<Long, Long> routeCountMap = new HashMap<>();
        for (Object[] row : routeStats) {
            routeCountMap.put((Long) row[0], (Long) row[1]);
        }

        // ── Build response (in-memory, no more DB queries) ──────────────────
        return allDrivers.stream().map(driver -> {
            Long id = driver.getId();
            Map<RouteStopStatus, Long> driverStops = stopMap.getOrDefault(id, Collections.emptyMap());

            long completedStops = driverStops.getOrDefault(RouteStopStatus.COMPLETED, 0L);
            long failedStops    = driverStops.getOrDefault(RouteStopStatus.FAILED, 0L);
            long totalStops     = driverStops.values().stream().mapToLong(Long::longValue).sum();
            long totalRoutes    = routeCountMap.getOrDefault(id, 0L);

            double successRate = totalStops > 0
                    ? (double) completedStops / totalStops * 100.0
                    : 0.0;

            String driverName = driver.getUser() != null
                    ? driver.getUser().getEmail() : "N/A";

            return DriverPerformanceResponse.builder()
                    .driverId(id)
                    .driverName(driverName)
                    .phone(driver.getPhone())
                    .status(driver.getStatus().name())
                    .totalRoutes(totalRoutes)
                    .completedStops(completedStops)
                    .failedStops(failedStops)
                    .totalStops(totalStops)
                    .successRate(Math.round(successRate * 100.0) / 100.0)
                    .build();
        }).collect(Collectors.toList());
    }
}
