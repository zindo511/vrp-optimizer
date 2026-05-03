package vn.ttcs.vrp.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import vn.ttcs.vrp.dto.response.OsrmTableResponse;
import vn.ttcs.vrp.model.Location;
import vn.ttcs.vrp.service.OsrmClientService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * OSRM Client với Circuit Breaker + Haversine Fallback
 *
 * ═══════════════════════════════════════════════════════════════════
 * TẠI SAO CẦN CIRCUIT BREAKER?
 * ═══════════════════════════════════════════════════════════════════
 *
 * OSRM public API (router.project-osrm.org) có thể:
 *   - Rate-limit → HTTP 429 Too Many Requests
 *   - OSRM server down → Connection timeout (30s mỗi request)
 *   - Network lỗi → tất cả request chờ timeout trước khi fail
 *
 * Nếu không có circuit breaker, khi OSRM chết:
 *   - Mỗi cặp điểm thiếu → gọi OSRM → chờ 30s → fail
 *   - 10 đơn = 100 cặp thiếu → chờ 50 phút (!) trước khi fail
 *
 * Với circuit breaker:
 *   - 3 lần fail liên tiếp → MỞ circuit → bỏ qua OSRM 60 giây
 *   - Dùng Haversine (đường chim bay × hệ số 1.3) làm ước lượng
 *   - Phân tuyến vẫn chạy được (chất lượng giảm nhẹ, không crash)
 *   - Sau 60 giây → thử lại OSRM (half-open state)
 *
 * ═══════════════════════════════════════════════════════════════════
 * HAVERSINE FALLBACK
 * ═══════════════════════════════════════════════════════════════════
 *
 * Công thức Haversine tính khoảng cách tuyến tính trên mặt cầu
 * giữa 2 điểm (lat, lng). Nhân hệ số 1.3 (detour factor) để
 * ước lượng khoảng cách đường bộ thực tế.
 *
 * Hệ số 1.3 là trung bình từ nghiên cứu logistics:
 *   - Khu vực đô thị dày đặc: ~1.4-1.5 (đường quanh co)
 *   - Khu vực ngoại thành: ~1.2-1.3
 *   - Trung bình: ~1.3
 *
 * Tốc độ ước lượng: 40 km/h → duration = distance / 40 × 3600
 */
@Service
@Slf4j(topic = "OSRM-CLIENT")
public class OsrmClientServiceImpl implements OsrmClientService {

    private static final double EARTH_RADIUS_METERS = 6_371_000;
    private static final double DETOUR_FACTOR = 1.3;
    private static final double ESTIMATED_SPEED_MPS = 40.0 * 1000 / 3600; // 40km/h → m/s

    private final RestClient restClient;

    // ── Circuit Breaker State ────────────────────────────────────
    private final int failureThreshold;
    private final long resetTimeoutMs;

    private int consecutiveFailures = 0;
    private long circuitOpenedAt = 0;

    // Circuit states: CLOSED (normal) → OPEN (skip OSRM) → HALF_OPEN (try once)
    private enum CircuitState { CLOSED, OPEN, HALF_OPEN }

    public OsrmClientServiceImpl(
            @Value("${osrm.base-url:https://router.project-osrm.org}") String osrmBaseUrl,
            @Value("${osrm-circuit-breaker.failure-threshold:3}") int failureThreshold,
            @Value("${osrm-circuit-breaker.reset-timeout-ms:60000}") long resetTimeoutMs) {

        this.failureThreshold = failureThreshold;
        this.resetTimeoutMs = resetTimeoutMs;

        this.restClient = RestClient.builder()
                .baseUrl(osrmBaseUrl)
                .defaultHeader("User-Agent", "VrpOptimizerApp/1.0 (ttcs.ptit)")
                .build();

        log.info("OSRM Client khởi tạo: baseUrl={}, failureThreshold={}, resetTimeoutMs={}",
                osrmBaseUrl, failureThreshold, resetTimeoutMs);
    }

    @Override
    public OsrmTableResponse getDistanceMatrix(List<Location> locations) {
        if (locations == null || locations.size() < 2) {
            log.warn("Chưa đủ điểm để tính ma trận OSRM");
            return null;
        }

        // ── Kiểm tra Circuit Breaker ────────────────────────────
        CircuitState state = getCircuitState();
        if (state == CircuitState.OPEN) {
            log.warn("⚡ Circuit OPEN — bỏ qua OSRM, dùng Haversine fallback ({} lỗi liên tiếp)",
                    consecutiveFailures);
            return buildHaversineFallback(locations);
        }

        if (state == CircuitState.HALF_OPEN) {
            log.info("⚡ Circuit HALF-OPEN — thử lại OSRM...");
        }

        // ── Gọi OSRM ───────────────────────────────────────────
        // OSRM yêu cầu Lon,Lat chứ không phải Lat,Lon
        String coordinates = locations.stream()
                .map(loc -> loc.getLongitude().toString() + "," + loc.getLatitude().toString())
                .collect(Collectors.joining(";"));

        try {
            // Ko dùng builder param vì RestClient mã hoá ; gây lỗi
            String uri = "/table/v1/driving/" + coordinates + "?annotations=duration,distance";

            OsrmTableResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(OsrmTableResponse.class);

            if (response != null && "Ok".equals(response.getCode())) {
                // ── SUCCESS → reset circuit ─────────────────────
                onSuccess();
                return response;
            } else {
                log.warn("OSRM trả về response không hợp lệ: code={}",
                        response != null ? response.getCode() : "null");
                onFailure();
                return buildHaversineFallback(locations);
            }

        } catch (Exception e) {
            log.error("Lỗi khi gọi OSRM: {} — dùng Haversine fallback", e.getMessage());
            onFailure();
            return buildHaversineFallback(locations);
        }
    }

    // ═════════════════════════════════════════════════════════════
    // CIRCUIT BREAKER LOGIC
    // ═════════════════════════════════════════════════════════════

    private synchronized CircuitState getCircuitState() {
        if (consecutiveFailures < failureThreshold) {
            return CircuitState.CLOSED;
        }
        // Circuit đã mở — kiểm tra timeout
        if (System.currentTimeMillis() - circuitOpenedAt >= resetTimeoutMs) {
            return CircuitState.HALF_OPEN;
        }
        return CircuitState.OPEN;
    }

    private synchronized void onSuccess() {
        if (consecutiveFailures > 0) {
            log.info("✅ OSRM phục hồi — đóng circuit (trước đó {} lỗi liên tiếp)",
                    consecutiveFailures);
        }
        consecutiveFailures = 0;
        circuitOpenedAt = 0;
    }

    private synchronized void onFailure() {
        consecutiveFailures++;
        if (consecutiveFailures >= failureThreshold && circuitOpenedAt == 0) {
            circuitOpenedAt = System.currentTimeMillis();
            log.error("🔴 Circuit OPENED — {} lỗi liên tiếp, chuyển Haversine fallback {}ms",
                    consecutiveFailures, resetTimeoutMs);
        }
    }

    // ═════════════════════════════════════════════════════════════
    // HAVERSINE FALLBACK
    // ═════════════════════════════════════════════════════════════
    // Khi OSRM không available, tạo ma trận khoảng cách ước lượng
    // bằng Haversine formula (đường chim bay × detour factor)
    // ═════════════════════════════════════════════════════════════

    private OsrmTableResponse buildHaversineFallback(List<Location> locations) {
        int n = locations.size();
        Double[][] distances = new Double[n][n];
        Double[][] durations = new Double[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    distances[i][j] = 0.0;
                    durations[i][j] = 0.0;
                } else {
                    double haversineMeters = haversine(
                            locations.get(i).getLatitude().doubleValue(),
                            locations.get(i).getLongitude().doubleValue(),
                            locations.get(j).getLatitude().doubleValue(),
                            locations.get(j).getLongitude().doubleValue()
                    );
                    // Nhân detour factor cho khoảng cách đường bộ thực tế
                    double roadDistance = haversineMeters * DETOUR_FACTOR;
                    double durationSeconds = roadDistance / ESTIMATED_SPEED_MPS;

                    distances[i][j] = roadDistance;
                    durations[i][j] = durationSeconds;
                }
            }
        }

        log.info("🗺️ Haversine fallback: tính {}×{} ma trận cho {} điểm", n, n, n);

        return OsrmTableResponse.builder()
                .code("Ok")
                .distances(distances)
                .durations(durations)
                .build();
    }

    /**
     * Haversine formula — khoảng cách giữa 2 điểm trên mặt cầu Trái Đất
     *
     * @return khoảng cách tính bằng METERS
     */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }
}
