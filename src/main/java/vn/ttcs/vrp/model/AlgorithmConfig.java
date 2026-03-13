package vn.ttcs.vrp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "algorithm_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlgorithmConfig extends AuditableEntity{

    @Column(length = 100, nullable = false)
    private String name;

    @Column(name = "population_size")
    @Builder.Default
    private Integer populationSize = 100;

    @Builder.Default
    private Integer generations = 500;

    @Column(name = "mutation_rate", precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal mutationRate = BigDecimal.valueOf(0.05);

    @Column(name = "crossover_rate", precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal crossoverRate = BigDecimal.valueOf(0.80);

    @Column(name = "elitism_count")
    @Builder.Default
    private Integer elitismCount = 2;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = false;

    @Column(columnDefinition = "TEXT")
    private String description;
}
