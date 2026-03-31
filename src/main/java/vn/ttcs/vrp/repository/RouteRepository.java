package vn.ttcs.vrp.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.ttcs.vrp.model.Driver;
import vn.ttcs.vrp.model.Route;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    @EntityGraph(attributePaths = { "vehicle", "startDepot", "routeStops", "routeStops.order", "routeStops.location" })
    Optional<Route> findByDriverAndRouteDate(Driver driver, LocalDate routeDate);
}
