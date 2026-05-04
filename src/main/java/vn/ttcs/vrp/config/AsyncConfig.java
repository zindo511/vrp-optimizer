package vn.ttcs.vrp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Cấu hình Async cho các tác vụ nặng (tối ưu hoá, GA solver, ...).
 *
 * Tại sao cần thread pool riêng?
 *   → Tối ưu hoá VRP có thể mất 30s–vài phút. Nếu chạy trên HTTP thread pool
 *     của Tomcat (mặc định 200 threads), sẽ giữ thread quá lâu → exhaustion.
 *   → Thread pool riêng giới hạn số lượng job chạy đồng thời, tránh OOM.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "optimizationExecutor")
    public Executor optimizationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);       // tối đa 2 job tối ưu hoá chạy song song
        executor.setMaxPoolSize(4);        // burst lên 4 nếu cần
        executor.setQueueCapacity(10);     // hàng đợi tối đa 10 job
        executor.setThreadNamePrefix("vrp-opt-");
        executor.setRejectedExecutionHandler((r, e) -> {
            throw new RuntimeException("Hệ thống đang xử lý quá nhiều yêu cầu tối ưu hoá. Vui lòng thử lại sau.");
        });
        executor.initialize();
        return executor;
    }
}
