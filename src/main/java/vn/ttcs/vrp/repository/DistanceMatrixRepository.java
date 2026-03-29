package vn.ttcs.vrp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.ttcs.vrp.model.DistanceMatrix;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DistanceMatrixRepository extends JpaRepository<DistanceMatrix, Long> {

    @Query("select d from DistanceMatrix d " +
            "where d.origin in :locationIds " +
            "and d.destination in :locationIds")
    List<DistanceMatrix> findAllByOriginAndDestination(List<Long> locationIds);

    @Modifying
    @Query("delete from DistanceMatrix d where d.calculatedAt < :expirationLimit")
    int deleteOrderThan(LocalDateTime expirationLimit);
}
