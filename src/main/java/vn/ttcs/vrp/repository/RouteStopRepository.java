package vn.ttcs.vrp.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.ttcs.vrp.model.RouteStop;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {

    @EntityGraph(attributePaths = { "route", "route.driver", "order" })
    Optional<RouteStop> findById(Long id);

    /**
     * Đếm số stop theo status cho 1 driver cụ thể.
     * Dùng cho báo cáo hiệu suất tài xế.
     */
    @Query("SELECT COUNT(rs) FROM RouteStop rs WHERE rs.route.driver.id = :driverId AND rs.status = :status")
    long countByDriverIdAndStatus(@Param("driverId") Long driverId,
                                  @Param("status") vn.ttcs.vrp.enums.RouteStopStatus status);

    /**
     * Đếm tổng số stop cho 1 driver.
     */
    @Query("SELECT COUNT(rs) FROM RouteStop rs WHERE rs.route.driver.id = :driverId")
    long countByDriverId(@Param("driverId") Long driverId);

    /**
     * Aggregate stop counts per driver, grouped by status — 1 query cho TẤT CẢ drivers.
     * Trả về Object[]: [driverId (Long), status (RouteStopStatus), count (Long)]
     */
    @Query("SELECT rs.route.driver.id, rs.status, COUNT(rs) " +
           "FROM RouteStop rs " +
           "GROUP BY rs.route.driver.id, rs.status")
    List<Object[]> countStopsGroupedByDriverAndStatus();

    /**
     * Đếm tổng routes per driver — 1 query cho TẤT CẢ drivers.
     * Trả về Object[]: [driverId (Long), routeCount (Long)]
     */
    @Query("SELECT r.driver.id, COUNT(r) FROM Route r GROUP BY r.driver.id")
    List<Object[]> countRoutesGroupedByDriver();
}
