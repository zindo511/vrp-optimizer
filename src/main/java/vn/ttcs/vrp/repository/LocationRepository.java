package vn.ttcs.vrp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.ttcs.vrp.model.Location;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

}
