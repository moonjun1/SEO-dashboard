package com.seodashboard.api.auth.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtTokenProvider")
class JwtTokenProviderTest {

    /**
     * 64-byte (512-bit) secret, Base64-encoded. Satisfies HS512 minimum
     * and passes the 32-byte key length guard in JwtTokenProvider.
     */
    private static final String STRONG_SECRET =
            Base64.getEncoder().encodeToString(new byte[64]); // 64 zero-bytes → 88-char Base64

    private static final long ACCESS_TOKEN_EXPIRATION = 3_600_000L;   // 1 hour
    private static final long REFRESH_TOKEN_EXPIRATION = 86_400_000L; // 24 hours
    private static final String PROFILE_LOCAL = "local";

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(
                STRONG_SECRET,
                ACCESS_TOKEN_EXPIRATION,
                REFRESH_TOKEN_EXPIRATION,
                PROFILE_LOCAL
        );
    }

    // ------------------------------------------------------------------ //
    //  Token generation                                                    //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("generateAccessToken")
    class GenerateAccessToken {

        @Test
        @DisplayName("returns a non-blank JWT string")
        void generatesNonBlankToken() {
            String token = tokenProvider.generateAccessToken(1L, "user@example.com", "ROLE_USER");

            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("token contains three dot-separated segments")
        void tokenHasThreeSegments() {
            String token = tokenProvider.generateAccessToken(1L, "user@example.com", "ROLE_USER");

            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("subject claim matches the given userId")
        void subjectMatchesUserId() {
            String token = tokenProvider.generateAccessToken(42L, "user@example.com", "ROLE_USER");

            Long extractedId = tokenProvider.getUserIdFromToken(token);

            assertThat(extractedId).isEqualTo(42L);
        }
    }

    // ------------------------------------------------------------------ //
    //  getUserIdFromToken                                                  //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("getUserIdFromToken")
    class GetUserIdFromToken {

        @Test
        @DisplayName("correctly extracts userId from token")
        void extractsCorrectUserId() {
            long expectedId = 99L;
            String token = tokenProvider.generateAccessToken(expectedId, "test@example.com", "ROLE_ADMIN");

            assertThat(tokenProvider.getUserIdFromToken(token)).isEqualTo(expectedId);
        }

        @Test
        @DisplayName("extracts different userId values faithfully")
        void extractsVariousUserIds() {
            for (long id : new long[]{1L, 100L, Long.MAX_VALUE}) {
                String token = tokenProvider.generateAccessToken(id, "u@x.com", "ROLE_USER");
                assertThat(tokenProvider.getUserIdFromToken(token))
                        .as("userId %d", id)
                        .isEqualTo(id);
            }
        }
    }

    // ------------------------------------------------------------------ //
    //  validateToken                                                       //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("returns true for a freshly issued token")
        void validToken_returnsTrue() {
            String token = tokenProvider.generateAccessToken(1L, "u@x.com", "ROLE_USER");

            assertThat(tokenProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("returns false for an already-expired token")
        void expiredToken_returnsFalse() {
            // Construct an already-expired token manually using the same secret
            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(STRONG_SECRET));
            String expiredToken = Jwts.builder()
                    .subject("1")
                    .issuedAt(new Date(System.currentTimeMillis() - 10_000))
                    .expiration(new Date(System.currentTimeMillis() - 5_000))
                    .signWith(key)
                    .compact();

            assertThat(tokenProvider.validateToken(expiredToken)).isFalse();
        }

        @Test
        @DisplayName("returns false for a token with tampered signature")
        void tamperedToken_returnsFalse() {
            String token = tokenProvider.generateAccessToken(1L, "u@x.com", "ROLE_USER");
            String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsignature";

            assertThat(tokenProvider.validateToken(tampered)).isFalse();
        }

        @Test
        @DisplayName("returns false for a completely random string")
        void randomString_returnsFalse() {
            assertThat(tokenProvider.validateToken("not.a.token")).isFalse();
        }

        @Test
        @DisplayName("returns false for blank string")
        void blankToken_returnsFalse() {
            assertThat(tokenProvider.validateToken("")).isFalse();
        }

        @Test
        @DisplayName("returns false for null")
        void nullToken_returnsFalse() {
            assertThat(tokenProvider.validateToken(null)).isFalse();
        }
    }

    // ------------------------------------------------------------------ //
    //  Constructor validation                                              //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("constructor secret validation")
    class ConstructorValidation {

        @Test
        @DisplayName("rejects secret shorter than 32 bytes when decoded (15 bytes)")
        void shortSecret_throwsIllegalState() {
            // 15 bytes Base64-encoded → decodes to 15 bytes < 32 required
            String shortSecret = Base64.getEncoder().encodeToString(new byte[15]);

            assertThatThrownBy(() -> new JwtTokenProvider(
                    shortSecret, ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION, PROFILE_LOCAL))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("256 bits");
        }

        @Test
        @DisplayName("rejects secret shorter than 32 bytes on non-local profile")
        void shortSecretOnProdProfile_throwsIllegalState() {
            String shortSecret = Base64.getEncoder().encodeToString(new byte[15]);

            assertThatThrownBy(() -> new JwtTokenProvider(
                    shortSecret, ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION, "prod"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("rejects weak default secret on non-local profile")
        void weakDefaultSecretOnProdProfile_throwsIllegalState() {
            // This starts with WEAK_DEFAULT_PREFIX = "c2VvLWRhc2hib2FyZC1zZWNyZXQ"
            // Provide a sufficiently long value (to pass the byte-length check) but
            // prefixed with the known-weak prefix.
            //
            // "c2VvLWRhc2hib2FyZC1zZWNyZXQ=" decodes to "seo-dashboard-secret" (20 bytes)
            // Pad to >= 32 bytes by appending extra Base64-safe padding.
            // We construct: prefix bytes + zero padding, then Base64-encode the whole thing.
            byte[] weakBytes = "seo-dashboard-secret0000000000000000".getBytes(); // 36 bytes
            String weakSecret = Base64.getEncoder().encodeToString(weakBytes);

            assertThatThrownBy(() -> new JwtTokenProvider(
                    weakSecret, ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION, "prod"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("JWT_SECRET");
        }

        @Test
        @DisplayName("accepts strong secret on non-local profile")
        void strongSecretOnProdProfile_doesNotThrow() {
            org.assertj.core.api.Assertions.assertThatCode(() -> new JwtTokenProvider(
                    STRONG_SECRET, ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION, "prod"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts weak-looking secret on local profile without throwing")
        void weakSecretOnLocalProfile_doesNotThrow() {
            // On local profile, the weak-secret guard is skipped; only the byte-length
            // guard still applies. Use a 32-byte secret.
            String localSecret = Base64.getEncoder().encodeToString(new byte[32]);

            org.assertj.core.api.Assertions.assertThatCode(() -> new JwtTokenProvider(
                    localSecret, ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION, "local"))
                    .doesNotThrowAnyException();
        }
    }
}
