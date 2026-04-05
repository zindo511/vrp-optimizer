package vn.ttcs.vrp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.ttcs.vrp.dto.ApiResponse;
import vn.ttcs.vrp.dto.request.UpdateRouteStatusRequest;
import vn.ttcs.vrp.dto.response.RouteResponse;
import vn.ttcs.vrp.enums.RouteStatus;
import vn.ttcs.vrp.service.RouteService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;

    @PutMapping("/{routeId}/assign-driver/{driverId}")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> assignDriverToRoute(
            @PathVariable Long routeId,
            @PathVariable Long driverId
    ) {
        routeService.assignDriverToRoute(routeId, driverId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(
                "Gán thành công tài xế cho tuyến đường"
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<RouteResponse>> getRouteById(@PathVariable Long id) {
        RouteResponse response = routeService.getRouteById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin tuyến đường thành công", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<RouteResponse>>> getRouteByDate(
            @RequestParam LocalDate date,
            @RequestParam(required = false) RouteStatus status) {
        List<RouteResponse> response = routeService.getRouteByDate(date, status);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin tuyến thành công", response));
    }

    @PatchMapping("/{routeId}/status")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<RouteResponse>> updateRouteByStatus(
            @PathVariable Long routeId,
            @Valid @RequestBody UpdateRouteStatusRequest status
    ) {
        RouteResponse response = routeService.updateRouteByStatus(routeId, status.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái tuyến đường thành công", response));
    }
}
