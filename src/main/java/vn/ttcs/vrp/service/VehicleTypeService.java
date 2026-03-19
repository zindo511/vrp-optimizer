package vn.ttcs.vrp.service;

import org.springframework.data.domain.Page;
import vn.ttcs.vrp.dto.request.UpdateVehicleTypeRequest;
import vn.ttcs.vrp.dto.request.VehicleTypeRequest;
import vn.ttcs.vrp.dto.response.VehicleTypeResponse;

public interface VehicleTypeService {

    // tạo loại xe
    VehicleTypeResponse createVehicleType(VehicleTypeRequest request);

    // Danh sách loại xe, sử dụng phân trang
    Page<VehicleTypeResponse> findAllVehicleTypes(int page, int size, String sortBy, String sortDir);

    // Cập nhật loại xe theo id
    VehicleTypeResponse updateVehicleType(Long id, UpdateVehicleTypeRequest request);
}
