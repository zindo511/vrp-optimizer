package vn.ttcs.vrp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Builder
@AllArgsConstructor
public class DepotResponse {

    private Long id;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
}
