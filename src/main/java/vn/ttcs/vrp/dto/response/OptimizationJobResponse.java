package vn.ttcs.vrp.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho optimization job — dùng cho cả lần submit đầu tiên
 * lẫn polling status.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OptimizationJobResponse {

    /**
     * ID duy nhất của job (UUID). Client dùng ID này để poll trạng thái.
     */
    private String jobId;

    /**
     * Trạng thái hiện tại: QUEUED, RUNNING, SUCCESS, FAILED
     */
    private String status;

    /**
     * Thông báo mô tả (tiến trình hoặc lỗi)
     */
    private String message;

    /**
     * Phần trăm hoàn thành (0-100), null nếu chưa có
     */
    private Integer progressPercent;

    /**
     * Thời gian đã chạy (ms)
     */
    private Long elapsedMs;

    /**
     * ID của OptimizationResult khi job hoàn thành thành công.
     * Client dùng ID này để fetch kết quả chi tiết.
     */
    private Long resultId;
}
