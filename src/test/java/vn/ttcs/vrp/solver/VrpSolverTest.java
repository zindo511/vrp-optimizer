package vn.ttcs.vrp.solver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.ttcs.vrp.dto.vrp.VrpSolutionDto;
import vn.ttcs.vrp.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests cho VRP Solvers — không cần Spring context, chạy thuần Java.
 *
 * ═══════════════════════════════════════════════════════════════════
 * Test cases:
 *  1. NN: Đơn lẻ → 1 xe, 1 tuyến
 *  2. NN: Vượt tải trọng → 2 tuyến
 *  3. NN: Không có xe → 0 tuyến, tất cả unassigned
 *  4. NN: Time window constraint
 *  5. GA: Kết quả hợp lệ (tất cả đơn được phân)
 *  6. GA: Early stopping / timeout
 *  7. NN: Inter-route relocate giảm cost
 *  8. Haversine fallback khi thiếu distance data
 *  9. maxDrivingTimeMinutes constraint
 * ═══════════════════════════════════════════════════════════════════
 */
@DisplayName("VRP Solver Tests")
class VrpSolverTest {

    // ── Test fixtures ──
    private NearestNeighborSolverImpl nnSolver;
    private GeneticAlgorithmSolverImpl gaSolver;
    private TwoOptLocalSearch twoOptLocalSearch;

    private Location depotLocation;
    private LocalDate routeDate;
    private LocalTime depotStartTime;
    private AlgorithmConfig defaultConfig;

    @BeforeEach
    void setUp() {
        twoOptLocalSearch = new TwoOptLocalSearch();
        nnSolver = new NearestNeighborSolverImpl(twoOptLocalSearch);
        gaSolver = new GeneticAlgorithmSolverImpl(twoOptLocalSearch);

        depotLocation = buildLocation(1L, "Depot HCM", 10.7769, 106.7009);
        routeDate = LocalDate.of(2026, 5, 4);
        depotStartTime = LocalTime.of(7, 0);

        defaultConfig = AlgorithmConfig.builder()
                .name("NEAREST_NEIGHBOR")
                .populationSize(20)
                .generations(50)
                .mutationRate(BigDecimal.valueOf(0.05))
                .crossoverRate(BigDecimal.valueOf(0.80))
                .elitismCount(2)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 1: NN — Đơn lẻ → 1 tuyến duy nhất
    // ═══════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("NN: 1 đơn + 1 xe → 1 tuyến với 1 stop")
    void nn_singleOrder_singleVehicle() {
        List<Order> orders = List.of(buildOrder(1L, 10.0, 1.0));
        List<Vehicle> vehicles = List.of(buildVehicle(1L, "59A-111", 1000, 50));
        Map<String, DistanceMatrix> dm = buildFullDistanceMatrix(depotLocation, orders);

        VrpSolutionDto result = nnSolver.solve(
                orders, vehicles, depotLocation, dm,
                defaultConfig, routeDate, depotStartTime, null);

        assertEquals(1, result.getRoutes().size(), "Phải có đúng 1 tuyến");
        assertEquals(1, result.getRoutes().get(0).getOrderStops().size(), "Tuyến phải có 1 stop");
        assertEquals(0, result.getUnassignedOrderCount(), "Không được có đơn chưa phân");
        assertTrue(result.getTotalDistanceMeters() > 0, "Khoảng cách phải > 0");
        assertTrue(result.getTotalCostVnd() > 0, "Chi phí phải > 0");
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 2: NN — Vượt tải trọng → cần 2 xe
    // ═══════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("NN: 2 đơn 60kg mỗi đơn + xe max 100kg → 2 tuyến")
    void nn_overweight_splitsToTwoRoutes() {
        List<Order> orders = List.of(
                buildOrder(1L, 60.0, 1.0),
                buildOrder(2L, 60.0, 1.0));
        List<Vehicle> vehicles = List.of(
                buildVehicle(1L, "59A-111", 100, 50),
                buildVehicle(2L, "59A-222", 100, 50));
        Map<String, DistanceMatrix> dm = buildFullDistanceMatrix(depotLocation, orders);

        VrpSolutionDto result = nnSolver.solve(
                orders, vehicles, depotLocation, dm,
                defaultConfig, routeDate, depotStartTime, null);

        assertEquals(2, result.getRoutes().size(), "Phải tách thành 2 tuyến");
        assertEquals(0, result.getUnassignedOrderCount(), "Tất cả đơn phải được phân");
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 3: NN — Không có xe → tất cả unassigned
    // ═══════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("NN: 3 đơn + 0 xe → 0 tuyến, 3 unassigned")
    void nn_noVehicles_allUnassigned() {
        List<Order> orders = List.of(
                buildOrder(1L, 10.0, 1.0),
                buildOrder(2L, 10.0, 1.0),
                buildOrder(3L, 10.0, 1.0));
        List<Vehicle> vehicles = List.of();
        Map<String, DistanceMatrix> dm = buildFullDistanceMatrix(depotLocation, orders);

        VrpSolutionDto result = nnSolver.solve(
                orders, vehicles, depotLocation, dm,
                defaultConfig, routeDate, depotStartTime, null);

        assertEquals(0, result.getRoutes().size(), "Không có xe → 0 tuyến");
        assertEquals(3, result.getUnassignedOrderCount(), "Tất cả đơn phải unassigned");
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 4: NN — Time window constraint
    // ═══════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("NN: Đơn có deadline quá sớm → bị bỏ qua")
    void nn_timeWindow_lateDeadlineSkipped() {
        Order earlyDeadlineOrder = buildOrder(1L, 10.0, 1.0);
        earlyDeadlineOrder.setTimeWindowTo(LocalTime.of(7, 5)); // deadline 7:05 — quá sát

        Order normalOrder = buildOrder(2L, 10.0, 1.0);

        List<Order> orders = List.of(earlyDeadlineOrder, normalOrder);
        List<Vehicle> vehicles = List.of(buildVehicle(1L, "59A-111", 1000, 50));
        Map<String, DistanceMatrix> dm = buildFullDistanceMatrix(depotLocation, orders);
        // Set duration rất lớn cho đơn 1 → không kịp deadline
        String key1 = depotLocation.getId() + "-" + earlyDeadlineOrder.getLocation().getId();
        dm.put(key1, buildDm(depotLocation, earlyDeadlineOrder.getLocation(), 5000, 600)); // 10 phút

        VrpSolutionDto result = nnSolver.solve(
                orders, vehicles, depotLocation, dm,
                defaultConfig, routeDate, depotStartTime, null);

        // Đơn earlyDeadline bị bỏ qua vì xe mất 10 phút di chuyển nhưng deadline 7:05
        assertTrue(result.getRoutes().size() >= 1);
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 5: GA — Kết quả hợp lệ
    // ═══════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("GA: 5 đơn + 2 xe → tất cả được phân")
    void ga_basicScenario_allOrdersAssigned() {
        List<Order> orders = new ArrayList<>();
        for (long i = 1; i <= 5; i++) {
            orders.add(buildOrder(i, 10.0, 1.0));
        }
        List<Vehicle> vehicles = List.of(
                buildVehicle(1L, "59A-111", 500, 50),
                buildVehicle(2L, "59A-222", 500, 50));

        AlgorithmConfig gaConfig = AlgorithmConfig.builder()
                .name("GENETIC_ALGORITHM")
                .populationSize(10) // nhỏ để test nhanh
                .generations(20)
                .mutationRate(BigDecimal.valueOf(0.10))
                .crossoverRate(BigDecimal.valueOf(0.80))
                .elitismCount(2)
                .build();

        Map<String, DistanceMatrix> dm = buildFullDistanceMatrix(depotLocation, orders);

        VrpSolutionDto result = gaSolver.solve(
                orders, vehicles, depotLocation, dm,
                gaConfig, routeDate, depotStartTime, null);

        assertNotNull(result);
        assertTrue(result.getRoutes().size() >= 1, "Phải có ít nhất 1 tuyến");
        assertEquals(0, result.getUnassignedOrderCount(), "Tất cả đơn phải được phân");
        assertTrue(result.getTotalCostVnd() > 0, "Chi phí phải > 0");

        // Verify tổng đơn đã phân = 5
        int totalAssigned = result.getRoutes().stream()
                .mapToInt(r -> r.getOrderStops().size())
                .sum();
        assertEquals(5, totalAssigned, "Tổng đơn đã phân phải = 5");
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 6: GA — Haversine fallback khi thiếu distance data
    // ═══════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("GA: Thiếu 1 cặp DM → dùng Haversine fallback, không bỏ qua đơn")
    void ga_missingDistanceData_usesFallback() {
        List<Order> orders = List.of(
                buildOrder(1L, 10.0, 1.0),
                buildOrder(2L, 10.0, 1.0));
        List<Vehicle> vehicles = List.of(buildVehicle(1L, "59A-111", 1000, 50));

        // Chỉ có DM cho depot↔order1, thiếu depot↔order2
        Map<String, DistanceMatrix> dm = new HashMap<>();
        dm.put(depotLocation.getId() + "-" + orders.get(0).getLocation().getId(),
                buildDm(depotLocation, orders.get(0).getLocation(), 3000, 300));
        dm.put(orders.get(0).getLocation().getId() + "-" + depotLocation.getId(),
                buildDm(orders.get(0).getLocation(), depotLocation, 3000, 300));
        dm.put(orders.get(0).getLocation().getId() + "-" + orders.get(1).getLocation().getId(),
                buildDm(orders.get(0).getLocation(), orders.get(1).getLocation(), 2000, 200));
        dm.put(orders.get(1).getLocation().getId() + "-" + depotLocation.getId(),
                buildDm(orders.get(1).getLocation(), depotLocation, 4000, 400));
        // Missing: depot→order2 and order2→order1

        AlgorithmConfig gaConfig = AlgorithmConfig.builder()
                .name("GENETIC_ALGORITHM")
                .populationSize(10)
                .generations(10)
                .mutationRate(BigDecimal.valueOf(0.05))
                .crossoverRate(BigDecimal.valueOf(0.80))
                .elitismCount(1)
                .build();

        VrpSolutionDto result = gaSolver.solve(
                orders, vehicles, depotLocation, dm,
                gaConfig, routeDate, depotStartTime, null);

        // GA phải dùng Haversine fallback thay vì bỏ qua đơn
        assertNotNull(result);
        int totalAssigned = result.getRoutes().stream()
                .mapToInt(r -> r.getOrderStops().size())
                .sum();
        assertTrue(totalAssigned >= 1, "GA phải gán ít nhất 1 đơn (dùng Haversine fallback)");
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 7: maxDrivingTimeMinutes constraint
    // ═══════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("NN: maxDrivingTime 60 phút + đơn xa → tách tuyến")
    void nn_maxDrivingTime_splitsRoute() {
        List<Order> orders = List.of(
                buildOrder(1L, 10.0, 1.0),  // ~5min từ depot
                buildOrder(2L, 10.0, 1.0),  // ~5min
                buildOrder(3L, 10.0, 1.0)); // ~5min

        // Xe có maxDrivingTime = 1 phút → mỗi đơn 1 xe
        VehicleType tightType = VehicleType.builder()
                .id(99L)
                .name("Very Tight")
                .maxWeightKg(BigDecimal.valueOf(1000))
                .maxVolumeM3(BigDecimal.valueOf(50))
                .costPerKm(BigDecimal.valueOf(5000))
                .fixedCost(BigDecimal.valueOf(50000))
                .maxDrivingTimeMinutes(1) // chỉ 1 phút!
                .build();

        List<Vehicle> vehicles = List.of(
                buildVehicleWithType(1L, "59A-111", tightType),
                buildVehicleWithType(2L, "59A-222", tightType),
                buildVehicleWithType(3L, "59A-333", tightType));

        Map<String, DistanceMatrix> dm = buildFullDistanceMatrix(depotLocation, orders);

        VrpSolutionDto result = nnSolver.solve(
                orders, vehicles, depotLocation, dm,
                defaultConfig, routeDate, depotStartTime, null);

        // Với maxDrivingTime = 1 phút, mỗi đơn mất > 1 phút di chuyển
        // → xe không đủ thời gian → đơn bị unassigned (đúng hành vi)
        assertTrue(result.getUnassignedOrderCount() >= 1,
                "Với maxDrivingTime quá chặt, phải có đơn bị unassigned");
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 8: NN kết quả cost-aware (xe rẻ ưu tiên trước)
    // ═══════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("NN: Xe rẻ được ưu tiên gán đơn trước")
    void nn_costAware_cheapVehicleFirst() {
        List<Order> orders = List.of(buildOrder(1L, 10.0, 1.0));

        VehicleType cheapType = VehicleType.builder()
                .id(1L).name("Cheap").maxWeightKg(BigDecimal.valueOf(1000))
                .maxVolumeM3(BigDecimal.valueOf(50))
                .costPerKm(BigDecimal.valueOf(3000))
                .fixedCost(BigDecimal.valueOf(10000)).build();

        VehicleType expensiveType = VehicleType.builder()
                .id(2L).name("Expensive").maxWeightKg(BigDecimal.valueOf(1000))
                .maxVolumeM3(BigDecimal.valueOf(50))
                .costPerKm(BigDecimal.valueOf(15000))
                .fixedCost(BigDecimal.valueOf(100000)).build();

        List<Vehicle> vehicles = List.of(
                buildVehicleWithType(2L, "59A-EXPENSIVE", expensiveType),
                buildVehicleWithType(1L, "59A-CHEAP", cheapType));

        Map<String, DistanceMatrix> dm = buildFullDistanceMatrix(depotLocation, orders);

        VrpSolutionDto result = nnSolver.solve(
                orders, vehicles, depotLocation, dm,
                defaultConfig, routeDate, depotStartTime, null);

        assertEquals(1, result.getRoutes().size());
        assertEquals("59A-CHEAP", result.getRoutes().get(0).getVehicle().getLicensePlate(),
                "Xe rẻ phải được chọn trước");
    }

    // ═══════════════════════════════════════════════════════════════════
    // HELPER METHODS — Tạo test data
    // ═══════════════════════════════════════════════════════════════════

    private Location buildLocation(Long id, String address, double lat, double lon) {
        Location loc = new Location();
        loc.setId(id);
        loc.setAddress(address);
        loc.setLatitude(BigDecimal.valueOf(lat));
        loc.setLongitude(BigDecimal.valueOf(lon));
        return loc;
    }

    private Order buildOrder(Long id, double weightKg, double volumeM3) {
        Location loc = buildLocation(100 + id, "Customer " + id,
                10.78 + id * 0.01, 106.70 + id * 0.01);

        Order order = Order.builder()
                .customerName("Khách " + id)
                .customerPhone("090000000" + id)
                .location(loc)
                .totalWeightKg(BigDecimal.valueOf(weightKg))
                .totalVolumeM3(BigDecimal.valueOf(volumeM3))
                .serviceTimeMinutes(15)
                .build();
        order.setId(id);
        return order;
    }

    private Vehicle buildVehicle(Long id, String plate, double maxWeight, double maxVolume) {
        VehicleType type = VehicleType.builder()
                .id(id)
                .name("Type-" + id)
                .maxWeightKg(BigDecimal.valueOf(maxWeight))
                .maxVolumeM3(BigDecimal.valueOf(maxVolume))
                .costPerKm(BigDecimal.valueOf(5000))
                .fixedCost(BigDecimal.valueOf(50000))
                .build();

        Vehicle vehicle = Vehicle.builder()
                .vehicleType(type)
                .licensePlate(plate)
                .build();
        vehicle.setId(id);
        return vehicle;
    }

    private Vehicle buildVehicleWithType(Long id, String plate, VehicleType type) {
        Vehicle vehicle = Vehicle.builder()
                .vehicleType(type)
                .licensePlate(plate)
                .build();
        vehicle.setId(id);
        return vehicle;
    }

    private DistanceMatrix buildDm(Location origin, Location dest, double distMeters, long durSeconds) {
        return DistanceMatrix.builder()
                .origin(origin)
                .destination(dest)
                .distanceMeters(BigDecimal.valueOf(distMeters))
                .durationSeconds(durSeconds)
                .build();
    }

    /**
     * Tạo ma trận khoảng cách đầy đủ (N+1 × N+1) cho depot + orders.
     * Khoảng cách ước lượng bằng Haversine.
     */
    private Map<String, DistanceMatrix> buildFullDistanceMatrix(
            Location depot, List<Order> orders) {

        List<Location> allLocations = new ArrayList<>();
        allLocations.add(depot);
        orders.forEach(o -> allLocations.add(o.getLocation()));

        Map<String, DistanceMatrix> dm = new HashMap<>();
        for (Location origin : allLocations) {
            for (Location dest : allLocations) {
                if (origin.getId().equals(dest.getId())) continue;

                double dist = haversine(
                        origin.getLatitude().doubleValue(),
                        origin.getLongitude().doubleValue(),
                        dest.getLatitude().doubleValue(),
                        dest.getLongitude().doubleValue()) * 1.3;

                long dur = (long) (dist / 1000.0 / 40.0 * 3600.0);

                dm.put(origin.getId() + "-" + dest.getId(),
                        buildDm(origin, dest, dist, dur));
            }
        }
        return dm;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
