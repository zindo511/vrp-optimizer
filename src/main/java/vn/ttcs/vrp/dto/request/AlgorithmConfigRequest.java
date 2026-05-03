package vn.ttcs.vrp.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class AlgorithmConfigRequest {

    @NotBlank(message = "Tên thuật toán không được trống")
    private String name;

    private Integer populationSize;
    private Integer generations;
    private BigDecimal mutationRate;
    private BigDecimal crossoverRate;
    private Integer elitismCount;
    private Boolean isActive;
    private String description;
}
