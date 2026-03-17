package com.seodashboard.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ObjectMapper objectMapper;

    // CORS is handled by SecurityConfig only (single source of truth).
    // No addCorsMappings() here to avoid duplicate/conflicting configuration.

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Public analyze: 5 requests per minute per IP
        registry.addInterceptor(new RateLimitInterceptor(5, 60_000, objectMapper))
                .addPathPatterns("/api/v1/public/analyze");

        // Auth login: 10 requests per minute per IP
        registry.addInterceptor(new RateLimitInterceptor(10, 60_000, objectMapper))
                .addPathPatterns("/api/v1/auth/login");

        // Auth signup: 3 requests per minute per IP
        registry.addInterceptor(new RateLimitInterceptor(3, 60_000, objectMapper))
                .addPathPatterns("/api/v1/auth/signup");
    }
}
