package vn.ttcs.vrp.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
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
    @Transactional
    public AuthResponse login(LogInRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        RefreshToken refreshToken = null;
        if (userDetails != null) {
            refreshToken = refreshTokenService.createRefreshToken(userDetails.user().getId());
        }
        return AuthResponse.builder()
                .accessToken(jwtUtils.generateToken(userDetails))
                .refreshToken(Objects.requireNonNull(refreshToken).getToken())
                .build();
    }

    @Override
    @Transactional
    public boolean logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetailsImpl userDetails) {
            int deleted = refreshTokenService.deleteByUserId(userDetails.user().getId());
            SecurityContextHolder.clearContext();
            return deleted > 0;
        }
        SecurityContextHolder.clearContext();
        return false;
    }

    @Override
    @Transactional
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
