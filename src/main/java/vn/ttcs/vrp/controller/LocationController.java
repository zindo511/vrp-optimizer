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
import vn.ttcs.vrp.dto.request.LocationRequest;
import vn.ttcs.vrp.dto.request.UpdateLocationRequest;
import vn.ttcs.vrp.dto.response.LocationResponse;
import vn.ttcs.vrp.service.LocationService;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
@Slf4j(topic = "LOCATION-CONTROLLER")
public class LocationController {

    private final LocationService locationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LocationResponse>> createLocation(
            @Valid @RequestBody LocationRequest locationRequest
    ) {
        LocationResponse response = locationService.createLocation(locationRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Location đã được tạo thành công", response
        ));
    }

    /**
     * Xem chi tiết location theo ID.
     * Tất cả user đã đăng nhập đều xem được.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LocationResponse>> getLocationById(@PathVariable Long id) {
        LocationResponse response = locationService.getLocationById(id);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(
                "Lấy thông tin location thành công", response
        ));
    }

    /**
     *
     * @param page Trang hiện tại
     * @param size Số lượng bản ghi mỗi trang (mặc định 20)
     * @param sortBy Trường sắp xếp (mặc định: createdAt)
     * @param sortDirection Chiều sắp xếp: asc | desc
     * @return Page<LocationResponse>
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<LocationResponse>>> getAllLocations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection
    ) {
        Page<LocationResponse> responses = locationService.findAllLocations(page, size, sortBy, sortDirection);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(
                "Lấy danh sách locations thành công", responses
        ));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LocationResponse>> updateLocation(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLocationRequest request
    ) {
        LocationResponse response = locationService.updateLocation(id, request);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(
                "Cập nhật location thành công",  response
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteLocation(@PathVariable Long id) {
        locationService.deleteLocation(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.success(
                "Xoá location thành công"
        ));
    }
}
