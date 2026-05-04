package vn.ttcs.vrp.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.ttcs.vrp.dto.response.OsrmTableResponse;
import vn.ttcs.vrp.model.DistanceMatrix;
import vn.ttcs.vrp.model.Location;
import vn.ttcs.vrp.repository.DistanceMatrixRepository;
import vn.ttcs.vrp.service.DistanceMatrixService;
import vn.ttcs.vrp.service.OsrmClientService;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Distance Matrix Service — quản lý ma trận khoảng cách giữa các điểm.
 *
 * ═══════════════════════════════════════════════════════════════════
 * ISSUE #8: INCREMENTAL UPDATE
 * ═══════════════════════════════════════════════════════════════════
 *
 * Vấn đề cũ: Khi thêm 1 location mới vào hệ thống có 200 locations,
 * cần tính 399 pairs mới (newLoc↔mỗi loc cũ). Nhưng code cũ gọi
 * OSRM cho TẤT CẢ 200×200 = 40,000 pairs → lãng phí 99%.
 *
 * Giải pháp: Xác định chính xác những pairs thiếu, rồi chỉ gọi OSRM
 * cho những locations liên quan đến pairs thiếu.
 *
 * Thuật toán:
 *   1. Query DB lấy tất cả existing pairs
 *   2. So sánh với full matrix → xác định missing pairs
 *   3. Từ missing pairs → xác định locations cần gọi OSRM
 *   4. Gọi OSRM CHỈ cho các locations liên quan
 *   5. Lưu CHỈ các pairs mới vào DB
 * ═══════════════════════════════════════════════════════════════════
 */
@Service
@RequiredArgsConstructor
@Slf4j(topic = "DISTANCE-MATRIX")
public class DistanceMatrixServiceImpl implements DistanceMatrixService {

    private final DistanceMatrixRepository distanceMatrixRepository;
    private final OsrmClientService osrmClientService;
    
    @Override
    public List<DistanceMatrix> getDistanceMatrix(List<Location> locations) {
        if (locations == null || locations.size() < 2) {
            return new ArrayList<>();
        }

        int totalExpectedPairs = locations.size() * locations.size();
        List<Long> locationIds = locations.stream().map(Location::getId).toList();

        // ── Bước 1: Lấy tất cả pairs đã có trong DB ────────────────────
        List<DistanceMatrix> existingPairs = distanceMatrixRepository.findAllByOriginAndDestination(locationIds);

        log.info("Distance matrix: đã có {} / {} pairs trong DB", existingPairs.size(), totalExpectedPairs);

        // Nếu đủ 100% → trả về ngay, không gọi OSRM
        if (existingPairs.size() >= totalExpectedPairs) {
            log.info("Ma trận khoảng cách đã đầy đủ — không cần gọi OSRM");
            return existingPairs;
        }

        // ── Bước 2: Xác định pairs thiếu ────────────────────────────────
        Set<String> existingKeys = existingPairs.stream()
                .map(d -> d.getOrigin().getId() + "-" + d.getDestination().getId())
                .collect(Collectors.toSet());

        // Tìm locations liên quan đến pairs thiếu
        Set<Long> locationIdsNeedingOsrm = new LinkedHashSet<>();
        int missingCount = 0;

        for (Location origin : locations) {
            for (Location dest : locations) {
                String key = origin.getId() + "-" + dest.getId();
                if (!existingKeys.contains(key)) {
                    locationIdsNeedingOsrm.add(origin.getId());
                    locationIdsNeedingOsrm.add(dest.getId());
                    missingCount++;
                }
            }
        }

        log.info("Thiếu {} pairs, liên quan đến {} locations (trong tổng {} locations)",
                missingCount, locationIdsNeedingOsrm.size(), locations.size());

        // ── Bước 3: Tạo danh sách locations cho OSRM call ──────────────
        // CHỈ gọi OSRM cho locations có pairs thiếu (thay vì tất cả)
        List<Location> osrmLocations;
        if (locationIdsNeedingOsrm.size() == locations.size()) {
            // Tất cả locations đều có pairs thiếu → gọi full matrix
            osrmLocations = locations;
            log.info("Tất cả locations đều thiếu pairs → gọi OSRM full matrix ({}x{})",
                    locations.size(), locations.size());
        } else {
            // Chỉ lấy locations liên quan đến pairs thiếu
            osrmLocations = locations.stream()
                    .filter(loc -> locationIdsNeedingOsrm.contains(loc.getId()))
                    .toList();
            log.info("Gọi OSRM incremental cho {} locations (tiết kiệm {} locations)",
                    osrmLocations.size(), locations.size() - osrmLocations.size());
        }

        // ── Bước 4: Gọi OSRM ───────────────────────────────────────────
        OsrmTableResponse osrmMatrix = osrmClientService.getDistanceMatrix(osrmLocations);
        if (osrmMatrix == null || osrmMatrix.getDistances() == null || osrmMatrix.getDurations() == null) {
            log.error("OSRM lỗi → trả về danh sách hiện có ({} pairs)", existingPairs.size());
            return existingPairs;
        }

        // ── Bước 5: Lưu CHỈ pairs mới ──────────────────────────────────
        List<DistanceMatrix> newlySavedPairs = new ArrayList<>();
        List<DistanceMatrix> finalFullMatrixList = new ArrayList<>(existingPairs);

        for (int i = 0; i < osrmLocations.size(); i++) {
            Location origin = osrmLocations.get(i);
            for (int j = 0; j < osrmLocations.size(); j++) {
                Location dest = osrmLocations.get(j);
                String cacheKey = origin.getId() + "-" + dest.getId();

                // Chỉ lưu pair CHƯA CÓ trong DB
                if (!existingKeys.contains(cacheKey)) {
                    Double dist = osrmMatrix.getDistances()[i][j];
                    Double duration = osrmMatrix.getDurations()[i][j];

                    DistanceMatrix newRecord = DistanceMatrix.builder()
                            .origin(origin)
                            .destination(dest)
                            .distanceMeters(BigDecimal.valueOf(dist != null ? dist : 0.0))
                            .durationSeconds(duration != null ? duration.longValue() : 0L)
                            .build();

                    newlySavedPairs.add(newRecord);
                    finalFullMatrixList.add(newRecord);
                    existingKeys.add(cacheKey); // Tránh duplicate trong cùng batch
                }
            }
        }

        // Đổ phần thiếu xuống DB
        if (!newlySavedPairs.isEmpty()) {
            distanceMatrixRepository.saveAll(newlySavedPairs);
            log.info("Lưu {} pairs mới vào DB (tiết kiệm {} OSRM calls so với full matrix)",
                    newlySavedPairs.size(),
                    totalExpectedPairs - existingPairs.size() - newlySavedPairs.size());
        }

        // Kiểm tra còn thiếu pairs nào không (edge case: OSRM incremental không cover hết)
        int stillMissing = totalExpectedPairs - finalFullMatrixList.size();
        if (stillMissing > 0) {
            log.warn("Vẫn còn thiếu {} pairs sau incremental update — có thể cần full OSRM call", stillMissing);
        }

        return finalFullMatrixList;
    }
}
