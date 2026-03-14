package vn.ttcs.vrp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int code;
    private String message;
    private String timestamp;
    private T data;

    private Object error;
    private String path;

    // ---- SUCCESS TRẢ VỀ PAYLOAD ----
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(message)
                .timestamp(now())
                .data(data)
                .build();

    }

    // ---- SUCCESS KHÔNG CÓ PAYLOAD
    public static  <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(message)
                .timestamp(now())
                .build();
    }

    // ---- ERROR lỗi đơn giản
    public static  <T> ApiResponse<T> error(int code, String message, String path) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .timestamp(now())
                .path(path)
                .build();
    }

    // ---- ERROR lỗi kèm chi tiết bổ sung
    public static  <T> ApiResponse<T> error(int code, String message, Object error, String path) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .timestamp(now())
                .error(error)
                .path(path)
                .build();
    }

    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}

