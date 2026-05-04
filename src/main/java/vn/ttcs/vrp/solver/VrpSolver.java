package vn.ttcs.vrp.solver;

import vn.ttcs.vrp.dto.vrp.VrpSolutionDto;
import vn.ttcs.vrp.model.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Strategy Interface - Khuôn mẫu chung cho mọi thuật toán VRP
 * Cần thêm thuật toán thì implement interface này
 */
public interface VrpSolver {

    VrpSolutionDto solve(
            List<Order> orders,
            List<Vehicle> vehicles,
            Location depotLocation,
            Map<String, DistanceMatrix> distanceMap,
            AlgorithmConfig config,
            LocalDate routeDate,
            LocalTime depotStartTime,
            LocalTime depotEndTime  // Issue #15: null = không giới hạn
    );
}
