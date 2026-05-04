package vn.ttcs.vrp.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.ttcs.vrp.dto.ApiResponse;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j(topic = "GLOBAL-EXCEPTION-HANDLER")
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        log.warn("Validation failed on {}: {}", request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        400, "Lỗi kiểm tra dữ liệu đầu vào", fieldErrors, request.getRequestURI()
                ));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequestException(
            BadRequestException ex, HttpServletRequest request
    ) {
        log.warn("Bad request on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(
                400, ex.getMessage(), request.getRequestURI()
        ));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResourceException(
            DuplicateResourceException ex, HttpServletRequest request
    ) {
        log.warn("Duplicate resource on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(
                409, ex.getMessage(), request.getRequestURI()
        ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request
    ) {
        log.warn("Authentication failed on {} from IP {}", request.getRequestURI(), request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(
                401, "Email hoặc mật khẩu bị sai", request.getRequestURI()
        ));
    }

    // ── 403 Forbidden — thiếu quyền truy cập ────────────────────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request
    ) {
        log.warn("Access denied on {} from IP {}", request.getRequestURI(), request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(
                403, "Bạn không có quyền truy cập tài nguyên này", request.getRequestURI()
        ));
    }

    // ── 404 Not Found — tài nguyên không được tìm thấy ──────────────
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request
    ) {
        log.warn("Resource not found on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(
                404, ex.getMessage(), request.getRequestURI()
        ));
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileStorageException(
            FileStorageException ex, HttpServletRequest request
    ) {
        log.error("File storage error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(
                500, ex.getMessage(), request.getRequestURI()
        ));
    }

    // ═══════════════════════════════════════════════════════════════════
    // 500 Internal Server Error — fallback cho TẤT CẢ exception chưa xử lý
    // ═══════════════════════════════════════════════════════════════════
    // CRITICAL: Log FULL STACK TRACE ở đây! Nếu không, khi production
    // có lỗi 500, team sẽ KHÔNG BIẾT lỗi ở đâu.
    //
    // Client chỉ nhận message chung chung (không leak stack trace).
    // Server log đầy đủ stack trace để debug.
    // ═══════════════════════════════════════════════════════════════════
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex, HttpServletRequest request) {
        log.error("⚠️ Unhandled exception on {} [{}]: {}",
                request.getRequestURI(), request.getMethod(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(
                500, "Lỗi hệ thống. Vui lòng thử lại sau.", request.getRequestURI()
        ));
    }
}
