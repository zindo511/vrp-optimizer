package vn.ttcs.vrp.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import vn.ttcs.vrp.dto.response.OsrmTableResponse;
import vn.ttcs.vrp.model.Location;
import vn.ttcs.vrp.service.OsrmClientService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j(topic = "OSRM-CLIENT")
public class OsrmClientServiceImpl implements OsrmClientService {

    private final RestClient restClient;

    public OsrmClientServiceImpl() {
        this.restClient = RestClient.builder()
                .baseUrl("https://router.project-osrm.org")
                .defaultHeader("User-Agent", "VrpOptimizerApp/1.0 (ttcs.ptit)")
                .build();
    }


    @Override
    public OsrmTableResponse getDistanceMatrix(List<Location> locations) {
        if (locations == null || locations.size() < 2) {
            log.warn("Chưa đủ điểm để tính ma trận OSRM");
            return null;
        }

        // OSRM yêu câu Lon, Lat chứ ko phải Lat, Lon
        String coordinates = locations.stream()
                .map(loc -> loc.getLongitude().toString() + "," + loc.getLatitude().toString())
                .collect(Collectors.joining(";"));

        log.info("Coordinates sẵn sàng: {}", coordinates);

        try {
            // ko dùng builder param vì rescline mã hoá ; gây sập
            String uri = "/table/v1/driving/" + coordinates + "?annotations=duration,distance";

            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(OsrmTableResponse.class);
        } catch (Exception e) {
            log.error("Lỗi khi gọi osrm: {}", e.getMessage());
            return null;
        }

    }
}
