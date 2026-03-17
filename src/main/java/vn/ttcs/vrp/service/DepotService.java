package vn.ttcs.vrp.service;

import org.springframework.data.domain.Page;
import vn.ttcs.vrp.dto.request.DepotRequest;
import vn.ttcs.vrp.dto.request.UpdateDepotRequest;
import vn.ttcs.vrp.dto.response.DepotResponse;

public interface DepotService {

    // tạo depot (admin)
    DepotResponse createDepot(DepotRequest request);

    // lấy depot theo id
    DepotResponse getDepotById(Long id);

    // danh sách tất cả kho với phân trang
    Page<DepotResponse> getAllDepots(int page, int size, String sortBy, String sortDir);

    // cập nhật 1 phần thông tin của kho
    DepotResponse updateDepotById(Long id, UpdateDepotRequest request);

    // xoá kho
    void deleteDepotById(Long id);

}
