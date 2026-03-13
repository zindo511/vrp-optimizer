package vn.ttcs.vrp.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.ttcs.vrp.dto.ApiResponse;
import vn.ttcs.vrp.dto.request.LogInRequest;
import vn.ttcs.vrp.dto.request.RefreshTokenRequest;
import vn.ttcs.vrp.dto.request.RegisterRequest;
import vn.ttcs.vrp.dto.response.AuthResponse;
import vn.ttcs.vrp.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ){
        AuthResponse authResponse = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng ký thành công", authResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LogInRequest request){
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", authResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request){
        boolean result = authService.logout();
        if(result){
            return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công. Xoá vĩnh viễn refresh token"));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(
                            400,
                            "Đăng xuất thất bại",
                            request.getRequestURI()
                    ));
        }
    }

    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ){
        AuthResponse authResponse = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Làm mới token thành công", authResponse));
    }
}
