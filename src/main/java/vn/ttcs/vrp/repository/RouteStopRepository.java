package vn.ttcs.vrp.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.ttcs.vrp.model.RouteStop;

import java.util.Optional;

@Repository
public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {

    @EntityGraph(attributePaths = { "route", "route.driver", "order" })
    Optional<RouteStop> findById(Long id);
}
