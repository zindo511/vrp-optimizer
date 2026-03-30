package vn.ttcs.vrp.dto.vrp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.ttcs.vrp.model.Order;
import vn.ttcs.vrp.model.Vehicle;

import java.util.List;

/**
 * DTO tạm trong RAM - đại diện cho 1 tuyến đường xe đi
 * Không có bảng DB, chỉ tồn tại để lưu tuyến đường của 1 xe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlannedRouteDto {

    private Vehicle vehicle;

    // danh sách đơn hàng theo thứ tự giao
    private List<Order> orderStops;

    private double totalDistanceMeters;
    private long totalDurationSeconds;
    private double totalWeightKg;
}
