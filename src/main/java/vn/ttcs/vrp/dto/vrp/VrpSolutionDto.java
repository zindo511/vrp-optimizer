package vn.ttcs.vrp.dto.vrp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO tạm trong RAM - chứa toàn bộ kết quả của 1 lần chạy thuật toán
 * VrpSolver trả về cái này, Engine nhận vào và lưu DB
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VrpSolutionDto {

    private List<PlannedRouteDto> routes;

    private int unassignedOrderCount;

    private double totalDistanceMeters;
}
