package vn.ttcs.vrp.dto.response;

import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class LocationResponse {

    private Long id;
    private String address;
    private Double latitude;
    private Double longitude;
}
