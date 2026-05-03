package vn.ttcs.vrp.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.ttcs.vrp.enums.RouteStatus;
import vn.ttcs.vrp.model.Driver;
import vn.ttcs.vrp.model.Route;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    @EntityGraph(attributePaths = { "vehicle", "startDepot", "routeStops", "routeStops.order", "routeStops.location" })
    Optional<Route> findByDriverAndRouteDate(Driver driver, LocalDate routeDate);

    Optional<Route> findById(Long id);

    @EntityGraph(attributePaths = { "driver", "routeStops", "vehicle", "vehicle.vehicleType",  "driver", "driver.user", "routeStops", "routeStops.order", "routeStops.location" })
    @Query("select r from Route r where r.id = :id")
    Optional<Route> findByIdWithDetails(Long id);

    @EntityGraph(attributePaths = { "driver", "routeStops", "vehicle", "vehicle.vehicleType",  "driver", "driver.user", "routeStops", "routeStops.order", "routeStops.location" })
    List<Route> findAllByRouteDateAndStatus(LocalDate routeDate, RouteStatus status);

    // Dùng khi không filter theo status (tất cả tuyến của ngày đó)
    @EntityGraph(attributePaths = { "driver", "routeStops", "vehicle", "vehicle.vehicleType", "driver.user", "routeStops.order", "routeStops.location" })
    List<Route> findAllByRouteDate(LocalDate routeDate);

    @Query("SELECT COUNT(r) FROM Route r WHERE r.driver.id = :driverId")
    long countByDriverId(@Param("driverId") Long driverId);
}
