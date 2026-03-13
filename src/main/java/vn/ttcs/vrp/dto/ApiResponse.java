package vn.ttcs.vrp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int code;
    private String message;
    private LocalDateTime timestamp;
    private T data;

    private Object error;
    private String path;

    // ---- SUCCESS TRẢ VỀ PAYLOAD ----
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(message)
                .timestamp(LocalDateTime.now())
                .data(data)
                .build();

    }

    // ---- SUCCESS KHÔNG CÓ PAYLOAD
    public static  <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

