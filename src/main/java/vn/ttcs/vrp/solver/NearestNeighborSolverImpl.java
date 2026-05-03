package vn.ttcs.vrp.solver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.ttcs.vrp.dto.vrp.PlannedRouteDto;
import vn.ttcs.vrp.dto.vrp.VrpSolutionDto;
import vn.ttcs.vrp.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Thuật toán láng giềng gần nhất (Nearest Neighbor) + 2-opt + Cost-aware
 *
 * ═══════════════════════════════════════════════════════════════════
 * PIPELINE:
 *   1. Sắp xếp xe theo costPerKm tăng dần (cost-aware — 6.2)
 *   2. Xây tuyến greedy bằng NN (giữ nguyên logic cũ)
 *   3. Chạy 2-opt cải thiện thứ tự stop (local search — 6.1)
 *   4. Tính chi phí thực tế: fixedCost + costPerKm × km (6.2)
 * ═══════════════════════════════════════════════════════════════════
 *
 * Ưu điểm: Đơn giản, nhanh, dễ hiểu, kết quả khá tốt
 * Nhược điểm: Không đảm bảo tối ưu toàn cục (nhưng 2-opt bù đắp phần nào)
 * Ràng buộc được kiểm tra: tải trọng, dung tích, khung giờ giao hàng
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
            LocalTime depotStartTime) {

        log.info("Nearest Neighbor bắt đầu xử lý {} đơn với {} xe", orders.size(), vehicles.size());

        // ═════════════════════════════════════════════════════════════════════
        // COST-AWARE (6.2): Sắp xếp xe theo costPerKm tăng dần
        // ═════════════════════════════════════════════════════════════════════
        // Lý do: Trong logistics thực tế, mục tiêu chính là GIẢM CHI PHÍ chứ
        // không chỉ giảm km. Xe nhỏ (costPerKm=5k) chạy 20km rẻ hơn xe lớn
        // (costPerKm=15k) chạy 10km. Bằng cách ưu tiên xe rẻ trước, ta đảm
        // bảo xe rẻ được gán đơn trước, xe đắt chỉ dùng khi cần thiết.
        //
        // Ví dụ: 3 đơn nhẹ 10kg → dùng xe bike (50k fixed + 5k/km)
        //        thay vì truck (500k fixed + 15k/km) → tiết kiệm ~450k
        // ═════════════════════════════════════════════════════════════════════
        List<Vehicle> sortedVehicles = new ArrayList<>(vehicles);
        sortedVehicles.sort(Comparator.comparing(
                v -> v.getVehicleType().getCostPerKm()));

        log.info("Thứ tự xe (cost-aware): {}",
                sortedVehicles.stream()
                        .map(v -> v.getLicensePlate() + "(" + v.getVehicleType().getCostPerKm() + "đ/km)")
                        .toList());

        List<PlannedRouteDto> plannedRoutes = new ArrayList<>();
        List<Order> unassigned = new ArrayList<>(orders);

        for (Vehicle vehicle : sortedVehicles) {
            if (unassigned.isEmpty()) break;

            Location currentLocation = depotLocation;
            double currentWeightKg   = 0;
            double currentVolumeM3   = 0;
            double routeDistanceMeters = 0;
            long   routeDurationSeconds = 0;
            List<Order> stopLists = new ArrayList<>();

            double maxWeightKg = vehicle.getVehicleType().getMaxWeightKg().doubleValue();

            // Nếu VehicleType không khai báo maxVolumeM3 → coi như không giới hạn dung tích
            double maxVolumeM3 = vehicle.getVehicleType().getMaxVolumeM3() != null
                    ? vehicle.getVehicleType().getMaxVolumeM3().doubleValue()
                    : Double.MAX_VALUE;

            // Đồng hồ thời gian thực của xe — bắt đầu từ giờ mở cửa depot
            // Dùng để kiểm tra và tuân thủ khung giờ giao hàng của từng đơn
            LocalDateTime clock = routeDate.atTime(depotStartTime);

            while (!unassigned.isEmpty()) {
                // Tìm đơn hàng gần nhất trong số các đơn thỏa mãn đồng thời:
                //   (1) không vượt tải trọng xe
                //   (2) không vượt dung tích xe
                //   (3) xe đến kịp trước deadline của đơn
                Order nearest = findNearestFeasibleOrder(
                        currentLocation, unassigned, distanceMap,
                        currentWeightKg, maxWeightKg,
                        currentVolumeM3, maxVolumeM3,
                        clock, routeDate);

                // Không còn đơn khả thi → kết thúc tuyến cho xe này
                if (nearest == null) break;

                // Lấy thông tin di chuyển từ vị trí hiện tại đến đơn hàng vừa chọn
                // (dm chắc chắn tồn tại vì findNearestFeasibleOrder đã lọc dm == null)
                String key = currentLocation.getId() + "-" + nearest.getLocation().getId();
                DistanceMatrix dm = distanceMap.get(key);
                if (dm != null) {
                    routeDistanceMeters += dm.getDistanceMeters().doubleValue();
                    routeDurationSeconds += dm.getDurationSeconds();
                    clock = clock.plusSeconds(dm.getDurationSeconds()); // tiến đồng hồ theo quãng đường di chuyển
                }

                // ── Xử lý Time Window ──────────────────────────────────────────────────
                // Nếu xe đến SỚM hơn khung giờ mở (timeWindowFrom) → xe phải chờ
                // Thời gian chờ được tính vào tổng thời gian tuyến (ảnh hưởng các stop sau)
                if (nearest.getTimeWindowFrom() != null) {
                    LocalDateTime windowOpen = routeDate.atTime(nearest.getTimeWindowFrom());
                    if (clock.isBefore(windowOpen)) {
                        long waitSeconds = java.time.Duration.between(clock, windowOpen).getSeconds();
                        routeDurationSeconds += waitSeconds;
                        clock = windowOpen;
                        log.debug("Xe [{}] chờ {}ph tại đơn #{} (đến {}, mở cửa {})",
                                vehicle.getLicensePlate(), waitSeconds / 60,
                                nearest.getId(), clock.toLocalTime(), nearest.getTimeWindowFrom());
                    }
                }
                // ───────────────────────────────────────────────────────────────────────

                // Cộng thời gian phục vụ tại điểm giao (mặc định 15 phút nếu không khai báo)
                int serviceMin = nearest.getServiceTimeMinutes() != null ? nearest.getServiceTimeMinutes() : 15;
                routeDurationSeconds += serviceMin * 60L;
                clock = clock.plusMinutes(serviceMin);

                // Cập nhật tích lũy tải trọng và dung tích đã dùng
                stopLists.add(nearest);
                currentWeightKg += nearest.getTotalWeightKg().doubleValue();
                if (nearest.getTotalVolumeM3() != null) {
                    currentVolumeM3 += nearest.getTotalVolumeM3().doubleValue();
                }
                currentLocation = nearest.getLocation();
                unassigned.remove(nearest);
            }

            if (!stopLists.isEmpty()) {

                // ═════════════════════════════════════════════════════════════
                // 2-OPT LOCAL SEARCH (6.1): Cải thiện thứ tự stop
                // ═════════════════════════════════════════════════════════════
                // Tại sao gọi 2-opt ở đây (sau NN, trước tính tổng)?
                //   → NN xây tuyến theo thứ tự greedy, thường tạo ra
                //     "crossed edges" (cạnh giao nhau). 2-opt đảo ngược
                //     các đoạn con để loại bỏ giao cắt.
                //   → Phải gọi TRƯỚC khi tính tổng khoảng cách vì 2-opt
                //     thay đổi thứ tự stop → khoảng cách thay đổi.
                // ═════════════════════════════════════════════════════════════
                List<Order> improvedStops = twoOptLocalSearch.improve(
                        stopLists, depotLocation, distanceMap,
                        routeDate, depotStartTime);
                stopLists = improvedStops;

                // Tính lại khoảng cách và thời gian sau 2-opt
                // (vì thứ tự stop có thể đã thay đổi)
                routeDistanceMeters = 0;
                routeDurationSeconds = 0;
                Location loc = depotLocation;
                LocalDateTime recalcClock = routeDate.atTime(depotStartTime);

                for (Order stop : stopLists) {
                    String k = loc.getId() + "-" + stop.getLocation().getId();
                    DistanceMatrix d = distanceMap.get(k);
                    if (d != null) {
                        routeDistanceMeters += d.getDistanceMeters().doubleValue();
                        routeDurationSeconds += d.getDurationSeconds();
                        recalcClock = recalcClock.plusSeconds(d.getDurationSeconds());
                    }

                    // Xử lý chờ time window
                    if (stop.getTimeWindowFrom() != null) {
                        LocalDateTime windowOpen = routeDate.atTime(stop.getTimeWindowFrom());
                        if (recalcClock.isBefore(windowOpen)) {
                            routeDurationSeconds += java.time.Duration.between(recalcClock, windowOpen).getSeconds();
                            recalcClock = windowOpen;
                        }
                    }

                    int svcMin = stop.getServiceTimeMinutes() != null ? stop.getServiceTimeMinutes() : 15;
                    routeDurationSeconds += svcMin * 60L;
                    recalcClock = recalcClock.plusMinutes(svcMin);

                    loc = stop.getLocation();
                }

                // Cộng quãng đường và thời gian quay về depot
                String returnKey = loc.getId() + "-" + depotLocation.getId();
                DistanceMatrix returnDm = distanceMap.get(returnKey);
                if (returnDm != null) {
                    routeDistanceMeters += returnDm.getDistanceMeters().doubleValue();
                    routeDurationSeconds += returnDm.getDurationSeconds();
                }

                // ═════════════════════════════════════════════════════════════
                // COST-AWARE (6.2): Tính chi phí thực tế cho tuyến
                // ═════════════════════════════════════════════════════════════
                // Công thức: totalCost = fixedCost + costPerKm × (distance / 1000)
                //
                // fixedCost: chi phí cố định mỗi lần xuất xe (bảo hiểm, khấu hao,
                //            lương tài xế tối thiểu...) → không phụ thuộc quãng đường
                // costPerKm: chi phí biến đổi theo km (xăng, hao mòn lốp...)
                //
                // Tại sao tính ở đây (solver) thay vì ở Engine?
                //   → Vì GA solver cần fitness = 1/cost để so sánh các cá thể.
                //     Nếu chỉ tính ở Engine thì GA không thể tối ưu cost.
                // ═════════════════════════════════════════════════════════════
                double fixedCost = vehicle.getVehicleType().getFixedCost() != null
                        ? vehicle.getVehicleType().getFixedCost().doubleValue() : 0;
                double costPerKm = vehicle.getVehicleType().getCostPerKm().doubleValue();
                double routeCost = fixedCost + costPerKm * (routeDistanceMeters / 1000.0);

                String volUsed = String.format("%.2f", currentVolumeM3);
                String volMax  = maxVolumeM3 == Double.MAX_VALUE ? "∞" : String.format("%.2f", maxVolumeM3);
                log.info("Xe [{}]: {} đơn — {}km — {}/{}kg — {}/{}m³ — chi phí: {}đ",
                        vehicle.getLicensePlate(), stopLists.size(),
                        String.format("%.2f", routeDistanceMeters / 1000),
                        String.format("%.1f", currentWeightKg), String.format("%.1f", maxWeightKg),
                        volUsed, volMax,
                        String.format("%,.0f", routeCost));

                plannedRoutes.add(PlannedRouteDto.builder()
                        .vehicle(vehicle)
                        .orderStops(stopLists)
                        .totalDistanceMeters(routeDistanceMeters)
                        .totalDurationSeconds(routeDurationSeconds)
                        .totalWeightKg(currentWeightKg)
                        .totalCostVnd(routeCost)
                        .build());
            }
        }

        double totalDistance = plannedRoutes.stream()
                .mapToDouble(PlannedRouteDto::getTotalDistanceMeters).sum();
        double totalCost = plannedRoutes.stream()
                .mapToDouble(PlannedRouteDto::getTotalCostVnd).sum();

        log.info("Hoàn thành! {} tuyến, {} đơn chưa phân. Tổng: {}km — Chi phí: {}đ",
                plannedRoutes.size(), unassigned.size(),
                String.format("%.2f", totalDistance / 1000),
                String.format("%,.0f", totalCost));

        return VrpSolutionDto.builder()
                .routes(plannedRoutes)
                .totalDistanceMeters(totalDistance)
                .totalCostVnd(totalCost)
                .unassignedOrderCount(unassigned.size())
                .build();
    }

    /**
     * Tìm đơn hàng gần nhất (theo khoảng cách thực) thỏa mãn đồng thời 3 ràng buộc:
     *
     *   1. Tải trọng: currentWeight + orderWeight <= maxWeight
     *   2. Dung tích: currentVolume + orderVolume <= maxVolume
     *   3. Khung giờ: xe đến trước hoặc đúng deadline (timeWindowTo)
     *      - Nếu đến sớm hơn timeWindowFrom → được tính là khả thi (xe sẽ chờ)
     *      - Nếu đến sau timeWindowTo       → loại đơn này (hard constraint)
     *
     * @return đơn hàng khả thi gần nhất, hoặc null nếu không còn đơn nào thỏa mãn
     */
    private Order findNearestFeasibleOrder(
            Location currentLocation,
            List<Order> candidates,
            Map<String, DistanceMatrix> distanceMap,
            double currentWeightKg, double maxWeightKg,
            double currentVolumeM3, double maxVolumeM3,
            LocalDateTime clock,
            LocalDate routeDate) {

        Order  nearestOrder = null;
        double minDistance  = Double.MAX_VALUE;

        for (Order candidate : candidates) {

            // ── Ràng buộc 1: Tải trọng ──────────────────────────────────────────
            double orderWeight = candidate.getTotalWeightKg() != null
                    ? candidate.getTotalWeightKg().doubleValue() : 0;
            if (currentWeightKg + orderWeight > maxWeightKg) continue;

            // ── Ràng buộc 2: Dung tích ──────────────────────────────────────────
            double orderVolume = candidate.getTotalVolumeM3() != null
                    ? candidate.getTotalVolumeM3().doubleValue() : 0;
            if (currentVolumeM3 + orderVolume > maxVolumeM3) continue;

            // ── Lấy thời gian di chuyển đến đơn này ─────────────────────────────
            // Bắt buộc phải có trong distanceMap; nếu không có → không thể đến → bỏ qua
            String key = currentLocation.getId() + "-" + candidate.getLocation().getId();
            DistanceMatrix dm = distanceMap.get(key);
            if (dm == null) continue;

            // ── Ràng buộc 3: Khung giờ — hard constraint ────────────────────────
            // Tính thời điểm xe dự kiến đến (chưa tính chờ, chỉ tính di chuyển)
            if (candidate.getTimeWindowTo() != null) {
                LocalDateTime estimatedArrival = clock.plusSeconds(dm.getDurationSeconds());
                LocalDateTime deadline = routeDate.atTime(candidate.getTimeWindowTo());
                if (estimatedArrival.isAfter(deadline)) {
                    // Xe đến sau cửa sổ thời gian → loại đơn này hoàn toàn
                    log.debug("Loại đơn #{} — dự kiến đến {} nhưng deadline {}",
                            candidate.getId(),
                            estimatedArrival.toLocalTime(),
                            candidate.getTimeWindowTo());
                    continue;
                }
            }
            // ────────────────────────────────────────────────────────────────────

            // Chọn đơn gần nhất trong số các đơn đã vượt qua tất cả ràng buộc
            double dist = dm.getDistanceMeters().doubleValue();
            if (dist < minDistance) {
                nearestOrder = candidate;
                minDistance  = dist;
            }
        }

        return nearestOrder;
    }
}
