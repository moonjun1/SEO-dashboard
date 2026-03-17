package com.seodashboard.api.publicseo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seodashboard.api.publicseo.dto.PublicAnalysisResponse;
import com.seodashboard.api.publicseo.repository.PublicAnalysisRepository;
import com.seodashboard.common.domain.PublicAnalysis;
import com.seodashboard.common.domain.enums.AnalysisStatus;
import com.seodashboard.common.exception.BusinessException;
import com.seodashboard.common.exception.ErrorCode;
import com.seodashboard.crawler.analyzer.SeoAnalyzer;
import com.seodashboard.crawler.engine.HtmlParser;
import com.seodashboard.crawler.engine.PageFetcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PublicSeoService.
 *
 * <p>UrlValidator.validateForFetch() is a static method that performs real DNS lookups,
 * so fetch-failure tests must use hostnames that actually resolve (e.g. example.com).
 * The pageFetcher is mocked to return a failure result, exercising the FAILED-status path
 * without making a real HTTP connection.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PublicSeoService")
class PublicSeoServiceTest {

    /** A real hostname that resolves in any environment and passes UrlValidator. */
    private static final String VALID_URL = "https://example.com";
    private static final String VALID_DOMAIN = "example.com";

    @Mock private PageFetcher pageFetcher;
    @Mock private HtmlParser htmlParser;
    @Mock private SeoAnalyzer seoAnalyzer;
    @Mock private PublicAnalysisRepository publicAnalysisRepository;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private PublicSeoService publicSeoService;

    // ------------------------------------------------------------------ //
    //  analyze() – SSRF protection                                         //
    //  UrlValidator blocks hostname/IP before repository is ever touched.  //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("analyze() SSRF protection")
    class SsrfProtection {

        @Test
        @DisplayName("throws BusinessException(VALIDATION_ERROR) for localhost URL")
        void localhostUrl_throwsBusinessException() {
            assertThatThrownBy(() -> publicSeoService.analyze("http://localhost/admin"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).getErrorCode())
                                    .isEqualTo(ErrorCode.VALIDATION_ERROR));
        }

        @Test
        @DisplayName("throws BusinessException(VALIDATION_ERROR) for 127.0.0.1")
        void loopbackIp_throwsBusinessException() {
            assertThatThrownBy(() -> publicSeoService.analyze("http://127.0.0.1/"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).getErrorCode())
                                    .isEqualTo(ErrorCode.VALIDATION_ERROR));
        }

        @Test
        @DisplayName("throws BusinessException(VALIDATION_ERROR) for private 192.168.x.x")
        void privateIp_throwsBusinessException() {
            assertThatThrownBy(() -> publicSeoService.analyze("http://192.168.1.1/"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).getErrorCode())
                                    .isEqualTo(ErrorCode.VALIDATION_ERROR));
        }

        @Test
        @DisplayName("does not call pageFetcher when URL is blocked by SSRF guard")
        void blockedUrl_pageFetcherNeverCalled() {
            try {
                publicSeoService.analyze("http://localhost/");
            } catch (BusinessException ignored) {
                // expected – verify pageFetcher is untouched
            }

            verify(pageFetcher, never()).fetch(anyString());
        }

        @Test
        @DisplayName("does not persist anything when URL is blocked by SSRF guard")
        void blockedUrl_repositoryNeverCalled() {
            try {
                publicSeoService.analyze("http://127.0.0.1/secret");
            } catch (BusinessException ignored) {
                // expected
            }

            verify(publicAnalysisRepository, never()).save(any());
        }
    }

    // ------------------------------------------------------------------ //
    //  analyze() – fetch failure                                           //
    //                                                                      //
    //  Uses example.com (always resolvable) so UrlValidator passes.        //
    //  pageFetcher is mocked to simulate a network failure.                //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("analyze() when a valid URL fails to fetch")
    class FetchFailure {

        /**
         * Stubs repository.save() to return the given entity.
         * Entity JSON fields are all null, so parseJsonSafe() returns null
         * immediately without touching objectMapper.
         */
        private PublicAnalysis stubInitialSave() {
            PublicAnalysis entity = PublicAnalysis.builder()
                    .url(VALID_URL)
                    .domain(VALID_DOMAIN)
                    .status(AnalysisStatus.ANALYZING)
                    .build();
            when(publicAnalysisRepository.save(any(PublicAnalysis.class))).thenReturn(entity);
            return entity;
        }

        @Test
        @DisplayName("returns PublicAnalysisResponse with FAILED status")
        void fetchFails_responseHasFailedStatus() {
            stubInitialSave();
            when(pageFetcher.fetch(anyString())).thenReturn(failedFetch("Connection refused"));

            PublicAnalysisResponse response = publicSeoService.analyze(VALID_URL);

            assertThat(response.status()).isEqualTo("FAILED");
        }

        @Test
        @DisplayName("error message in response is not blank when fetch fails")
        void fetchFails_errorMessagePresent() {
            stubInitialSave();
            when(pageFetcher.fetch(anyString())).thenReturn(failedFetch("Connection refused"));

            PublicAnalysisResponse response = publicSeoService.analyze(VALID_URL);

            assertThat(response.errorMessage()).isNotBlank();
        }

        @Test
        @DisplayName("persists entity with FAILED status after unsuccessful fetch")
        void fetchFails_entitySavedWithFailedStatus() {
            stubInitialSave();
            when(pageFetcher.fetch(anyString())).thenReturn(failedFetch("Timeout"));

            publicSeoService.analyze(VALID_URL);

            // At minimum: save(ANALYZING), then save(FAILED after markFailed)
            ArgumentCaptor<PublicAnalysis> captor = ArgumentCaptor.forClass(PublicAnalysis.class);
            verify(publicAnalysisRepository, atLeast(2)).save(captor.capture());

            PublicAnalysis lastSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);
            assertThat(lastSaved.getStatus()).isEqualTo(AnalysisStatus.FAILED);
        }

        @Test
        @DisplayName("does not invoke HtmlParser or SeoAnalyzer when fetch is unsuccessful")
        void fetchFails_parserAndAnalyzerSkipped() {
            stubInitialSave();
            when(pageFetcher.fetch(anyString())).thenReturn(failedFetch("Connection refused"));

            publicSeoService.analyze(VALID_URL);

            verify(htmlParser, never()).parse(anyString(), anyString());
            verify(seoAnalyzer, never()).analyze(any(), anyInt(), anyInt());
        }

        // ── factory ─────────────────────────────────────────────────────────
        private PageFetcher.FetchResult failedFetch(String errorMsg) {
            // statusCode=0, body=null → isSuccess() returns false
            return new PageFetcher.FetchResult(0, null, null, 0L, 0, null, errorMsg);
        }
    }

    // ------------------------------------------------------------------ //
    //  getAnalysis()                                                       //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("getAnalysis()")
    class GetAnalysis {

        @Test
        @DisplayName("throws BusinessException(ANALYSIS_NOT_FOUND) for non-existent ID")
        void nonExistentId_throwsBusinessException() {
            when(publicAnalysisRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> publicSeoService.getAnalysis(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).getErrorCode())
                                    .isEqualTo(ErrorCode.ANALYSIS_NOT_FOUND));
        }

        @Test
        @DisplayName("returns response mapped from entity when ID exists")
        void existingId_returnsMappedResponse() {
            // All JSON fields (headingStructure, issues, linkList, metaTags) are null
            // → parseJsonSafe returns null without touching objectMapper.
            PublicAnalysis entity = PublicAnalysis.builder()
                    .url(VALID_URL)
                    .domain(VALID_DOMAIN)
                    .status(AnalysisStatus.COMPLETED)
                    .build();

            when(publicAnalysisRepository.findById(1L)).thenReturn(Optional.of(entity));

            PublicAnalysisResponse response = publicSeoService.getAnalysis(1L);

            assertThat(response).isNotNull();
            assertThat(response.url()).isEqualTo(VALID_URL);
            assertThat(response.domain()).isEqualTo(VALID_DOMAIN);
            assertThat(response.status()).isEqualTo("COMPLETED");
        }

        @Test
        @DisplayName("calls repository findById with the exact ID provided")
        void callsRepositoryWithCorrectId() {
            when(publicAnalysisRepository.findById(42L)).thenReturn(Optional.empty());

            try {
                publicSeoService.getAnalysis(42L);
            } catch (BusinessException ignored) {
                // expected
            }

            verify(publicAnalysisRepository).findById(42L);
        }
    }
}
