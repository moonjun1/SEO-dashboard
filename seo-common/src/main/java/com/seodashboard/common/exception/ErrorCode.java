package com.seodashboard.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Authentication & Authorization
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "Token has expired"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Invalid token"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied"),

    // Site
    SITE_NOT_FOUND(HttpStatus.NOT_FOUND, "SITE_NOT_FOUND", "Site not found"),
    DUPLICATE_SITE(HttpStatus.CONFLICT, "DUPLICATE_SITE", "Site already exists"),

    // Crawl
    CRAWL_ALREADY_RUNNING(HttpStatus.CONFLICT, "CRAWL_ALREADY_RUNNING", "Crawl job is already running"),
    CRAWL_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "CRAWL_JOB_NOT_FOUND", "Crawl job not found"),

    // Keyword
    KEYWORD_NOT_FOUND(HttpStatus.NOT_FOUND, "KEYWORD_NOT_FOUND", "Keyword not found"),
    DUPLICATE_KEYWORD(HttpStatus.CONFLICT, "DUPLICATE_KEYWORD", "Keyword already exists"),

    // Report
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND", "Report not found"),

    // Notification
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "Notification not found"),

    // Content Analysis
    ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "ANALYSIS_NOT_FOUND", "Content analysis not found"),
    ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ANALYSIS_FAILED", "Content analysis failed"),

    // System
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Rate limit exceeded"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "Service unavailable");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
