package vn.ttcs.vrp.solver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.ttcs.vrp.dto.vrp.PlannedRouteDto;
import vn.ttcs.vrp.dto.vrp.VrpSolutionDto;
import vn.ttcs.vrp.model.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Thuật toán láng giềng gần nhất (Nearest Neighbor) + 2-opt + Relocate + Cost-aware
 *
 * ═══════════════════════════════════════════════════════════════════
 * PIPELINE:
 *   1. Sắp xếp xe theo costPerKm tăng dần (cost-aware)
 *   2. Xây tuyến greedy bằng NN
 *   3. Chạy 2-opt cải thiện thứ tự stop (intra-route local search)
 *   4. Chạy Relocate cải thiện phân bổ đơn giữa các xe (inter-route)
 *   5. Tính chi phí thực tế: fixedCost + costPerKm × km
 * ═══════════════════════════════════════════════════════════════════
 *
 * Ưu điểm: Đơn giản, nhanh, dễ hiểu, kết quả tốt
 * Ràng buộc: tải trọng, dung tích, khung giờ, thời gian lái tối đa
 */
@Component("nearestNeighborSolver")
@RequiredArgsConstructor
@Slf4j(topic = "NEAREST-NEIGHBOR-SOLVER")
public class NearestNeighborSolverImpl implements VrpSolver {

    private final TwoOptLocalSearch twoOptLocalSearch;

    @Override
    public VrpSolutionDto solve(
            List<Order> orders,
            List<Vehicle> vehicles,
            Location depotLocation,
            Map<String, DistanceMatrix> distanceMap,
            AlgorithmConfig config,
            LocalDate routeDate,
            LocalTime depotStartTime,
            LocalTime depotEndTime) {

        log.info("Nearest Neighbor bắt đầu xử lý {} đơn với {} xe", orders.size(), vehicles.size());

        // ═════════════════════════════════════════════════════════════════════
        // COST-AWARE: Sắp xếp xe theo costPerKm tăng dần
        // ═════════════════════════════════════════════════════════════════════
        List<Vehicle> sortedVehicles = new ArrayList<>(vehicles);
        sortedVehicles.sort(Comparator.comparing(
                v -> v.getVehicleType().getCostPerKm()));

        log.info("Thứ tự xe (cost-aware): {}",
                sortedVehicles.stream()
                        .map(v -> v.getLicensePlate() + "(" + v.getVehicleType().getCostPerKm() + "đ/km)")
                        .toList());

        // ═════════════════════════════════════════════════════════════════════
        // ISSUE #6: Dùng boolean[] assigned thay vì ArrayList.remove(Object)
        // ═════════════════════════════════════════════════════════════════════
        // ArrayList.remove(Object) là O(n) vì phải duyệt + shift elements.
        // Với 500 đơn → O(n²) = 250,000 operations.
        // boolean[] assigned → O(1) per mark, tổng O(n) → nhanh hơn đáng kể.
        // ═════════════════════════════════════════════════════════════════════
        boolean[] assigned = new boolean[orders.size()];
        int remainingCount = orders.size();

        List<PlannedRouteDto> plannedRoutes = new ArrayList<>();

        for (Vehicle vehicle : sortedVehicles) {
            if (remainingCount == 0) break;

            Location currentLocation = depotLocation;
            double currentWeightKg   = 0;
            double currentVolumeM3   = 0;
            double routeDistanceMeters = 0;
            long   routeDurationSeconds = 0;
            List<Order> stopLists = new ArrayList<>();

            double maxWeightKg = vehicle.getVehicleType().getMaxWeightKg().doubleValue();
            double maxVolumeM3 = vehicle.getVehicleType().getMaxVolumeM3() != null
                    ? vehicle.getVehicleType().getMaxVolumeM3().doubleValue()
                    : Double.MAX_VALUE;

            // ── ISSUE #4: maxDrivingTimeMinutes ──────────────────────────────
            long maxDrivingSeconds = vehicle.getVehicleType().getMaxDrivingTimeMinutes() != null
                    ? vehicle.getVehicleType().getMaxDrivingTimeMinutes() * 60L
                    : 600L * 60; // 10 giờ mặc định

            LocalDateTime clock = routeDate.atTime(depotStartTime);

            while (remainingCount > 0) {
                // Tìm đơn gần nhất trong các đơn chưa assigned
                int nearestIdx = findNearestFeasibleOrderIdx(
                        currentLocation, orders, assigned, distanceMap,
                        currentWeightKg, maxWeightKg,
                        currentVolumeM3, maxVolumeM3,
                        routeDurationSeconds, maxDrivingSeconds,
                        clock, routeDate, depotEndTime, depotLocation);

                if (nearestIdx < 0) break;

                Order nearest = orders.get(nearestIdx);

                // Lấy thông tin di chuyển
                String key = currentLocation.getId() + "-" + nearest.getLocation().getId();
                DistanceMatrix dm = distanceMap.get(key);
                if (dm != null) {
                    routeDistanceMeters += dm.getDistanceMeters().doubleValue();
                    routeDurationSeconds += dm.getDurationSeconds();
                    clock = clock.plusSeconds(dm.getDurationSeconds());
                }

                // Xử lý chờ time window
                if (nearest.getTimeWindowFrom() != null) {
                    LocalDateTime windowOpen = routeDate.atTime(nearest.getTimeWindowFrom());
                    if (clock.isBefore(windowOpen)) {
                        long waitSeconds = Duration.between(clock, windowOpen).getSeconds();
                        routeDurationSeconds += waitSeconds;
                        clock = windowOpen;
                    }
                }

                // Cộng thời gian phục vụ
                int serviceMin = nearest.getServiceTimeMinutes() != null ? nearest.getServiceTimeMinutes() : 15;
                routeDurationSeconds += serviceMin * 60L;
                clock = clock.plusMinutes(serviceMin);

                // Cập nhật tích lũy
                stopLists.add(nearest);
                currentWeightKg += nearest.getTotalWeightKg().doubleValue();
                if (nearest.getTotalVolumeM3() != null) {
                    currentVolumeM3 += nearest.getTotalVolumeM3().doubleValue();
                }
                currentLocation = nearest.getLocation();

                // Mark assigned — O(1) thay vì O(n) remove
                assigned[nearestIdx] = true;
                remainingCount--;
            }

            if (!stopLists.isEmpty()) {
                // ── 2-OPT LOCAL SEARCH: Cải thiện thứ tự stop ────────────────
                List<Order> improvedStops = twoOptLocalSearch.improve(
                        stopLists, depotLocation, distanceMap,
                        routeDate, depotStartTime);
                stopLists = improvedStops;

                // Tính lại khoảng cách/thời gian sau 2-opt
                RouteMetrics metrics = recalculateRouteMetrics(
                        stopLists, depotLocation, distanceMap, routeDate, depotStartTime);

                // Tính chi phí
                double fixedCost = vehicle.getVehicleType().getFixedCost() != null
                        ? vehicle.getVehicleType().getFixedCost().doubleValue() : 0;
                double costPerKm = vehicle.getVehicleType().getCostPerKm().doubleValue();
                double routeCost = fixedCost + costPerKm * (metrics.distanceMeters / 1000.0);

                String volUsed = String.format("%.2f", currentVolumeM3);
                String volMax  = maxVolumeM3 == Double.MAX_VALUE ? "∞" : String.format("%.2f", maxVolumeM3);
                log.info("Xe [{}]: {} đơn — {}km — {}/{}kg — {}/{}m³ — chi phí: {}đ",
                        vehicle.getLicensePlate(), stopLists.size(),
                        String.format("%.2f", metrics.distanceMeters / 1000),
                        String.format("%.1f", currentWeightKg), String.format("%.1f", maxWeightKg),
                        volUsed, volMax,
                        String.format("%,.0f", routeCost));

                plannedRoutes.add(PlannedRouteDto.builder()
                        .vehicle(vehicle)
                        .orderStops(stopLists)
                        .totalDistanceMeters(metrics.distanceMeters)
                        .totalDurationSeconds(metrics.durationSeconds)
                        .totalWeightKg(currentWeightKg)
                        .totalCostVnd(routeCost)
                        .build());
            }
        }

        // ═════════════════════════════════════════════════════════════════════
        // ISSUE #7: INTER-ROUTE RELOCATE
        // ═════════════════════════════════════════════════════════════════════
        // NN + 2-opt chỉ tối ưu intra-route (trong 1 tuyến).
        // Relocate thử di chuyển đơn từ xe A sang xe B nếu giảm tổng cost.
        //
        // Ví dụ:
        //   Xe A: [1, 2, 3, 7] → đơn 7 ở xa cụm, gần cụm xe B hơn
        //   Xe B: [4, 5, 6]
        //   → Di chuyển đơn 7 sang xe B: cost giảm 15%
        //
        // Tại sao cần inter-route optimization?
        //   → NN gán đơn vào xe theo thứ tự greedy, nhưng đơn cuối cùng
        //     của xe trước có thể nên thuộc xe sau (nếu xe sau đi qua gần)
        //   → 2-opt không thể giải quyết vì chỉ đảo thứ tự trong 1 tuyến
        //   → Relocate bổ sung chiều tối ưu inter-route
        // ═════════════════════════════════════════════════════════════════════
        if (plannedRoutes.size() >= 2) {
            plannedRoutes = applyInterRouteRelocate(
                    plannedRoutes, depotLocation, distanceMap, routeDate, depotStartTime);
        }

        // Tính tổng
        double totalDistance = plannedRoutes.stream()
                .mapToDouble(PlannedRouteDto::getTotalDistanceMeters).sum();
        double totalCost = plannedRoutes.stream()
                .mapToDouble(PlannedRouteDto::getTotalCostVnd).sum();

        log.info("Hoàn thành! {} tuyến, {} đơn chưa phân. Tổng: {}km — Chi phí: {}đ",
                plannedRoutes.size(), remainingCount,
                String.format("%.2f", totalDistance / 1000),
                String.format("%,.0f", totalCost));

        return VrpSolutionDto.builder()
                .routes(plannedRoutes)
                .totalDistanceMeters(totalDistance)
                .totalCostVnd(totalCost)
                .unassignedOrderCount(remainingCount)
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FIND NEAREST FEASIBLE ORDER — dùng index thay vì object reference
    // ═════════════════════════════════════════════════════════════════════════
    private int findNearestFeasibleOrderIdx(
            Location currentLocation,
            List<Order> allOrders,
            boolean[] assigned,
            Map<String, DistanceMatrix> distanceMap,
            double currentWeightKg, double maxWeightKg,
            double currentVolumeM3, double maxVolumeM3,
            long currentDurationSeconds, long maxDrivingSeconds,
            LocalDateTime clock,
            LocalDate routeDate,
            LocalTime depotEndTime,
            Location depotLocation) {

        int    nearestIdx = -1;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < allOrders.size(); i++) {
            if (assigned[i]) continue; // đã được gán → bỏ qua O(1)

            Order candidate = allOrders.get(i);

            // Ràng buộc 1: Tải trọng
            double orderWeight = candidate.getTotalWeightKg() != null
                    ? candidate.getTotalWeightKg().doubleValue() : 0;
            if (currentWeightKg + orderWeight > maxWeightKg) continue;

            // Ràng buộc 2: Dung tích
            double orderVolume = candidate.getTotalVolumeM3() != null
                    ? candidate.getTotalVolumeM3().doubleValue() : 0;
            if (currentVolumeM3 + orderVolume > maxVolumeM3) continue;

            // Ràng buộc 3: Khoảng cách
            String key = currentLocation.getId() + "-" + candidate.getLocation().getId();
            DistanceMatrix dm = distanceMap.get(key);
            if (dm == null) continue;

            // Ràng buộc 4: Thời gian lái xe tối đa
            long estimatedDuration = currentDurationSeconds + dm.getDurationSeconds();
            if (estimatedDuration > maxDrivingSeconds) continue;

            // Ràng buộc 5: Khung giờ giao hàng
            LocalDateTime estimatedArrival = clock.plusSeconds(dm.getDurationSeconds());
            if (candidate.getTimeWindowTo() != null) {
                LocalDateTime deadline = routeDate.atTime(candidate.getTimeWindowTo());
                if (estimatedArrival.isAfter(deadline)) continue;
            }

            // ═══════════════════════════════════════════════════════════════
            // Ràng buộc 6: DEPOT RETURN TIME (Issue #15)
            // ═══════════════════════════════════════════════════════════════
            // Ước tính: đi tới đơn + phục vụ + về depot
            // Nếu về depot muộn hơn depotEndTime → bỏ qua đơn này
            // ═══════════════════════════════════════════════════════════════
            if (depotEndTime != null && depotLocation != null) {
                int serviceMin = candidate.getServiceTimeMinutes() != null
                        ? candidate.getServiceTimeMinutes() : 15;
                LocalDateTime afterService = estimatedArrival.plusMinutes(serviceMin);

                // Thời gian về depot
                String returnKey = candidate.getLocation().getId() + "-" + depotLocation.getId();
                DistanceMatrix returnDm = distanceMap.get(returnKey);
                long returnSeconds = returnDm != null ? returnDm.getDurationSeconds() : 0;
                LocalDateTime estimatedReturn = afterService.plusSeconds(returnSeconds);
                LocalDateTime depotDeadline = routeDate.atTime(depotEndTime);

                if (estimatedReturn.isAfter(depotDeadline)) continue;
            }

            // Chọn đơn gần nhất
            double dist = dm.getDistanceMeters().doubleValue();
            if (dist < minDistance) {
                nearestIdx = i;
                minDistance = dist;
            }
        }

        return nearestIdx;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // INTER-ROUTE RELOCATE OPERATOR
    // ═════════════════════════════════════════════════════════════════════════
    // Thử di chuyển từng đơn từ mỗi tuyến sang tuyến khác.
    // Nếu tổng cost giảm VÀ tuyến đích vẫn khả thi → thực hiện.
    //
    // Complexity: O(R² × S) với R = số tuyến, S = trung bình stop/tuyến
    // Thường R < 10, S < 50 → rất nhanh.
    // ═════════════════════════════════════════════════════════════════════════
    private List<PlannedRouteDto> applyInterRouteRelocate(
            List<PlannedRouteDto> routes,
            Location depotLocation,
            Map<String, DistanceMatrix> distanceMap,
            LocalDate routeDate,
            LocalTime depotStartTime) {

        // Convert to mutable lists
        List<PlannedRouteDto> mutableRoutes = new ArrayList<>(routes);
        boolean improved = true;
        int totalRelocations = 0;

        while (improved) {
            improved = false;

            for (int srcIdx = 0; srcIdx < mutableRoutes.size(); srcIdx++) {
                PlannedRouteDto srcRoute = mutableRoutes.get(srcIdx);
                if (srcRoute.getOrderStops().size() <= 1) continue; // không rút từ tuyến có 1 đơn

                for (int stopIdx = 0; stopIdx < srcRoute.getOrderStops().size(); stopIdx++) {
                    Order candidate = srcRoute.getOrderStops().get(stopIdx);

                    // Thử chèn vào mỗi tuyến đích
                    for (int dstIdx = 0; dstIdx < mutableRoutes.size(); dstIdx++) {
                        if (dstIdx == srcIdx) continue;

                        PlannedRouteDto dstRoute = mutableRoutes.get(dstIdx);

                        // Kiểm tra tải trọng tuyến đích
                        double candidateWeight = candidate.getTotalWeightKg() != null
                                ? candidate.getTotalWeightKg().doubleValue() : 0;
                        if (dstRoute.getTotalWeightKg() + candidateWeight
                                > dstRoute.getVehicle().getVehicleType().getMaxWeightKg().doubleValue()) {
                            continue;
                        }

                        // Kiểm tra dung tích tuyến đích
                        double candidateVolume = candidate.getTotalVolumeM3() != null
                                ? candidate.getTotalVolumeM3().doubleValue() : 0;
                        double dstMaxVolume = dstRoute.getVehicle().getVehicleType().getMaxVolumeM3() != null
                                ? dstRoute.getVehicle().getVehicleType().getMaxVolumeM3().doubleValue()
                                : Double.MAX_VALUE;
                        double dstCurrentVolume = dstRoute.getOrderStops().stream()
                                .mapToDouble(o -> o.getTotalVolumeM3() != null ? o.getTotalVolumeM3().doubleValue() : 0)
                                .sum();
                        if (dstCurrentVolume + candidateVolume > dstMaxVolume) continue;

                        // Tạo tuyến mới: src - candidate, dst + candidate
                        List<Order> newSrcStops = new ArrayList<>(srcRoute.getOrderStops());
                        newSrcStops.remove(stopIdx);

                        List<Order> newDstStops = new ArrayList<>(dstRoute.getOrderStops());
                        // Tìm vị trí chèn tối ưu (vị trí giảm distance nhiều nhất)
                        int bestInsertPos = findBestInsertPosition(
                                candidate, newDstStops, depotLocation, distanceMap);
                        newDstStops.add(bestInsertPos, candidate);

                        // Tính cost mới
                        RouteMetrics newSrcMetrics = recalculateRouteMetrics(
                                newSrcStops, depotLocation, distanceMap, routeDate, depotStartTime);
                        RouteMetrics newDstMetrics = recalculateRouteMetrics(
                                newDstStops, depotLocation, distanceMap, routeDate, depotStartTime);

                        double oldSrcCost = srcRoute.getTotalCostVnd();
                        double oldDstCost = dstRoute.getTotalCostVnd();

                        Vehicle srcVehicle = srcRoute.getVehicle();
                        Vehicle dstVehicle = dstRoute.getVehicle();

                        double newSrcCost = calcCost(srcVehicle, newSrcMetrics.distanceMeters);
                        double newDstCost = calcCost(dstVehicle, newDstMetrics.distanceMeters);

                        double oldTotal = oldSrcCost + oldDstCost;
                        double newTotal = newSrcCost + newDstCost;

                        // Chỉ relocate nếu giảm cost ít nhất 0.01đ
                        if (newTotal < oldTotal - 0.01) {
                            // Cập nhật tuyến
                            mutableRoutes.set(srcIdx, PlannedRouteDto.builder()
                                    .vehicle(srcVehicle)
                                    .orderStops(newSrcStops)
                                    .totalDistanceMeters(newSrcMetrics.distanceMeters)
                                    .totalDurationSeconds(newSrcMetrics.durationSeconds)
                                    .totalWeightKg(srcRoute.getTotalWeightKg() - candidateWeight)
                                    .totalCostVnd(newSrcCost)
                                    .build());

                            mutableRoutes.set(dstIdx, PlannedRouteDto.builder()
                                    .vehicle(dstVehicle)
                                    .orderStops(newDstStops)
                                    .totalDistanceMeters(newDstMetrics.distanceMeters)
                                    .totalDurationSeconds(newDstMetrics.durationSeconds)
                                    .totalWeightKg(dstRoute.getTotalWeightKg() + candidateWeight)
                                    .totalCostVnd(newDstCost)
                                    .build());

                            improved = true;
                            totalRelocations++;
                            log.debug("Relocate đơn #{} từ {} → {}: tiết kiệm {}đ",
                                    candidate.getId(),
                                    srcVehicle.getLicensePlate(),
                                    dstVehicle.getLicensePlate(),
                                    String.format("%,.0f", oldTotal - newTotal));
                            break; // restart inner loop vì routes đã thay đổi
                        }
                    }
                    if (improved) break; // restart stop loop
                }
                if (improved) break; // restart route loop
            }
        }

        // Xóa tuyến rỗng (nếu mọi đơn được relocate sang xe khác)
        mutableRoutes.removeIf(r -> r.getOrderStops().isEmpty());

        if (totalRelocations > 0) {
            double totalCostAfter = mutableRoutes.stream().mapToDouble(PlannedRouteDto::getTotalCostVnd).sum();
            double totalCostBefore = routes.stream().mapToDouble(PlannedRouteDto::getTotalCostVnd).sum();
            log.info("🔄 Relocate hoàn thành: {} lần di chuyển, tiết kiệm {}đ ({}→{}đ)",
                    totalRelocations,
                    String.format("%,.0f", totalCostBefore - totalCostAfter),
                    String.format("%,.0f", totalCostBefore),
                    String.format("%,.0f", totalCostAfter));
        }

        return mutableRoutes;
    }

    /**
     * Tìm vị trí chèn tối ưu cho đơn hàng vào tuyến đích.
     * Thử chèn ở mỗi vị trí [0..n], chọn vị trí tăng distance ít nhất.
     */
    private int findBestInsertPosition(
            Order candidate,
            List<Order> stops,
            Location depotLocation,
            Map<String, DistanceMatrix> distanceMap) {

        if (stops.isEmpty()) return 0;

        int bestPos = 0;
        double minInsertionCost = Double.MAX_VALUE;

        for (int pos = 0; pos <= stops.size(); pos++) {
            // Tính chi phí chèn tại vị trí pos
            Location prevLoc = pos == 0 ? depotLocation : stops.get(pos - 1).getLocation();
            Location nextLoc = pos == stops.size() ? depotLocation : stops.get(pos).getLocation();

            // Cost mới = prev→candidate + candidate→next
            double costIn = getDistance(prevLoc, candidate.getLocation(), distanceMap)
                    + getDistance(candidate.getLocation(), nextLoc, distanceMap);
            // Cost cũ = prev→next (đã có trước khi chèn)
            double costOut = getDistance(prevLoc, nextLoc, distanceMap);

            double insertionCost = costIn - costOut;
            if (insertionCost < minInsertionCost) {
                minInsertionCost = insertionCost;
                bestPos = pos;
            }
        }

        return bestPos;
    }

    private double getDistance(Location from, Location to, Map<String, DistanceMatrix> distanceMap) {
        String key = from.getId() + "-" + to.getId();
        DistanceMatrix dm = distanceMap.get(key);
        return dm != null ? dm.getDistanceMeters().doubleValue() : 0;
    }

    private double calcCost(Vehicle vehicle, double distanceMeters) {
        double fixedCost = vehicle.getVehicleType().getFixedCost() != null
                ? vehicle.getVehicleType().getFixedCost().doubleValue() : 0;
        double costPerKm = vehicle.getVehicleType().getCostPerKm().doubleValue();
        return fixedCost + costPerKm * (distanceMeters / 1000.0);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ROUTE METRICS — tính lại distance/duration cho 1 tuyến sau khi thay đổi
    // ═════════════════════════════════════════════════════════════════════════
    private record RouteMetrics(double distanceMeters, long durationSeconds) {}

    private RouteMetrics recalculateRouteMetrics(
            List<Order> stops,
            Location depotLocation,
            Map<String, DistanceMatrix> distanceMap,
            LocalDate routeDate,
            LocalTime depotStartTime) {

        double distance = 0;
        long duration = 0;
        Location loc = depotLocation;
        LocalDateTime clock = routeDate.atTime(depotStartTime);

        for (Order stop : stops) {
            String k = loc.getId() + "-" + stop.getLocation().getId();
            DistanceMatrix d = distanceMap.get(k);
            if (d != null) {
                distance += d.getDistanceMeters().doubleValue();
                duration += d.getDurationSeconds();
                clock = clock.plusSeconds(d.getDurationSeconds());
            }

            if (stop.getTimeWindowFrom() != null) {
                LocalDateTime windowOpen = routeDate.atTime(stop.getTimeWindowFrom());
                if (clock.isBefore(windowOpen)) {
                    duration += Duration.between(clock, windowOpen).getSeconds();
                    clock = windowOpen;
                }
            }

            int svcMin = stop.getServiceTimeMinutes() != null ? stop.getServiceTimeMinutes() : 15;
            duration += svcMin * 60L;
            clock = clock.plusMinutes(svcMin);

            loc = stop.getLocation();
        }

        // Quay về depot
        String returnKey = loc.getId() + "-" + depotLocation.getId();
        DistanceMatrix returnDm = distanceMap.get(returnKey);
        if (returnDm != null) {
            distance += returnDm.getDistanceMeters().doubleValue();
            duration += returnDm.getDurationSeconds();
        }

        return new RouteMetrics(distance, duration);
    }
}
