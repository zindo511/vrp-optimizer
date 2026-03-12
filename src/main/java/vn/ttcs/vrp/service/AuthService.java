package vn.ttcs.vrp.service;

import vn.ttcs.vrp.dto.request.LogInRequest;
import vn.ttcs.vrp.dto.request.RefreshTokenRequest;
import vn.ttcs.vrp.dto.request.RegisterRequest;
import vn.ttcs.vrp.dto.response.AuthResponse;
import vn.ttcs.vrp.model.User;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LogInRequest request);

    boolean logout();

    AuthResponse refreshToken(RefreshTokenRequest request);
}
