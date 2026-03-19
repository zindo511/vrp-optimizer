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
import vn.ttcs.vrp.dto.request.UpdateVehicleTypeRequest;
import vn.ttcs.vrp.dto.request.VehicleTypeRequest;
import vn.ttcs.vrp.dto.response.VehicleTypeResponse;
import vn.ttcs.vrp.service.VehicleTypeService;

@RestController
@RequestMapping("/api/vehicle-types")
@RequiredArgsConstructor
@Slf4j(topic = "VEHICLE-TYPE-CONTROLLER")
public class VehicleTypeController {

    private final VehicleTypeService vehicleTypeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<VehicleTypeResponse>> createVehicleType(
            @Valid @RequestBody VehicleTypeRequest request
    ){
        VehicleTypeResponse response = vehicleTypeService.createVehicleType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Tạo loại xe thành công", response
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<VehicleTypeResponse>>> findAllVehicleTypes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "maxVolumeM3") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ){
        Page<VehicleTypeResponse> page1 = vehicleTypeService.findAllVehicleTypes(page, size, sortBy, sortDir);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(
                "Lấy thành công các loại xe", page1
        ));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<VehicleTypeResponse>> updateVehicleType(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVehicleTypeRequest request
    ) {
        VehicleTypeResponse response = vehicleTypeService.updateVehicleType(id, request);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(
                "Cập nhật thành công loại xe", response
        ));
    }
}
