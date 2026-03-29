package vn.ttcs.vrp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class OsrmTableResponse { // hứng JSON gửi từ OSRM về

    private String code;
    private Double[][] distances;
    private Double[][] durations;
    private List<Waypoint> sources;
    private List<Waypoint> destinations;

    @Data
    public static class Waypoint { // chi tiết về điểm đó
        private String hint;
        private Double distance; // khoảng cách từ nó tới điểm OSRM chọn
        private String name; // tên đường
        private Double[] location;
    }
}
