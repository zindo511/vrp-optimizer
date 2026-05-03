package vn.ttcs.vrp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.ttcs.vrp.enums.DriverStatus;
import vn.ttcs.vrp.model.Driver;
import vn.ttcs.vrp.model.User;
import vn.ttcs.vrp.model.Vehicle;

import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    boolean existsByLicenseNumber(String licenseNumber);

    boolean existsByUser(User user);

    @EntityGraph(attributePaths = { "user", "vehicle" })
    Page<Driver> findAll(Pageable pageable);

    @EntityGraph(attributePaths = { "user", "vehicle" })
    Page<Driver> findByStatus(DriverStatus status, Pageable pageable);

    @EntityGraph(attributePaths = { "user", "vehicle" })
    Optional<Driver> findById(Long id);

    @EntityGraph(attributePaths = {"user"})
    Optional<Driver> findByUser(User user);

    long countByStatus(DriverStatus status);

    // Dùng trong OptimizationEngine: tìm driver lái xe X để gán vào Route
    Optional<Driver> findByVehicle(Vehicle vehicle);

    boolean existsByVehicle(Vehicle vehicle);
}
