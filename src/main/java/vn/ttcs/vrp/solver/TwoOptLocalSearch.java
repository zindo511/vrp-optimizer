package vn.ttcs.vrp.solver;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.ttcs.vrp.model.DistanceMatrix;
import vn.ttcs.vrp.model.Location;
import vn.ttcs.vrp.model.Order;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 2-opt Local Search — cải thiện thứ tự stop trong 1 tuyến đã có.
 *
 * ═══════════════════════════════════════════════════════════════════
 * TẠI SAO CẦN 2-OPT?
 * ═══════════════════════════════════════════════════════════════════
 *
 * Nearest Neighbor là thuật toán tham lam (greedy) — ở mỗi bước nó
 * chọn điểm gần nhất tính từ vị trí hiện tại. Kết quả thường có
 * dạng "cạnh giao nhau" (crossed edges), gây lãng phí khoảng cách.
 *
 * Ví dụ NN build được tuyến:
 *     Depot → A → D → B → C → Depot   (tổng 80km, cạnh A→D cắt B→C)
 *
 * Sau 2-opt:
 *     Depot → A → B → C → D → Depot   (tổng 60km, bỏ giao cắt)
 *
 * 2-opt thử đảo ngược (reverse) mọi đoạn con liên tiếp [i..j] trong
 * tuyến. Nếu tổng khoảng cách giảm → giữ lại. Lặp cho đến khi
 * không cải thiện được nữa (hội tụ).
 *
 * ═══════════════════════════════════════════════════════════════════
 * ĐỘ PHỨC TẠP VÀ HIỆU QUẢ
 * ═══════════════════════════════════════════════════════════════════
 *
 * - Mỗi vòng lặp: O(n²) với n = số stop
 * - Thường hội tụ sau 3-5 vòng
 * - Cải thiện trung bình 10-25% so với NN thuần
 *
 * ═══════════════════════════════════════════════════════════════════
 * XỬ LÝ TIME WINDOW
 * ═══════════════════════════════════════════════════════════════════
 *
 * Sau khi đảo thứ tự, thứ tự giao hàng thay đổi → cần kiểm tra lại
 * tất cả time window. Nếu bất kỳ đơn nào vi phạm (xe đến sau
 * deadline) → REVERT về thứ tự cũ, bỏ qua lần đảo này.
 *
 * Lý do: time window là HARD CONSTRAINT (bắt buộc tuân thủ), khác
 * với khoảng cách là SOFT OBJECTIVE (tối ưu nhưng không bắt buộc).
 */
@Component
@Slf4j(topic = "TWO-OPT")
public class TwoOptLocalSearch {

    /**
     * Chạy 2-opt trên 1 tuyến đã có, trả về thứ tự stop mới đã cải thiện.
     *
     * @param stops         danh sách đơn hàng theo thứ tự NN đã build
     * @param depotLocation vị trí depot (điểm xuất phát và kết thúc)
     * @param distanceMap   ma trận khoảng cách tra cứu nhanh
     * @param routeDate     ngày chạy tuyến (để kiểm tra time window)
     * @param depotStartTime giờ xe xuất phát từ depot
     * @return list mới đã sắp xếp tối ưu hơn (có thể giống cũ nếu k cải thiện được)
     */
    public List<Order> improve(
            List<Order> stops,
            Location depotLocation,
            Map<String, DistanceMatrix> distanceMap,
            LocalDate routeDate,
            LocalTime depotStartTime) {

        if (stops.size() <= 2) {
            // Tuyến có <= 2 stop thì không có gì để đảo
            return stops;
        }

        List<Order> bestRoute = new ArrayList<>(stops);
        double bestDistance = calculateTotalDistance(bestRoute, depotLocation, distanceMap);
        boolean improved = true;

        int iteration = 0;
        while (improved) {
            improved = false;
            iteration++;

            for (int i = 0; i < bestRoute.size() - 1; i++) {
                for (int j = i + 1; j < bestRoute.size(); j++) {

                    // Tạo tuyến mới bằng cách đảo ngược đoạn [i..j]
                    List<Order> newRoute = twoOptSwap(bestRoute, i, j);

                    // ── Kiểm tra TIME WINDOW ────────────────────────────
                    // Đây là bước QUAN TRỌNG: 2-opt chỉ tối ưu khoảng cách
                    // nhưng KHÔNG ĐƯỢC vi phạm ràng buộc thời gian.
                    // Nếu sau khi đảo, bất kỳ đơn nào bị xe đến trễ
                    // → bỏ qua lần đảo này hoàn toàn.
                    if (!isTimeWindowFeasible(newRoute, depotLocation, distanceMap, routeDate, depotStartTime)) {
                        continue;
                    }

                    double newDistance = calculateTotalDistance(newRoute, depotLocation, distanceMap);

                    if (newDistance < bestDistance - 0.01) {
                        // Tuyến mới ngắn hơn VÀ khả thi về time window → giữ lại
                        bestRoute = newRoute;
                        bestDistance = newDistance;
                        improved = true;
                    }
                }
            }
        }

        double originalDist = calculateTotalDistance(stops, depotLocation, distanceMap);
        double savedMeters = originalDist - bestDistance;
        if (savedMeters > 1) {
            log.info("2-opt cải thiện: {:.2f}km → {:.2f}km (tiết kiệm {:.2f}km, {} vòng lặp)"
                            .replace("{:.2f}", "%.2f"),
                    originalDist / 1000, bestDistance / 1000, savedMeters / 1000, iteration);
        }

        return bestRoute;
    }

    /**
     * Đảo ngược đoạn [i..j] trong tuyến.
     *
     * Ví dụ: route = [A, B, C, D, E], i=1, j=3
     * Kết quả: [A, D, C, B, E]  (đoạn B-C-D bị đảo thành D-C-B)
     *
     * Tại sao dùng reverse thay vì swap đơn giản?
     * → Reverse giữ nguyên tính liên tục của tuyến. Nếu chỉ swap 2 điểm,
     *   ta có thể tạo ra tuyến zigzag tệ hơn. Reverse đảm bảo rằng
     *   nếu một cụm điểm đang theo thứ tự tốt, ta chỉ đổi hướng đi
     *   qua cụm đó chứ không phá vỡ cụm.
     */
    private List<Order> twoOptSwap(List<Order> route, int i, int j) {
        List<Order> newRoute = new ArrayList<>(route.size());

        // Giữ nguyên phần đầu [0..i-1]
        for (int k = 0; k < i; k++) {
            newRoute.add(route.get(k));
        }

        // Đảo ngược đoạn [i..j]
        for (int k = j; k >= i; k--) {
            newRoute.add(route.get(k));
        }

        // Giữ nguyên phần cuối [j+1..n-1]
        for (int k = j + 1; k < route.size(); k++) {
            newRoute.add(route.get(k));
        }

        return newRoute;
    }

    /**
     * Tính tổng khoảng cách của tuyến: Depot → stop₁ → ... → stopₙ → Depot
     */
    private double calculateTotalDistance(
            List<Order> route,
            Location depotLocation,
            Map<String, DistanceMatrix> distanceMap) {

        double total = 0;
        Location current = depotLocation;

        for (Order stop : route) {
            String key = current.getId() + "-" + stop.getLocation().getId();
            DistanceMatrix dm = distanceMap.get(key);
            if (dm != null) {
                total += dm.getDistanceMeters().doubleValue();
            }
            current = stop.getLocation();
        }

        // Quay về depot
        String returnKey = current.getId() + "-" + depotLocation.getId();
        DistanceMatrix returnDm = distanceMap.get(returnKey);
        if (returnDm != null) {
            total += returnDm.getDistanceMeters().doubleValue();
        }

        return total;
    }

    /**
     * Kiểm tra toàn bộ tuyến mới có thỏa mãn time window không.
     *
     * Mô phỏng xe chạy từ depot → stop₁ → ... → stopₙ:
     *   - Cộng thời gian di chuyển + thời gian phục vụ
     *   - Nếu đến sớm → chờ (cho phép, thêm vào clock)
     *   - Nếu đến trễ (sau timeWindowTo) → FALSE → revert 2-opt swap
     *
     * Tại sao phải check lại toàn bộ thay vì chỉ check 2 stop bị đổi?
     * → Vì khi đảo đoạn [i..j], thời gian đến tất cả stop từ i+1 trở đi
     *   đều thay đổi (hiệu ứng domino). Chỉ check stop i và j là không đủ.
     */
    private boolean isTimeWindowFeasible(
            List<Order> route,
            Location depotLocation,
            Map<String, DistanceMatrix> distanceMap,
            LocalDate routeDate,
            LocalTime depotStartTime) {

        LocalDateTime clock = routeDate.atTime(depotStartTime);
        Location current = depotLocation;

        for (Order stop : route) {
            String key = current.getId() + "-" + stop.getLocation().getId();
            DistanceMatrix dm = distanceMap.get(key);

            if (dm != null) {
                clock = clock.plusSeconds(dm.getDurationSeconds());
            }

            // Nếu đến SAU deadline → vi phạm hard constraint
            if (stop.getTimeWindowTo() != null) {
                LocalDateTime deadline = routeDate.atTime(stop.getTimeWindowTo());
                if (clock.isAfter(deadline)) {
                    return false;
                }
            }

            // Nếu đến SỚM → chờ (OK, không vi phạm)
            if (stop.getTimeWindowFrom() != null) {
                LocalDateTime windowOpen = routeDate.atTime(stop.getTimeWindowFrom());
                if (clock.isBefore(windowOpen)) {
                    clock = windowOpen;
                }
            }

            // Cộng thời gian phục vụ
            int serviceMin = stop.getServiceTimeMinutes() != null ? stop.getServiceTimeMinutes() : 15;
            clock = clock.plusMinutes(serviceMin);

            current = stop.getLocation();
        }

        return true;
    }
}
