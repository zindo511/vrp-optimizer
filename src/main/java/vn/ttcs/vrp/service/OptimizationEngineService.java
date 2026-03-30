package vn.ttcs.vrp.service;

import vn.ttcs.vrp.dto.request.OptimizationRequest;
import vn.ttcs.vrp.model.OptimizationResult;

public interface OptimizationEngineService {
    OptimizationResult runOptimization(OptimizationRequest request);
}
