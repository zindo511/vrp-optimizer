package vn.ttcs.vrp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgorithmConfigResponse {

    private Long id;
    private String name;
    private Integer populationSize;
    private Integer generations;
    private BigDecimal mutationRate;
    private BigDecimal crossoverRate;
    private Integer elitismCount;
    private Boolean isActive;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
