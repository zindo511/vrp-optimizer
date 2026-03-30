package vn.ttcs.vrp.solver;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.ttcs.vrp.dto.vrp.PlannedRouteDto;
import vn.ttcs.vrp.dto.vrp.VrpSolutionDto;
import vn.ttcs.vrp.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Thuật toán láng giềng gần nhất
 * Ý tưởng: Ở mỗi bước, chọn ra thằng có vị trí gần nhất
 * Ưu điểm: Đơn giản, nhanh , dễ hiểu
 * Nhược điểm: Không đảm bảo tối ưu
 */
@Component("nearestNeighborSolver")
@Slf4j(topic = "NEAREST-NEIGHBOR-SOLVER")
public class NearestNeighborSolverImpl implements VrpSolver {

    @Override
    public VrpSolutionDto solve(
            List<Order> orders,
            List<Vehicle> vehicles,
            Location depotLocation,
            Map<String, DistanceMatrix> distanceMap,
            AlgorithmConfig config
    ) {

        log.info("Nearest Neighbor bắt đầu xử lý {} đơn với {} xe", orders.size(), vehicles.size());

        List<PlannedRouteDto> plannedRoutes = new ArrayList<>();

        // những đơn chưa được phân xe
        List<Order> unassigned = new ArrayList<>(orders);

        for (Vehicle vehicle : vehicles) {
            if (unassigned.isEmpty()) {
                break;
            }

            Location currentLocation = depotLocation;
            double currentWeightKg = 0;
            double routeDistanceMeters = 0;
            long routeDurationSeconds = 0;
            List<Order> stopLists = new ArrayList<>();

            // tải trọng mà xe có thể chở được
            double maxWeightKg = vehicle.getVehicleType().getMaxWeightKg().doubleValue();

            while (!unassigned.isEmpty()) {
                Order nearest = findNearestOrder(currentLocation, unassigned, distanceMap);
                if (nearest == null)
                    break;

                double orderWeight = nearest.getTotalWeightKg().doubleValue();

                // kiểm tra tải trọng
                if (currentWeightKg + orderWeight > maxWeightKg) {
                    log.debug("Xe [{}] đầy tải ({}/{}kg). Kết thúc tuyến xe",
                            vehicle.getLicensePlate(), currentWeightKg, maxWeightKg);
                    break;
                }

                // lấy khoảng cách từ vị trí hiện tại đến order
                String key = currentLocation.getId() + "-" + nearest.getLocation().getId();
                DistanceMatrix dm = distanceMap.get(key);
                if (dm != null) {
                    routeDistanceMeters += dm.getDistanceMeters().doubleValue();
                    routeDurationSeconds += dm.getDurationSeconds();
                    // cộng thời gian phục vụ
                    routeDurationSeconds += (nearest.getServiceTimeMinutes() != null
                        ? nearest.getServiceTimeMinutes() * 60L : 900L);
                }

                stopLists.add(nearest);
                currentWeightKg += orderWeight;
                currentLocation = nearest.getLocation();
                unassigned.remove(nearest);
            }

            // nếu xe có điểm đi tới
            if (!stopLists.isEmpty()) {
                // cộng thêm điểm quay về
                String returnKey = currentLocation.getId() + "-" + depotLocation.getId();
                DistanceMatrix returnDm = distanceMap.get(returnKey);
                if (returnDm != null) {
                    routeDistanceMeters += returnDm.getDistanceMeters().doubleValue();
                    routeDurationSeconds += returnDm.getDurationSeconds();
                }

                plannedRoutes.add(PlannedRouteDto.builder()
                                .vehicle(vehicle)
                                .orderStops(stopLists)
                                .totalDistanceMeters(routeDistanceMeters)
                                .totalDurationSeconds(routeDurationSeconds)
                                .totalWeightKg(currentWeightKg)
                        .build());
                log.info("Xe [{}]: {} đơn - {}km",
                        vehicle.getLicensePlate(), stopLists.size(), routeDistanceMeters / 1000);
            }
        }
        double totalDistance = plannedRoutes.stream()
                .mapToDouble(PlannedRouteDto::getTotalDistanceMeters).sum();

        log.info("Hoàn thành! {} tuyến, {} đơn chưa phân. Tổng: {}km",
                plannedRoutes.size(), unassigned.size(), totalDistance / 1000);
        return VrpSolutionDto.builder()
                .routes(plannedRoutes)
                .totalDistanceMeters(totalDistance)
                .unassignedOrderCount(unassigned.size())
                .build();
    }

    /**
     *
     * @param currentLocation vị trí hiện tải của xe
     * @param candidates những ứng viên còn lại
     * @param distanceMap ma trận khoảng cách dùng để tra cứu nhanh
     * @return trả về đơn hàng ở vị trí gần nhất
     */
    private Order findNearestOrder(
            Location currentLocation,
            List<Order> candidates,
            Map<String, DistanceMatrix> distanceMap
    ) {
        Order nearestOrder = null;
        double minDistance = Double.MAX_VALUE;

        for (Order candidate : candidates) {
            String key = currentLocation.getId() + "-" + candidate.getId();
            DistanceMatrix dm = distanceMap.get(key);

            if (dm != null) {
                double dist = dm.getDistanceMeters().doubleValue();
                if (dist < minDistance) {
                    nearestOrder = candidate;
                    minDistance = dist;
                }
            }
        }
        return nearestOrder;
    }
}
