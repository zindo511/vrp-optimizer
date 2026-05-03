package vn.ttcs.vrp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.ttcs.vrp.dto.ApiResponse;
import vn.ttcs.vrp.dto.request.AlgorithmConfigRequest;
import vn.ttcs.vrp.dto.request.UpdateAlgorithmConfigRequest;
import vn.ttcs.vrp.dto.response.AlgorithmConfigResponse;
import vn.ttcs.vrp.service.AlgorithmConfigService;

import java.util.List;

@RestController
@RequestMapping("/api/algorithm-configs")
@RequiredArgsConstructor
@Slf4j(topic = "ALGORITHM-CONFIG-CONTROLLER")
public class AlgorithmConfigController {

    private final AlgorithmConfigService algorithmConfigService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<List<AlgorithmConfigResponse>>> findAll() {
        List<AlgorithmConfigResponse> data = algorithmConfigService.findAll();
        return ResponseEntity.ok(ApiResponse.success("Danh sách cấu hình thuật toán", data));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AlgorithmConfigResponse>> create(
            @Valid @RequestBody AlgorithmConfigRequest request) {
        AlgorithmConfigResponse response = algorithmConfigService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo cấu hình thuật toán thành công", response));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AlgorithmConfigResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAlgorithmConfigRequest request) {
        AlgorithmConfigResponse response = algorithmConfigService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật cấu hình thành công", response));
    }
}
