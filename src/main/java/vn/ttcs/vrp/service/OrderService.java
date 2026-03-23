package vn.ttcs.vrp.service;

import org.springframework.data.domain.Page;
import vn.ttcs.vrp.dto.request.OrderRequest;
import vn.ttcs.vrp.dto.request.UpdateOrderRequest;
import vn.ttcs.vrp.dto.request.UpdateOrderStatus;
import vn.ttcs.vrp.dto.response.OrderResponse;
import vn.ttcs.vrp.enums.OrderStatus;

public interface OrderService {

    OrderResponse createOrder(OrderRequest request);

    Page<OrderResponse> findAllOrders(int page, int size, String sortBy, String sortDir, OrderStatus status);

    OrderResponse findOrderById(Long orderId);

    OrderResponse updateOrder(Long id, UpdateOrderRequest request);

    OrderResponse updateOrderStatus(Long id, UpdateOrderStatus status);

    void deleteOrder(Long id);
}
