package vn.ttcs.vrp.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import vn.ttcs.vrp.dto.Coordinate;
import vn.ttcs.vrp.service.GeocodingService;

import java.util.List;
import java.util.Map;

@Service
@Slf4j(topic = "NOMINATIM-GECODING")
public class NominatimGeocodingServiceImpl implements GeocodingService {

    private final RestClient restClient;

    public NominatimGeocodingServiceImpl() {
        // cấu hình ResClinet
        this.restClient = RestClient.builder()
                .baseUrl("https://nominatim.openstreetmap.org/")
                .defaultHeader("User-Agent", "VrpOptimizerApp/1.0 (ttcs.ptit)")
                .build();
    }


    @Override
    public Coordinate geocode(String address) {

        log.info("Đang gọi OSM Nominatim API bằng ResClinet để lấy toạ độ cho: {}", address);

        // validate dữ liệu
        List<Map<String, Object>> result = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", address)
                        .queryParam("format", "json")
                        .queryParam("limit", 5)
                        .build()
                )
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (result != null && !result.isEmpty()) {
            Map<String, Object> firstResult = result.get(0);

            Double lat = Double.parseDouble(firstResult.get("lat").toString());
            Double lon = Double.parseDouble(firstResult.get("lon").toString());

            log.info("Geocoding thành công: [{}, {}]", lat, lon);
            return Coordinate.builder()
                    .latitude(lat)
                    .longitude(lon)
                    .build();
        }

        log.warn("Nominatim không tìm thấy toạ độ cho địa chỉ: {}", address);

        return null;
    }
}
