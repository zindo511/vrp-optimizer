package vn.ttcs.vrp.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.ttcs.vrp.dto.request.LogInRequest;
import vn.ttcs.vrp.dto.request.RefreshTokenRequest;
import vn.ttcs.vrp.dto.request.RegisterRequest;
import vn.ttcs.vrp.dto.response.AuthResponse;
import vn.ttcs.vrp.model.RefreshToken;
import vn.ttcs.vrp.model.User;
import vn.ttcs.vrp.repository.UserRepository;
import vn.ttcs.vrp.security.JwtUtils;
import vn.ttcs.vrp.security.RefreshTokenService;
import vn.ttcs.vrp.security.UserDetailsImpl;
import vn.ttcs.vrp.service.AuthService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;

    @Override
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        User saved = userRepository.save(user);

        UserDetailsImpl userDetails = new UserDetailsImpl(saved);
        return AuthResponse.builder()
                .accessToken(jwtUtils.generateToken(userDetails))
                .refreshToken(refreshTokenService.createRefreshToken(saved.getId()).getToken())
                .build();
    }

    @Override
    public AuthResponse login(LogInRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (request.getEmail() == null || request.getPassword() == null) {
            throw new RuntimeException("Username or Password required");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        return AuthResponse.builder()
                .accessToken(jwtUtils.generateToken(userDetails))
                .refreshToken(refreshTokenService.createRefreshToken(user.getId()).getToken())
                .build();
    }

    @Override
    public void logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Cannot log out");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetailsImpl userDetails) {
            int deleted = refreshTokenService.deleteByUserId(userDetails.user().getId());
            if (deleted == 0)
                throw new RuntimeException("Cannot log out");
        }
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {

        RefreshToken refreshToken = refreshTokenService.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenService.deleteByUserId(refreshToken.getUser().getId());
            throw new RuntimeException("Token expired");
        }

        UserDetailsImpl userDetails = new UserDetailsImpl(refreshToken.getUser());
        return AuthResponse.builder()
                .accessToken(jwtUtils.generateToken(userDetails))
                .refreshToken(refreshToken.getToken())
                .build();
    }


}
