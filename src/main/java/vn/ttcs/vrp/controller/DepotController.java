package vn.ttcs.vrp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.ttcs.vrp.dto.ApiResponse;
import vn.ttcs.vrp.dto.request.DepotRequest;
import vn.ttcs.vrp.dto.request.UpdateDepotRequest;
import vn.ttcs.vrp.dto.response.DepotResponse;
import vn.ttcs.vrp.service.DepotService;

@RestController
@RequestMapping("/api/depots")
@RequiredArgsConstructor
public class DepotController {

    private final DepotService depotService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DepotResponse>> createDepot(
            @Valid @RequestBody DepotRequest request
    ) {
        DepotResponse response = depotService.createDepot(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Tạo thành công depot", response
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DepotResponse>> getDepotById(
            @PathVariable Long id
    ) {
        DepotResponse response = depotService.getDepotById(id);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(
                "Lấy thông tin kho hàng thành công",  response
        ));
    }

    /**
     *
     * Danh sách tất cả kho với phân trang
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<DepotResponse>>> getAllDepots(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Page<DepotResponse> responses = depotService.getAllDepots(page, size, sortBy, sortDir);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(
                "Lấy danh sách kho hàng thành công", responses
        ));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DepotResponse>> updateDepotById(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDepotRequest request
    ) {
        DepotResponse response = depotService.updateDepotById(id, request);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(
                "Cập nhật kho thành công", response
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDepotById(
            @PathVariable Long id
    ) {
        depotService.deleteDepotById(id);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Xoá kho thành công"));
    }

}
