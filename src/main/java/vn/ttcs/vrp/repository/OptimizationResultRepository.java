package vn.ttcs.vrp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.ttcs.vrp.model.OptimizationResult;

@Repository
public interface OptimizationResultRepository extends JpaRepository<OptimizationResult, Long> {
}
