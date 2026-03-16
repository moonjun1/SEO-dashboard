package com.seodashboard.api.analysis.service;

import com.seodashboard.ai.client.AiClient;
import com.seodashboard.ai.dto.MetaSuggestion;
import com.seodashboard.api.analysis.dto.ContentAnalysisListResponse;
import com.seodashboard.api.analysis.dto.ContentAnalysisRequest;
import com.seodashboard.api.analysis.dto.ContentAnalysisResponse;
import com.seodashboard.api.analysis.dto.MetaGenerateRequest;
import com.seodashboard.api.analysis.dto.MetaGenerateResponse;
import com.seodashboard.api.analysis.event.AnalysisStartedEvent;
import com.seodashboard.api.analysis.repository.ContentAnalysisRepository;
import com.seodashboard.api.site.repository.SiteRepository;
import com.seodashboard.common.domain.ContentAnalysis;
import com.seodashboard.common.domain.Site;
import com.seodashboard.common.domain.User;
import com.seodashboard.common.dto.PageResponse;
import com.seodashboard.common.exception.BusinessException;
import com.seodashboard.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final ContentAnalysisRepository contentAnalysisRepository;
    private final SiteRepository siteRepository;
    private final AiClient aiClient;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ContentAnalysisResponse requestAnalysis(User user, ContentAnalysisRequest request) {
        // Validate target keywords count
        if (request.targetKeywords() != null && !request.targetKeywords().isBlank()) {
            long keywordCount = Arrays.stream(request.targetKeywords().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .count();
            if (keywordCount > 10) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Maximum 10 target keywords allowed");
            }
        }

        // Resolve site if provided
        Site site = null;
        if (request.siteId() != null) {
            site = siteRepository.findByIdAndUserId(request.siteId(), user.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.SITE_NOT_FOUND));
        }

        ContentAnalysis analysis = ContentAnalysis.builder()
                .site(site)
                .user(user)
                .title(request.title())
                .content(request.content())
                .targetKeywords(request.targetKeywords())
                .build();

        analysis = contentAnalysisRepository.save(analysis);

        log.info("Content analysis requested: id={}, userId={}", analysis.getId(), user.getId());

        // Publish event for async processing
        eventPublisher.publishEvent(new AnalysisStartedEvent(analysis.getId()));

        return ContentAnalysisResponse.from(analysis);
    }

    @Transactional(readOnly = true)
    public ContentAnalysisResponse getAnalysis(Long analysisId, Long userId) {
        ContentAnalysis analysis = contentAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_FOUND));

        if (!analysis.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ANALYSIS_NOT_FOUND);
        }

        return ContentAnalysisResponse.from(analysis);
    }

    @Transactional(readOnly = true)
    public PageResponse<ContentAnalysisListResponse> listAnalyses(Long userId, Long siteId,
                                                                    String status, Pageable pageable) {
        Page<ContentAnalysis> page;

        if (siteId != null) {
            page = contentAnalysisRepository.findByUserIdAndSiteId(userId, siteId, pageable);
        } else if (status != null && !status.isBlank()) {
            page = contentAnalysisRepository.findByUserIdAndStatus(userId, status, pageable);
        } else {
            page = contentAnalysisRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        Page<ContentAnalysisListResponse> responsePage = page.map(ContentAnalysisListResponse::from);
        return PageResponse.from(responsePage);
    }

    public MetaGenerateResponse generateMeta(MetaGenerateRequest request) {
        List<String> keywords = List.of();
        if (request.targetKeywords() != null && !request.targetKeywords().isBlank()) {
            keywords = Arrays.stream(request.targetKeywords().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }

        List<MetaSuggestion> suggestions = aiClient.generateMeta(
                request.content(), keywords, request.countOrDefault());

        return MetaGenerateResponse.from(suggestions);
    }
}
