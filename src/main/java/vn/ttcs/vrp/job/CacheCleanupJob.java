package vn.ttcs.vrp.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.ttcs.vrp.repository.DistanceMatrixRepository;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "CACHE-CLEANUP-JOB")
public class CacheCleanupJob {

    private final DistanceMatrixRepository distanceMatrixRepository;

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldDistanceMatrix() {
        log.info("Bắt đầu dọn dẹp ma trận khoảng cách");

        LocalDateTime expirationLimit = LocalDateTime.now().minusDays(30);
        int deletedCount = distanceMatrixRepository.deleteOrderThan(expirationLimit);
        log.info("Đã xoá {} khoảng cách cũ", deletedCount);
    }
}
