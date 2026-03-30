package vn.ttcs.vrp.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class OptimizationRequest {

    @NotNull(message = "depotId không được để trống")
    private Long depotId;

    @NotNull(message = "routeDate không được để trống")
    private LocalDate routeDate;

    @NotNull(message = "algorithmConfigId không được để trống")
    private Long algorithmConfigId;
}
