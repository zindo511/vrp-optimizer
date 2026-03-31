package vn.ttcs.vrp.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.ttcs.vrp.dto.ApiResponse;
import vn.ttcs.vrp.service.RouteService;

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
        routeService.assginDriverToRoute(routeId, driverId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(
                "Gán thành công tài xế cho tuyến đường"
        ));
    }

}
