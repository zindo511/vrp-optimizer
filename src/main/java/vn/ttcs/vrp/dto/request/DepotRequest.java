package vn.ttcs.vrp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
public class DepotRequest {

    @NotNull(message = "LocationId không được để trống")
    private Long locationId;

    @NotBlank(message = "Tên kho hàng không được để trống")
    private String name;

    private LocalTime startTime;
    private LocalTime endTime;
}
