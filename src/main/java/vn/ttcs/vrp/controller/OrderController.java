package vn.ttcs.vrp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.ttcs.vrp.dto.ApiResponse;
import vn.ttcs.vrp.dto.request.OrderRequest;
import vn.ttcs.vrp.dto.request.UpdateOrderRequest;
import vn.ttcs.vrp.dto.request.UpdateOrderStatus;
import vn.ttcs.vrp.dto.response.OrderResponse;
import vn.ttcs.vrp.enums.OrderStatus;
import vn.ttcs.vrp.service.OrderService;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j(topic = "ORDER-CONTROLLER")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request
    ) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Tạo đơn hàng thành công", response
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> findAllOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) OrderStatus status
    ) {
        Page<OrderResponse> pageResponse = orderService.findAllOrders(page, size, sortBy, sortDir, status);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(
                "Danh sách đơn hàng có filter + phân trang: ", pageResponse
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> findOrderById(
            @PathVariable Long id) {
        OrderResponse response = orderService.findOrderById(id);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(
                "Chi tiết đơn hàng: ", response
        ));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderRequest request
    ) {
        OrderResponse response = orderService.updateOrder(id, request);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(
                "Cập nhật đơn hàng thành công", response
        ));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatus orderStatus
    ) {
        OrderResponse response = orderService.updateOrderStatus(id, orderStatus);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(
                "Cập nhật trạng thái đơn hàng thành công", response
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(
                "Xóa đơn hàng thành công"));
    }
}
