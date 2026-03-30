package vn.ttcs.vrp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.ttcs.vrp.enums.OrderStatus;
import vn.ttcs.vrp.model.Order;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = { "location", "user" })
    Page<Order> findAll(Pageable pageable);

    @EntityGraph(attributePaths = { "location", "user" })
    Page<Order> findAllByStatus(OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = { "location", "user" })
    Optional<Order> findById(Long id);

    @EntityGraph(attributePaths = {"location"})
    List<Order> findAllByStatus(OrderStatus status);
}
