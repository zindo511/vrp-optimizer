package vn.ttcs.vrp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.ttcs.vrp.dto.ApiResponse;
import vn.ttcs.vrp.dto.request.UpdateVehicleRequest;
import vn.ttcs.vrp.dto.request.UpdateVehicleStatusRequest;
import vn.ttcs.vrp.dto.request.VehicleRequest;
import vn.ttcs.vrp.dto.response.VehicleResponse;
import vn.ttcs.vrp.enums.VehicleStatus;
import vn.ttcs.vrp.service.VehicleService;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@Slf4j(topic = "VEHICLE-CONTROLLER")
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<VehicleResponse>> createVehicle(
            @Valid @RequestBody VehicleRequest request
    ) {
        VehicleResponse response = vehicleService.createVehicle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Tạo thành công xe", response
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<VehicleResponse>>> listVehicleByStatus(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) VehicleStatus status
    ) {
        Page<VehicleResponse> page1 = vehicleService.listVehiclesByStatus(page, size, sortBy, sortDir, status);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(
                "Lấy thành công danh sách xe", page1
        ));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicle(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVehicleRequest request
    ) {
        VehicleResponse response = vehicleService.updateVehicle(id, request);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(
                "Cập nhật xe thành công", response
        ));
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicleStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVehicleStatusRequest statusRequest
    ) {
        VehicleResponse response = vehicleService.updateVehicleStatus(id, statusRequest.getVehicleStatus());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(
                "Cập nhật trạng thái xe thành công", response
        ));
    }
}
