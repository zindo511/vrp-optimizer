package vn.ttcs.vrp.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Filter — ngăn chặn API abuse bằng Token Bucket algorithm.
 *
 * ═══════════════════════════════════════════════════════════════════
 * TẠI SAO CẦN RATE LIMITING?
 * ═══════════════════════════════════════════════════════════════════
 *
 * Không có rate limiting, attacker có thể:
 *   1. DDoS bằng spam POST /api/optimization/run (CPU-intensive)
 *   2. Brute-force login qua POST /api/auth/login
 *   3. Scrape toàn bộ data qua GET endpoints
 *
 * Token Bucket algorithm:
 *   - Mỗi client (IP) có 1 bucket chứa N tokens
 *   - Mỗi request tiêu 1 token
 *   - Tokens được refill theo thời gian
 *   - Bucket đầy → request được xử lý
 *   - Bucket rỗng → HTTP 429 Too Many Requests
 *
 * ═══════════════════════════════════════════════════════════════════
 * CẤU HÌNH RATE LIMITS
 * ═══════════════════════════════════════════════════════════════════
 *
 *   Endpoint             | Limit         | Lý do
 *   ---------------------|---------------|---------------------------
 *   /api/auth/login      | 5 req/min     | Chống brute-force
 *   /api/optimization/*  | 3 req/min     | CPU-intensive
 *   /api/** (mặc định)   | 60 req/min    | Chống scraping
 *   /actuator/**         | Không limit   | Health check cần luôn OK
 * ═══════════════════════════════════════════════════════════════════
 */
@Component
@Slf4j(topic = "RATE-LIMITER")
public class RateLimitFilter extends OncePerRequestFilter {

    /**
     * Cache buckets theo clientKey (IP + endpoint category).
     * ConcurrentHashMap thread-safe, tự dọn khi bucket hết hạn.
     */
    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    // ── Rate limit configurations ──────────────────────────────────
    private static final int AUTH_LIMIT     = 5;    // 5 req/phút
    private static final int OPT_LIMIT      = 3;    // 3 req/phút
    private static final int DEFAULT_LIMIT  = 60;   // 60 req/phút

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Không rate-limit actuator, swagger, và static resources
        if (path.startsWith("/actuator") || path.startsWith("/swagger") || path.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Xác định rate limit dựa trên endpoint
        String clientIp = getClientIp(request);
        String bucketKey;
        int limit;

        if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register")) {
            bucketKey = clientIp + ":auth";
            limit = AUTH_LIMIT;
        } else if (path.startsWith("/api/optimization")) {
            bucketKey = clientIp + ":optimization";
            limit = OPT_LIMIT;
        } else {
            bucketKey = clientIp + ":default";
            limit = DEFAULT_LIMIT;
        }

        Bucket bucket = bucketCache.computeIfAbsent(bucketKey, k -> createBucket(limit));

        if (bucket.tryConsume(1)) {
            // Token available → xử lý request
            filterChain.doFilter(request, response);
        } else {
            // Bucket rỗng → từ chối
            log.warn("🚫 Rate limit exceeded: IP={}, path={}, bucket={}",
                    clientIp, path, bucketKey);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("""
                    {
                        "code": 429,
                        "message": "Quá nhiều request. Vui lòng thử lại sau 1 phút.",
                        "path": "%s"
                    }
                    """.formatted(path));
        }
    }

    /**
     * Tạo Bucket với capacity = limit, refill limit tokens mỗi phút.
     */
    private Bucket createBucket(int limit) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(limit)
                        .refillGreedy(limit, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    /**
     * Lấy IP thực của client, xử lý trường hợp đi qua reverse proxy.
     * Priority: X-Forwarded-For > X-Real-IP > RemoteAddr
     */
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // X-Forwarded-For có thể chứa nhiều IP: client, proxy1, proxy2
            // IP đầu tiên là client thực
            return xff.split(",")[0].trim();
        }

        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) {
            return xri.trim();
        }

        return request.getRemoteAddr();
    }
}
