package vn.ttcs.vrp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import vn.ttcs.vrp.model.Route;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {
}
