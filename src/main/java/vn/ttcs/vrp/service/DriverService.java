package vn.ttcs.vrp.service;

import org.springframework.data.domain.Page;
import vn.ttcs.vrp.dto.request.DriverRequest;
import vn.ttcs.vrp.dto.request.UpdateDriverRequest;
import vn.ttcs.vrp.dto.response.DriverResponse;
import vn.ttcs.vrp.enums.DriverStatus;

public interface DriverService {

    // tạo profile tài xế
    DriverResponse createDriver(DriverRequest request);

    // danh sách tài xế, kèm phân trang
    Page<DriverResponse> findAllDrivers(int page, int size, String sortBy, String sortDir, DriverStatus status);

    // chi tiết của tài xế
    DriverResponse findDriverById(Long id);

    // cập nhật tài xế
    DriverResponse updateDriverById(Long id, UpdateDriverRequest request);

    // cập nhật trạng thái tài xế
    DriverResponse updateDriverStatus(Long id, DriverStatus driverStatus);
}
