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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        List<DistanceMatrix> existingPairs = distanceMatrixRepository.findAllByOriginAndDestination(locationIds);

        log.info("getDistanceMatrix đã tìm thấy {} / {} pairs", existingPairs.size(), totalExpectedPairs);

        // nếu đủ 100% rồi -> trả về kết quả ngay ko gọi OSRM nữa
        if (existingPairs.size() == totalExpectedPairs) {
            log.info("khoảng cách giữa các cặp điểm đã có sẵn trong database rồi");
            return existingPairs;
        }

        // nếu thiếu, dùng OSRM đi lấy
        log.info("Đang thiếu {} khoảng cách, gọi OSRM", totalExpectedPairs - existingPairs.size());

        OsrmTableResponse osrmMatrix = osrmClientService.getDistanceMatrix(locations);
        if (osrmMatrix == null || osrmMatrix.getDistances() == null || osrmMatrix.getDurations() == null) {
            log.error("OSRM lỗi -> Trả về danh sách thiếu Data");
            return existingPairs;
        }

        // Biến đổi từ list -> HashMap tìm kiếm cho nhanh
        Map<String, DistanceMatrix> cacheMap = existingPairs.stream()
                .collect(Collectors.toMap(
                        d -> d.getOrigin().getId() + "-" + d.getDestination().getId(),
                        d -> d
                ));

        List<DistanceMatrix> newlySavedPairs = new ArrayList<>();
        List<DistanceMatrix> finalFullMatrixList = new ArrayList<>(existingPairs);

        // Bóc tách bảng mảng 2 chiều của OSRM trả về
        for (int i = 0; i < locations.size(); i++) {
            Location orgin = locations.get(i);
            for (int j = 0; j < locations.size(); j++) {
                Location dest = locations.get(j);
                String cacheKey = orgin.getId() + "-" + dest.getId();

                // Nếu cặp origin-dest chưa có trong bộ nhớ DB
                if (!cacheMap.containsKey(cacheKey)) {
                    Double dist = osrmMatrix.getDistances()[i][j];
                    Double duration = osrmMatrix.getDurations()[i][j];

                    DistanceMatrix newRecord = DistanceMatrix.builder()
                            .origin(orgin)
                            .destination(dest)
                            .distanceMeters(BigDecimal.valueOf(dist != null ? dist : 0.0))
                            .durationSeconds(duration != null ? duration.longValue() : 0L)
                            .build();

                    newlySavedPairs.add(newRecord);
                    finalFullMatrixList.add(newRecord);
                }
            }
        }

        // đổ phần thiếu xuống DB
        if (!newlySavedPairs.isEmpty()) {
            distanceMatrixRepository.saveAll(newlySavedPairs);
            log.info("Lưu thành công {} khoảng cách mới vào DB", newlySavedPairs.size());
        }
        return finalFullMatrixList;
    }
}
