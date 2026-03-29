package vn.ttcs.vrp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.ttcs.vrp.model.Location;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    Optional<Location> findByLatitudeAndLongitude(BigDecimal latitude, BigDecimal longitude);

}
