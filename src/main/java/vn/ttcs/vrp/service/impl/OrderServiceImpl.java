package vn.ttcs.vrp.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ttcs.vrp.dto.request.OrderRequest;
import vn.ttcs.vrp.dto.request.UpdateOrderRequest;
import vn.ttcs.vrp.dto.request.UpdateOrderStatus;
import vn.ttcs.vrp.dto.response.OrderResponse;
import vn.ttcs.vrp.enums.OrderStatus;
import vn.ttcs.vrp.exception.BadRequestException;
import vn.ttcs.vrp.exception.ResourceNotFoundException;
import vn.ttcs.vrp.mapper.OrderMapper;
import vn.ttcs.vrp.model.Location;
import vn.ttcs.vrp.model.Order;
import vn.ttcs.vrp.model.User;
import vn.ttcs.vrp.repository.LocationRepository;
import vn.ttcs.vrp.repository.OrderRepository;
import vn.ttcs.vrp.repository.UserRepository;
import vn.ttcs.vrp.security.UserDetailsImpl;
import vn.ttcs.vrp.service.OrderService;

import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final LocationRepository locationRepository;
    private final OrderMapper orderMapper;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl user)) {
            throw new BadRequestException("Không xác định được danh tính người dùng hiện tại");
        }
        return userRepository.findById(user.user().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    private void validateTimeWindow(LocalTime from, LocalTime to) {
        if (from != null && to != null) {
            if (!from.isBefore(to)) {
                throw new BadRequestException(
                        "Thời gian bắt đầu (from) phải trước thời gian kết thúc (to)"
                );
            }
        }
    }

    private Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id: " + orderId));
    }

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {

        validateTimeWindow(request.getTimeWindowFrom(), request.getTimeWindowTo());

        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Location không tìm thấy với id: " + request.getLocationId()));

        Order order = orderMapper.toOrder(request);
        order.setLocation(location); // set location

        //set user
        User user = getCurrentUser();
        order.setUser(user);

        if (order.getStatus() == null) {
            order.setStatus(OrderStatus.PENDING);
        }
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public Page<OrderResponse> findAllOrders(int page, int size, String sortBy, String sortDir, OrderStatus status) {
        // tạo đối tượng sort
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);

        // validate
        Pageable pageable = PageRequest.of(page >= 1 ? page - 1 : 0, size, sort);
        Page<Order> orders;

        if (status != null) {
            orders = orderRepository.findAllByStatus(status, pageable);
        } else {
            orders = orderRepository.findAll(pageable);
        }
        return orders.map(orderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse findOrderById(Long orderId) {
        Order order = getOrderById(orderId);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrder(Long id, UpdateOrderRequest request) {

        validateTimeWindow(request.getTimeWindowFrom(), request.getTimeWindowTo());

        Order order = getOrderById(id);
        if (order.getStatus() == OrderStatus.PENDING) {
            throw new BadRequestException("Đơn hàng này không thể cập nhật được: " + order.getId());
        }

        orderMapper.updateOrder(request, order);
        if (request.getLocationId() != null && !request.getLocationId().equals(order.getLocation().getId())) {
            Location location = locationRepository.findById(request.getLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kho hàng với id: " + request.getLocationId()));
            order.setLocation(location);
        }
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, UpdateOrderStatus status) {
        Order order = getOrderById(id);
        order.setStatus(status.getOrderStatus());
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        Order order = getOrderById(id);
        // Chỉ cho phép xóa khi đơn hàng còn ở trạng thái PENDING
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể xóa đơn hàng ở trạng thái PENDING");
        }
        orderRepository.delete(order);
    }

}
