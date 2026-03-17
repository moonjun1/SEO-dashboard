package com.seodashboard.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimitInterceptor")
class RateLimitInterceptorTest {

    private static final int MAX_REQUESTS = 3;
    private static final long WINDOW_MS = 60_000L; // 1 minute
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitInterceptor(MAX_REQUESTS, WINDOW_MS, OBJECT_MAPPER);
    }

    // ------------------------------------------------------------------ //
    //  Happy Path – within limit                                           //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("requests within the limit")
    class WithinLimit {

        @Test
        @DisplayName("first request returns true")
        void firstRequest_returnsTrue() throws Exception {
            MockHttpServletRequest request = requestFrom("1.2.3.4", "/api/analyze");
            MockHttpServletResponse response = new MockHttpServletResponse();

            boolean result = interceptor.preHandle(request, response, new Object());

            assertThat(result).isTrue();
            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("requests up to maxRequests all return true")
        void requestsUpToLimit_allReturnTrue() throws Exception {
            for (int i = 1; i <= MAX_REQUESTS; i++) {
                MockHttpServletRequest request = requestFrom("1.2.3.4", "/api/analyze");
                MockHttpServletResponse response = new MockHttpServletResponse();

                boolean result = interceptor.preHandle(request, response, new Object());

                assertThat(result)
                        .as("Request #%d should be allowed", i)
                        .isTrue();
            }
        }
    }

    // ------------------------------------------------------------------ //
    //  Rate limit exceeded                                                 //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("request exceeding the limit")
    class ExceedingLimit {

        @Test
        @DisplayName("request beyond maxRequests returns false and 429")
        void requestBeyondLimit_returns429() throws Exception {
            // exhaust the quota
            for (int i = 0; i < MAX_REQUESTS; i++) {
                interceptor.preHandle(requestFrom("5.5.5.5", "/api/analyze"),
                        new MockHttpServletResponse(), new Object());
            }

            // one more request over the limit
            MockHttpServletRequest over = requestFrom("5.5.5.5", "/api/analyze");
            MockHttpServletResponse response = new MockHttpServletResponse();

            boolean result = interceptor.preHandle(over, response, new Object());

            assertThat(result).isFalse();
            assertThat(response.getStatus()).isEqualTo(429);
        }

        @Test
        @DisplayName("429 response body is valid JSON with RATE_LIMIT_EXCEEDED code")
        void exceededResponse_hasCorrectJsonBody() throws Exception {
            for (int i = 0; i <= MAX_REQUESTS; i++) {
                interceptor.preHandle(requestFrom("6.6.6.6", "/api/analyze"),
                        new MockHttpServletResponse(), new Object());
            }

            MockHttpServletRequest over = requestFrom("6.6.6.6", "/api/analyze");
            MockHttpServletResponse response = new MockHttpServletResponse();
            interceptor.preHandle(over, response, new Object());

            String body = response.getContentAsString();
            assertThat(body).contains("RATE_LIMIT_EXCEEDED");
        }

        @Test
        @DisplayName("429 response includes Retry-After header")
        void exceededResponse_hasRetryAfterHeader() throws Exception {
            for (int i = 0; i <= MAX_REQUESTS; i++) {
                interceptor.preHandle(requestFrom("7.7.7.7", "/api/analyze"),
                        new MockHttpServletResponse(), new Object());
            }

            MockHttpServletRequest over = requestFrom("7.7.7.7", "/api/analyze");
            MockHttpServletResponse response = new MockHttpServletResponse();
            interceptor.preHandle(over, response, new Object());

            assertThat(response.getHeader("Retry-After")).isNotNull();
        }
    }

    // ------------------------------------------------------------------ //
    //  IP isolation                                                        //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("different IPs have independent counters")
    class IpIsolation {

        @Test
        @DisplayName("IP-A exhausting quota does not affect IP-B")
        void differentIps_haveIndependentCounters() throws Exception {
            // exhaust quota for IP-A
            for (int i = 0; i <= MAX_REQUESTS; i++) {
                interceptor.preHandle(requestFrom("10.0.0.1", "/api/analyze"),
                        new MockHttpServletResponse(), new Object());
            }

            // IP-B should still be allowed
            MockHttpServletRequest ipBRequest = requestFrom("10.0.0.2", "/api/analyze");
            MockHttpServletResponse response = new MockHttpServletResponse();

            boolean result = interceptor.preHandle(ipBRequest, response, new Object());

            assertThat(result).isTrue();
            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("X-Forwarded-For header is used to determine client IP")
        void xForwardedForHeader_usedAsClientIp() throws Exception {
            // exhaust quota via X-Forwarded-For IP
            for (int i = 0; i <= MAX_REQUESTS; i++) {
                MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/analyze");
                req.addHeader("X-Forwarded-For", "20.20.20.20");
                interceptor.preHandle(req, new MockHttpServletResponse(), new Object());
            }

            // same forwarded IP should be blocked
            MockHttpServletRequest over = new MockHttpServletRequest("GET", "/api/analyze");
            over.addHeader("X-Forwarded-For", "20.20.20.20");
            MockHttpServletResponse response = new MockHttpServletResponse();

            boolean result = interceptor.preHandle(over, response, new Object());

            assertThat(result).isFalse();
            assertThat(response.getStatus()).isEqualTo(429);
        }

        @Test
        @DisplayName("different URI paths for the same IP have independent counters")
        void differentUris_haveIndependentCounters() throws Exception {
            // exhaust quota on /api/analyze
            for (int i = 0; i <= MAX_REQUESTS; i++) {
                interceptor.preHandle(requestFrom("30.30.30.30", "/api/analyze"),
                        new MockHttpServletResponse(), new Object());
            }

            // same IP but different path should still be allowed
            MockHttpServletRequest otherPath = requestFrom("30.30.30.30", "/api/ranking");
            MockHttpServletResponse response = new MockHttpServletResponse();

            boolean result = interceptor.preHandle(otherPath, response, new Object());

            assertThat(result).isTrue();
        }
    }

    // ------------------------------------------------------------------ //
    //  Window reset                                                        //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("window resets after the time period passes")
    class WindowReset {

        @Test
        @DisplayName("new interceptor with very short window resets after window expires")
        void windowExpiry_resetsCounter() throws Exception {
            long shortWindowMs = 50L; // 50 ms window
            RateLimitInterceptor shortWindowInterceptor =
                    new RateLimitInterceptor(MAX_REQUESTS, shortWindowMs, OBJECT_MAPPER);

            // exhaust quota
            for (int i = 0; i <= MAX_REQUESTS; i++) {
                shortWindowInterceptor.preHandle(
                        requestFrom("99.99.99.99", "/api/analyze"),
                        new MockHttpServletResponse(), new Object());
            }

            // wait for window to expire
            Thread.sleep(shortWindowMs + 20);

            // first request in the new window should be allowed
            MockHttpServletRequest fresh = requestFrom("99.99.99.99", "/api/analyze");
            MockHttpServletResponse response = new MockHttpServletResponse();

            boolean result = shortWindowInterceptor.preHandle(fresh, response, new Object());

            assertThat(result).isTrue();
        }
    }

    // ------------------------------------------------------------------ //
    //  Helper                                                              //
    // ------------------------------------------------------------------ //

    private MockHttpServletRequest requestFrom(String remoteAddr, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
