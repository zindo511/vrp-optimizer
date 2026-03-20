package vn.ttcs.vrp.service;

import org.springframework.data.domain.Page;
import vn.ttcs.vrp.dto.request.UpdateVehicleRequest;
import vn.ttcs.vrp.dto.request.VehicleRequest;
import vn.ttcs.vrp.dto.response.VehicleResponse;
import vn.ttcs.vrp.enums.VehicleStatus;

public interface VehicleService {

    // thêm xe mới
    VehicleResponse createVehicle(VehicleRequest request);

    // Lọc xe theo trang thái (lọc theo status)
    Page<VehicleResponse> listVehiclesByStatus(int page, int size, String sortBy,
                              String sortDirection, VehicleStatus status);

    // cập nhật xe theo id của xe
    VehicleResponse updateVehicle(Long id, UpdateVehicleRequest request);

    // cập nhật trạng thái của xe
    VehicleResponse updateVehicleStatus(Long id, VehicleStatus status);
}
