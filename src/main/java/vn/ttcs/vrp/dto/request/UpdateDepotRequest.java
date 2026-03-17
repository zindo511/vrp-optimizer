package vn.ttcs.vrp.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
public class UpdateDepotRequest {

    private Long locationId;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
}
