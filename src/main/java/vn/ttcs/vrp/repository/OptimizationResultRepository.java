package vn.ttcs.vrp.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.ttcs.vrp.model.OptimizationResult;

import java.util.List;

@Repository
public interface OptimizationResultRepository extends JpaRepository<OptimizationResult, Long> {

    @EntityGraph(attributePaths = {"config", "user"})
    @Query("SELECT r FROM OptimizationResult r ORDER BY r.runDate DESC")
    List<OptimizationResult> findAllOrderByRunDateDesc();

    long countByStatus(String status);
}
