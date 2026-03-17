package com.seodashboard.api.auth.service;

import com.seodashboard.api.auth.dto.LoginRequest;
import com.seodashboard.api.auth.dto.SignupRequest;
import com.seodashboard.api.auth.dto.TokenResponse;
import com.seodashboard.api.auth.dto.UserResponse;
import com.seodashboard.api.auth.jwt.JwtTokenProvider;
import com.seodashboard.api.auth.repository.UserRepository;
import com.seodashboard.common.domain.User;
import com.seodashboard.common.domain.enums.UserRole;
import com.seodashboard.common.exception.BusinessException;
import com.seodashboard.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String USED_REFRESH_TOKEN_PREFIX = "used_refresh_token:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public TokenResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Email already exists");
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name())
                .role(UserRole.USER)
                .build();

        user = userRepository.save(user);
        log.info("User registered: {}", user.getEmail());

        return generateTokenResponse(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid email or password");
        }

        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Account is deactivated");
        }

        user.updateLastLoginAt();
        log.info("User logged in: {}", user.getEmail());

        return generateTokenResponse(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Invalid refresh token");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        // Replay attack detection: if this token was already used and rotated out,
        // someone is reusing a stolen token. Invalidate all tokens for the user.
        String usedTokenKey = USED_REFRESH_TOKEN_PREFIX + refreshToken;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(usedTokenKey))) {
            log.warn("Refresh token replay attack detected for userId={}. Invalidating all tokens.", userId);
            redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Refresh token reuse detected. All sessions invalidated.");
        }

        String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Refresh token not found or mismatched");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "User not found"));

        // Mark the old refresh token as used before generating new tokens.
        // Keep it for the duration of the old token's remaining TTL so replay can be detected.
        Long remainingTtl = redisTemplate.getExpire(REFRESH_TOKEN_PREFIX + userId, TimeUnit.MILLISECONDS);
        long ttlMs = (remainingTtl != null && remainingTtl > 0)
                ? remainingTtl
                : jwtTokenProvider.getRefreshTokenExpiration();

        // Delete the old refresh token
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);

        // Store old token in the used-token set for replay detection
        redisTemplate.opsForValue().set(usedTokenKey, userId.toString(), ttlMs, TimeUnit.MILLISECONDS);

        // Generate and store new token pair
        return generateTokenResponse(user);
    }

    public void logout(Long userId) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
        log.info("User logged out: userId={}", userId);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "User not found"));
        return UserResponse.from(user);
    }

    private TokenResponse generateTokenResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name()
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getId(),
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpiration(),
                TimeUnit.MILLISECONDS
        );

        return new TokenResponse(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpiration() / 1000,
                UserResponse.from(user)
        );
    }
}
