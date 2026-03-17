package com.seodashboard.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seodashboard.common.dto.ApiResponse;
import com.seodashboard.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter for public endpoints.
 * Uses a sliding window counter per IP address.
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private final int maxRequests;
    private final long windowMs;
    private final ObjectMapper objectMapper;

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public RateLimitInterceptor(int maxRequests, long windowMs, ObjectMapper objectMapper) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);
        String key = clientIp + ":" + request.getRequestURI();

        WindowCounter counter = counters.compute(key, (k, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || now - existing.windowStart > windowMs) {
                return new WindowCounter(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (counter.count.get() > maxRequests) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(windowMs / 1000));
            ApiResponse<Void> body = ApiResponse.error(
                    new ErrorResponse("RATE_LIMIT_EXCEEDED", "Too many requests. Please try again later.")
            );
            objectMapper.writeValue(response.getOutputStream(), body);
            return false;
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private record WindowCounter(long windowStart, AtomicInteger count) {
    }
}
