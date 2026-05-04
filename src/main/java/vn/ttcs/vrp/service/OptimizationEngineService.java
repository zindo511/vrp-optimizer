package vn.ttcs.vrp.service;

import vn.ttcs.vrp.dto.request.OptimizationRequest;
import vn.ttcs.vrp.dto.response.OptimizationJobResponse;
import vn.ttcs.vrp.model.OptimizationResult;

public interface OptimizationEngineService {

    /**
     * [LEGACY — đồng bộ] Chạy tối ưu hoá và chờ kết quả.
     * Vẫn giữ cho backward compatibility, nhưng không nên dùng từ controller.
     */
    OptimizationResult runOptimization(OptimizationRequest request);

    /**
     * [ASYNC] Submit job tối ưu hoá — trả về jobId ngay lập tức.
     * Job chạy ngầm trên thread pool riêng.
     */
    String submitOptimizationJob(OptimizationRequest request, String username);

    /**
     * Lấy trạng thái hiện tại của job.
     */
    OptimizationJobResponse getJobStatus(String jobId);

    /**
     * Reset dữ liệu test — chỉ khả dụng ở môi trường dev.
     */
    void resetTestingData();
}
