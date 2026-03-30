package vn.ttcs.vrp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.ttcs.vrp.model.AlgorithmConfig;

import java.util.Optional;

@Repository
public interface AlgorithmConfigRepository extends JpaRepository<AlgorithmConfig, Long> {

    Optional<AlgorithmConfig> findByIsActiveTrue();
}
