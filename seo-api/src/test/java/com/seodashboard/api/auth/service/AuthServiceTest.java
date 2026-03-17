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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    // ── Redis key prefixes (must mirror AuthService constants) ───────────
    private static final String REFRESH_TOKEN_PREFIX   = "refresh_token:";
    private static final String USED_REFRESH_TOKEN_PREFIX = "used_refresh_token:";

    // ── Shared test fixtures ─────────────────────────────────────────────
    private static final Long   USER_ID        = 1L;
    private static final String USER_EMAIL     = "test@example.com";
    private static final String USER_NAME      = "Test User";
    private static final String RAW_PASSWORD   = "Password1!";
    private static final String HASHED_PASSWORD = "$2a$10$hashed";
    private static final String ACCESS_TOKEN   = "access.token.value";
    private static final String REFRESH_TOKEN  = "refresh.token.value";
    private static final long   ACCESS_EXP_MS  = 3_600_000L;   // 1 hour
    private static final long   REFRESH_EXP_MS = 86_400_000L;  // 24 hours

    @Mock private UserRepository      userRepository;
    @Mock private PasswordEncoder     passwordEncoder;
    @Mock private JwtTokenProvider    jwtTokenProvider;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    // ── Helper: build a User instance with a known id ────────────────────

    /**
     * Creates a User via the builder (isActive defaults to true) and injects
     * the given id into BaseEntity's private field via reflection, since the
     * field has no public setter (JPA normally sets it).
     */
    private User buildActiveUser() {
        User user = User.builder()
                .email(USER_EMAIL)
                .passwordHash(HASHED_PASSWORD)
                .name(USER_NAME)
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    /**
     * Stubs all JwtTokenProvider calls and the Redis opsForValue().set()
     * that generateTokenResponse() always invokes.
     * Call this in every test that expects a successful token pair to be
     * returned; also call it in tests where refresh() succeeds after rotation,
     * since generateTokenResponse is called at the end of that path too.
     */
    private void stubTokenGeneration() {
        when(jwtTokenProvider.generateAccessToken(eq(USER_ID), eq(USER_EMAIL), anyString()))
                .thenReturn(ACCESS_TOKEN);
        when(jwtTokenProvider.generateRefreshToken(USER_ID))
                .thenReturn(REFRESH_TOKEN);
        when(jwtTokenProvider.getRefreshTokenExpiration())
                .thenReturn(REFRESH_EXP_MS);
        when(jwtTokenProvider.getAccessTokenExpiration())
                .thenReturn(ACCESS_EXP_MS);
        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);
    }

    // ================================================================== //
    //  signup()                                                            //
    // ================================================================== //

    @Nested
    @DisplayName("signup()")
    class Signup {

        private SignupRequest request;

        @BeforeEach
        void setUp() {
            request = new SignupRequest(USER_EMAIL, RAW_PASSWORD, USER_NAME);
        }

        @Test
        @DisplayName("should return a TokenResponse containing both tokens when email is new")
        void signup_success() {
            when(userRepository.existsByEmail(USER_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);

            User savedUser = buildActiveUser();
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            stubTokenGeneration();

            TokenResponse response = authService.signup(request);

            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.user().email()).isEqualTo(USER_EMAIL);
        }

        @Test
        @DisplayName("should persist the new user exactly once with encoded password")
        void signup_savesUserWithEncodedPassword() {
            when(userRepository.existsByEmail(USER_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);

            User savedUser = buildActiveUser();
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            stubTokenGeneration();

            authService.signup(request);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).isEqualTo(HASHED_PASSWORD);
        }

        @Test
        @DisplayName("should throw BusinessException(VALIDATION_ERROR) when email already exists")
        void signup_duplicateEmail_throwsBusinessException() {
            when(userRepository.existsByEmail(USER_EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> authService.signup(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).getErrorCode())
                                    .isEqualTo(ErrorCode.VALIDATION_ERROR));

            verify(userRepository, never()).save(any());
        }
    }

    // ================================================================== //
    //  login()                                                             //
    // ================================================================== //

    @Nested
    @DisplayName("login()")
    class Login {

        private LoginRequest request;

        @BeforeEach
        void setUp() {
            request = new LoginRequest(USER_EMAIL, RAW_PASSWORD);
        }

        @Test
        @DisplayName("should return a TokenResponse when credentials are correct")
        void login_success() {
            User user = buildActiveUser();
            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
            stubTokenGeneration();

            TokenResponse response = authService.login(request);

            assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
            assertThat(response.user().email()).isEqualTo(USER_EMAIL);
        }

        @Test
        @DisplayName("should update lastLoginAt on successful login")
        void login_success_updatesLastLoginAt() {
            User user = buildActiveUser();
            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
            stubTokenGeneration();

            authService.login(request);

            // lastLoginAt is set inside updateLastLoginAt(); verify it is now non-null
            assertThat(user.getLastLoginAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw BusinessException(UNAUTHORIZED) when email is not found")
        void login_emailNotFound_throwsBusinessException() {
            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).getErrorCode())
                                    .isEqualTo(ErrorCode.UNAUTHORIZED));
        }

        @Test
        @DisplayName("should throw BusinessException(UNAUTHORIZED) when password is wrong")
        void login_wrongPassword_throwsBusinessException() {
            User user = buildActiveUser();
            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).getErrorCode())
                                    .isEqualTo(ErrorCode.UNAUTHORIZED));
        }

        @Test
        @DisplayName("should throw BusinessException(ACCESS_DENIED) when account is deactivated")
        void login_inactiveUser_throwsBusinessException() {
            User user = buildActiveUser();
            user.deactivate(); // sets isActive = false
            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).getErrorCode())
                                    .isEqualTo(ErrorCode.ACCESS_DENIED));
        }
    }

    // ================================================================== //
    //  refresh()                                                           //
    // ================================================================== //

    @Nested
    @DisplayName("refresh()")
    class Refresh {

        /**
         * The token value used as an incoming refresh request in most tests.
         * Deliberately different from REFRESH_TOKEN so we can distinguish the
         * old token (incoming) from the new token (generated after rotation).
         */
        private static final String INCOMING_REFRESH_TOKEN = "old.refresh.token";

        @Test
        @DisplayName("should return a new TokenResponse and delete the old Redis key on success")
        void refresh_success() {
            when(jwtTokenProvider.validateToken(INCOMING_REFRESH_TOKEN)).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken(INCOMING_REFRESH_TOKEN)).thenReturn(USER_ID);

            String usedKey = USED_REFRESH_TOKEN_PREFIX + INCOMING_REFRESH_TOKEN;
            when(redisTemplate.hasKey(usedKey)).thenReturn(false);

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(REFRESH_TOKEN_PREFIX + USER_ID))
                    .thenReturn(INCOMING_REFRESH_TOKEN);

            User user = buildActiveUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            when(redisTemplate.getExpire(REFRESH_TOKEN_PREFIX + USER_ID, TimeUnit.MILLISECONDS))
                    .thenReturn(3_600_000L);

            when(jwtTokenProvider.generateAccessToken(eq(USER_ID), eq(USER_EMAIL), anyString()))
                    .thenReturn(ACCESS_TOKEN);
            when(jwtTokenProvider.generateRefreshToken(USER_ID)).thenReturn(REFRESH_TOKEN);
            when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(REFRESH_EXP_MS);
            when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(ACCESS_EXP_MS);

            TokenResponse response = authService.refresh(INCOMING_REFRESH_TOKEN);

            assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
        }

        @Test
        @DisplayName("should delete the old refresh_token key from Redis on success")
        void refresh_success_deletesOldRefreshKey() {
            when(jwtTokenProvider.validateToken(INCOMING_REFRESH_TOKEN)).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken(INCOMING_REFRESH_TOKEN)).thenReturn(USER_ID);

            String usedKey = USED_REFRESH_TOKEN_PREFIX + INCOMING_REFRESH_TOKEN;
            when(redisTemplate.hasKey(usedKey)).thenReturn(false);

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(REFRESH_TOKEN_PREFIX + USER_ID))
                    .thenReturn(INCOMING_REFRESH_TOKEN);

            User user = buildActiveUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(redisTemplate.getExpire(REFRESH_TOKEN_PREFIX + USER_ID, TimeUnit.MILLISECONDS))
                    .thenReturn(3_600_000L);
            when(jwtTokenProvider.generateAccessToken(eq(USER_ID), eq(USER_EMAIL), anyString()))
                    .thenReturn(ACCESS_TOKEN);
            when(jwtTokenProvider.generateRefreshToken(USER_ID)).thenReturn(REFRESH_TOKEN);
            when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(REFRESH_EXP_MS);
            when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(ACCESS_EXP_MS);

            authService.refresh(INCOMING_REFRESH_TOKEN);

            // The old key must be deleted so it can no longer be reused
            verify(redisTemplate).delete(REFRESH_TOKEN_PREFIX + USER_ID);
        }

        @Test
        @DisplayName("should store old token under used_refresh_token key for future replay detection")
        void refresh_success_storesUsedToken() {
            when(jwtTokenProvider.validateToken(INCOMING_REFRESH_TOKEN)).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken(INCOMING_REFRESH_TOKEN)).thenReturn(USER_ID);

            String usedKey = USED_REFRESH_TOKEN_PREFIX + INCOMING_REFRESH_TOKEN;
            when(redisTemplate.hasKey(usedKey)).thenReturn(false);

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(REFRESH_TOKEN_PREFIX + USER_ID))
                    .thenReturn(INCOMING_REFRESH_TOKEN);

            User user = buildActiveUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            long remainingTtl = 1_800_000L;
            when(redisTemplate.getExpire(REFRESH_TOKEN_PREFIX + USER_ID, TimeUnit.MILLISECONDS))
                    .thenReturn(remainingTtl);
            when(jwtTokenProvider.generateAccessToken(eq(USER_ID), eq(USER_EMAIL), anyString()))
                    .thenReturn(ACCESS_TOKEN);
            when(jwtTokenProvider.generateRefreshToken(USER_ID)).thenReturn(REFRESH_TOKEN);
            when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(REFRESH_EXP_MS);
            when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(ACCESS_EXP_MS);

            authService.refresh(INCOMING_REFRESH_TOKEN);

            // First opsForValue().set() → store used token; second → store new token
            verify(valueOperations).set(
                    eq(usedKey),
                    eq(USER_ID.toString()),
                    eq(remainingTtl),
                    eq(TimeUnit.MILLISECONDS)
            );
        }

        @Test
        @DisplayName("should throw BusinessException(INVALID_TOKEN) when token fails validation")
        void refresh_expiredToken_throwsBusinessException() {
            when(jwtTokenProvider.validateToken(INCOMING_REFRESH_TOKEN)).thenReturn(false);

            assertThatThrownBy(() -> authService.refresh(INCOMING_REFRESH_TOKEN))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).getErrorCode())
                                    .isEqualTo(ErrorCode.INVALID_TOKEN));

            // Nothing should be touched in Redis
            verify(redisTemplate, never()).hasKey(anyString());
            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("should throw BusinessException(INVALID_TOKEN) when stored token does not match")
        void refresh_tokenMismatch_throwsBusinessException() {
            when(jwtTokenProvider.validateToken(INCOMING_REFRESH_TOKEN)).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken(INCOMING_REFRESH_TOKEN)).thenReturn(USER_ID);

            // Not a used token
            when(redisTemplate.hasKey(USED_REFRESH_TOKEN_PREFIX + INCOMING_REFRESH_TOKEN))
                    .thenReturn(false);

            // Redis holds a different token value than what was presented
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(REFRESH_TOKEN_PREFIX + USER_ID))
                    .thenReturn("completely.different.token");

            assertThatThrownBy(() -> authService.refresh(INCOMING_REFRESH_TOKEN))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).getErrorCode())
                                    .isEqualTo(ErrorCode.INVALID_TOKEN));
        }

        @Test
        @DisplayName("should throw BusinessException(INVALID_TOKEN) when Redis returns null for stored token")
        void refresh_tokenNotFoundInRedis_throwsBusinessException() {
            when(jwtTokenProvider.validateToken(INCOMING_REFRESH_TOKEN)).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken(INCOMING_REFRESH_TOKEN)).thenReturn(USER_ID);

            when(redisTemplate.hasKey(USED_REFRESH_TOKEN_PREFIX + INCOMING_REFRESH_TOKEN))
                    .thenReturn(false);

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(REFRESH_TOKEN_PREFIX + USER_ID)).thenReturn(null);

            assertThatThrownBy(() -> authService.refresh(INCOMING_REFRESH_TOKEN))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).getErrorCode())
                                    .isEqualTo(ErrorCode.INVALID_TOKEN));
        }

        @Test
        @DisplayName("should throw BusinessException(INVALID_TOKEN) and invalidate all sessions on replay attack")
        void refresh_replayAttack_throwsAndInvalidatesAllSessions() {
            when(jwtTokenProvider.validateToken(INCOMING_REFRESH_TOKEN)).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken(INCOMING_REFRESH_TOKEN)).thenReturn(USER_ID);

            // The used-token key already exists → this token was already rotated
            String usedKey = USED_REFRESH_TOKEN_PREFIX + INCOMING_REFRESH_TOKEN;
            when(redisTemplate.hasKey(usedKey)).thenReturn(true);

            assertThatThrownBy(() -> authService.refresh(INCOMING_REFRESH_TOKEN))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).getErrorCode())
                                    .isEqualTo(ErrorCode.INVALID_TOKEN));

            // All active sessions for the user must be wiped
            verify(redisTemplate).delete(REFRESH_TOKEN_PREFIX + USER_ID);
        }

        @Test
        @DisplayName("on replay attack, should not proceed to load the user or generate new tokens")
        void refresh_replayAttack_doesNotCallRepositoryOrTokenProvider() {
            when(jwtTokenProvider.validateToken(INCOMING_REFRESH_TOKEN)).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken(INCOMING_REFRESH_TOKEN)).thenReturn(USER_ID);

            when(redisTemplate.hasKey(USED_REFRESH_TOKEN_PREFIX + INCOMING_REFRESH_TOKEN))
                    .thenReturn(true);

            try {
                authService.refresh(INCOMING_REFRESH_TOKEN);
            } catch (BusinessException ignored) {
                // expected
            }

            verify(userRepository, never()).findById(anyLong());
            verify(jwtTokenProvider, never()).generateAccessToken(anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("should throw BusinessException(UNAUTHORIZED) when user is not found after token validation")
        void refresh_userNotFound_throwsBusinessException() {
            when(jwtTokenProvider.validateToken(INCOMING_REFRESH_TOKEN)).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken(INCOMING_REFRESH_TOKEN)).thenReturn(USER_ID);

            when(redisTemplate.hasKey(USED_REFRESH_TOKEN_PREFIX + INCOMING_REFRESH_TOKEN))
                    .thenReturn(false);

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(REFRESH_TOKEN_PREFIX + USER_ID))
                    .thenReturn(INCOMING_REFRESH_TOKEN);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh(INCOMING_REFRESH_TOKEN))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).getErrorCode())
                                    .isEqualTo(ErrorCode.UNAUTHORIZED));
        }

        @Test
        @DisplayName("should fall back to getRefreshTokenExpiration() when Redis TTL is not positive")
        void refresh_success_usesFallbackTtlWhenRedisReturnsNonPositive() {
            when(jwtTokenProvider.validateToken(INCOMING_REFRESH_TOKEN)).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken(INCOMING_REFRESH_TOKEN)).thenReturn(USER_ID);

            String usedKey = USED_REFRESH_TOKEN_PREFIX + INCOMING_REFRESH_TOKEN;
            when(redisTemplate.hasKey(usedKey)).thenReturn(false);

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(REFRESH_TOKEN_PREFIX + USER_ID))
                    .thenReturn(INCOMING_REFRESH_TOKEN);

            User user = buildActiveUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            // Redis returns -1 (key has no TTL / not found) → fallback to provider value
            when(redisTemplate.getExpire(REFRESH_TOKEN_PREFIX + USER_ID, TimeUnit.MILLISECONDS))
                    .thenReturn(-1L);
            when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(REFRESH_EXP_MS);
            when(jwtTokenProvider.generateAccessToken(eq(USER_ID), eq(USER_EMAIL), anyString()))
                    .thenReturn(ACCESS_TOKEN);
            when(jwtTokenProvider.generateRefreshToken(USER_ID)).thenReturn(REFRESH_TOKEN);
            when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(ACCESS_EXP_MS);

            authService.refresh(INCOMING_REFRESH_TOKEN);

            // used token must be stored with the fallback TTL (REFRESH_EXP_MS)
            verify(valueOperations).set(
                    eq(usedKey),
                    eq(USER_ID.toString()),
                    eq(REFRESH_EXP_MS),
                    eq(TimeUnit.MILLISECONDS)
            );
        }
    }

    // ================================================================== //
    //  logout()                                                            //
    // ================================================================== //

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("should delete the refresh_token Redis key for the given userId")
        void logout_success_deletesRefreshTokenKey() {
            authService.logout(USER_ID);

            verify(redisTemplate).delete(REFRESH_TOKEN_PREFIX + USER_ID);
        }

        @Test
        @DisplayName("should not interact with JwtTokenProvider or UserRepository")
        void logout_doesNotTouchTokenProviderOrRepository() {
            authService.logout(USER_ID);

            verify(jwtTokenProvider, never()).validateToken(anyString());
            verify(userRepository, never()).findById(anyLong());
        }
    }

    // ================================================================== //
    //  getCurrentUser()                                                    //
    // ================================================================== //

    @Nested
    @DisplayName("getCurrentUser()")
    class GetCurrentUser {

        @Test
        @DisplayName("should return a UserResponse mapped from the found user")
        void getCurrentUser_success() {
            User user = buildActiveUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            UserResponse response = authService.getCurrentUser(USER_ID);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(USER_ID);
            assertThat(response.email()).isEqualTo(USER_EMAIL);
            assertThat(response.name()).isEqualTo(USER_NAME);
            assertThat(response.role()).isEqualTo(UserRole.USER);
            assertThat(response.isActive()).isTrue();
        }

        @Test
        @DisplayName("should throw BusinessException(UNAUTHORIZED) when the user does not exist")
        void getCurrentUser_notFound_throwsBusinessException() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.getCurrentUser(USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).getErrorCode())
                                    .isEqualTo(ErrorCode.UNAUTHORIZED));
        }

        @Test
        @DisplayName("should query the repository with the exact userId provided")
        void getCurrentUser_callsRepositoryWithCorrectId() {
            Long specificId = 42L;
            when(userRepository.findById(specificId)).thenReturn(Optional.empty());

            try {
                authService.getCurrentUser(specificId);
            } catch (BusinessException ignored) {
                // expected
            }

            verify(userRepository).findById(specificId);
        }
    }
}
