package vn.ttcs.vrp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.ttcs.vrp.model.RouteStop;

@Repository
public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {
}
