package vn.ttcs.vrp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.ttcs.vrp.enums.VehicleStatus;
import vn.ttcs.vrp.model.Vehicle;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    boolean existsByLicensePlate(String licensePlate);

    @EntityGraph(attributePaths = {"vehicleType"})
    Page<Vehicle> findVehicleByStatus(VehicleStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"vehicleType"})
    List<Vehicle> findAllByStatus(VehicleStatus status);
}
