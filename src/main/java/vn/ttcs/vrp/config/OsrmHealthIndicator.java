package vn.ttcs.vrp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Custom Health Indicator cho OSRM routing service.
 *
 * ═══════════════════════════════════════════════════════════════════
 * Kiểm tra OSRM có hoạt động không bằng cách gọi /nearest endpoint.
 *
 * Kết quả hiện tại /actuator/health:
 * {
 *   "status": "UP",
 *   "components": {
 *     "osrm": {
 *       "status": "UP",
 *       "details": { "baseUrl": "https://router.project-osrm.org" }
 *     },
 *     "db": { "status": "UP" }
 *   }
 * }
 *
 * Nếu OSRM down → status = "DOWN" nhưng app vẫn chạy (dùng Haversine fallback).
 * → overall status vẫn là "UP" vì OSRM không critical (có fallback).
 * ═══════════════════════════════════════════════════════════════════
 */
@Component("osrm")
@Slf4j(topic = "OSRM-HEALTH")
public class OsrmHealthIndicator implements HealthIndicator {

    @Value("${osrm.base-url:https://router.project-osrm.org}")
    private String osrmBaseUrl;

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Override
    public Health health() {
        try {
            // Gọi /nearest với tọa độ HCM để kiểm tra OSRM phản hồi
            String testUrl = osrmBaseUrl + "/nearest/v1/driving/106.7009,10.7769?number=1";

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(TIMEOUT)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(testUrl))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return Health.up()
                        .withDetail("baseUrl", osrmBaseUrl)
                        .withDetail("responseTimeMs", "< " + TIMEOUT.toMillis())
                        .build();
            } else {
                return Health.down()
                        .withDetail("baseUrl", osrmBaseUrl)
                        .withDetail("httpStatus", response.statusCode())
                        .withDetail("fallback", "Haversine đang được sử dụng")
                        .build();
            }
        } catch (Exception e) {
            log.debug("OSRM health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("baseUrl", osrmBaseUrl)
                    .withDetail("error", e.getMessage())
                    .withDetail("fallback", "Haversine đang được sử dụng")
                    .build();
        }
    }
}
