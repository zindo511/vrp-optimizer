package vn.ttcs.vrp.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.ttcs.vrp.dto.request.LocationRequest;
import vn.ttcs.vrp.dto.request.UpdateLocationRequest;
import vn.ttcs.vrp.dto.response.LocationResponse;

public interface LocationService {

    // tạo location: dispatcher, admin
    LocationResponse createLocation(LocationRequest request);

    // lấy chi tiết 1 location, ai đăng nhập đều xem được
    LocationResponse getLocationById(Long locationId);

    // danh sách kèm phân trang
    Page<LocationResponse> findAllLocations(int page, int size, String sortBy, String sortDirection);

    // cập nhật những field muốn cập nhật
    LocationResponse updateLocation(Long locationId, UpdateLocationRequest request);

    void deleteLocation(Long locationId);
}
