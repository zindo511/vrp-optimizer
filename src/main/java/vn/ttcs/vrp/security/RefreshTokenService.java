package vn.ttcs.vrp.security;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.ttcs.vrp.model.RefreshToken;
import vn.ttcs.vrp.model.User;
import vn.ttcs.vrp.repository.RefreshTokenRepository;
import vn.ttcs.vrp.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService { // 4 hàm, tìm/tạo/kiểm tra/xoá

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;

    // Tìm kiếm Refresh Token dựa vào token
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    // TẠO MỚI HOẶC CẬP NHẬT REFRESH TOKEN CHO NGƯỜI DÙNG
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId).orElse(null);

        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElse(new RefreshToken());

        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());

        LocalDateTime expiryDate = LocalDateTime.now().plusNanos(jwtProperties.getJwtRefreshExpirationMs() * 1_000_000);
        refreshToken.setExpiryDate(expiryDate);
        return refreshTokenRepository.save(refreshToken);
    }

    // verify refresh token xem nó hết hạn chưa
    public boolean verifyExpiration(RefreshToken refreshToken) {
        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token expired");
        }
        return true;
    }

    // đăng xuất - logout khỏi thiết bị
    @Transactional
    public int deleteByUserId(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        return refreshTokenRepository.deleteByUser(user);
    }
}
