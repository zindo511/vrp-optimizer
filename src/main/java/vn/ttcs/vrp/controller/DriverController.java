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
import vn.ttcs.vrp.dto.request.DriverRequest;
import vn.ttcs.vrp.dto.request.StopStatusRequest;
import vn.ttcs.vrp.dto.request.UpdateDriverRequest;
import vn.ttcs.vrp.dto.request.UpdateDriverStatusRequest;
import vn.ttcs.vrp.dto.response.DriverResponse;
import vn.ttcs.vrp.dto.response.MyRouteResponse;
import vn.ttcs.vrp.enums.DriverStatus;
import vn.ttcs.vrp.service.DriverOperationService;
import vn.ttcs.vrp.service.DriverService;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@Slf4j(topic = "DRIVER-CONTROLLER")
public class DriverController {

    private final DriverService driverService;
    private final DriverOperationService driverOperationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DriverResponse>> createDriver(
            @Valid @RequestBody DriverRequest driverRequest
    ) {
        DriverResponse response = driverService.createDriver(driverRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Tạo thành công driver", response
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DriverResponse>>> findAllDrivers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam("id") String sortBy,
            @RequestParam("asc") String sortDir,
            @RequestParam(required = false) DriverStatus status
    ) {
        Page<DriverResponse> driverResponses = driverService.findAllDrivers(page, size, sortBy, sortDir, status);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(
                "Lấy thành công danh sách tài xế", driverResponses
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DriverResponse>> findDriverById(
            @PathVariable Long id
    ){
        DriverResponse response = driverService.findDriverById(id);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(
                "Lấy chi tiết thành công tài xế", response
        ));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DriverResponse>> updateDriverById(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDriverRequest request
    ) {
        DriverResponse response = driverService.updateDriverById(id, request);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(
                "Cập nhật thành công", response
        ));
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DriverResponse>> updateDriverStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDriverStatusRequest request
    ) {
        DriverResponse response = driverService.updateDriverStatus(id, request.getDriverStatus());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(
                "Cập nhật trạng thái tài xế thành công", response
        ));
    }

    // ===== DRIVER APP APIS -  TÀI XẾ ĐĂNG NHẬP THÌ MỚI CÓ

    // API: Tải lộ trình của ngày hôm nay
    @GetMapping("/my-route")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<MyRouteResponse>> getMyTodayRoutes() {
        MyRouteResponse routeResponse = driverOperationService.getMyTodayRoutes();
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(
                "Lộ trình hôm nay của bạn", routeResponse
        ));
    }

    // API: Cập nhật trạng thái giao hàng (Giao xong/Thất bại)
    @PutMapping("/stops/{stopId}/status")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<Void>> updateStopStatus(
            @PathVariable Long stopId,
            @Valid @RequestBody StopStatusRequest request
    ) {
        driverOperationService.updateStopStatus(stopId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công"));
    }

    //API: cập nhật trạng thái sau nâng cấp sử dụng web socket
}
