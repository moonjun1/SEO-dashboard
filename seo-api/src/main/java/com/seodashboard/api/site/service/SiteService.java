package com.seodashboard.api.site.service;

import com.seodashboard.api.site.dto.SiteCreateRequest;
import com.seodashboard.api.site.dto.SiteListResponse;
import com.seodashboard.api.site.dto.SiteResponse;
import com.seodashboard.api.site.dto.SiteUpdateRequest;
import com.seodashboard.api.site.repository.SiteRepository;
import com.seodashboard.common.domain.Site;
import com.seodashboard.common.domain.User;
import com.seodashboard.common.dto.PageResponse;
import com.seodashboard.common.exception.BusinessException;
import com.seodashboard.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteService {

    private final SiteRepository siteRepository;

    @Transactional
    public SiteResponse createSite(User user, SiteCreateRequest request) {
        if (siteRepository.existsByUserIdAndUrl(user.getId(), request.url())) {
            throw new BusinessException(ErrorCode.DUPLICATE_SITE, "Site with this URL already exists");
        }

        Site site = Site.builder()
                .user(user)
                .url(request.url())
                .name(request.name())
                .description(request.description())
                .crawlIntervalHours(request.crawlIntervalHoursOrDefault())
                .build();

        site = siteRepository.save(site);
        log.info("Site created: id={}, url={}, userId={}", site.getId(), site.getUrl(), user.getId());

        return SiteResponse.from(site);
    }

    @Transactional(readOnly = true)
    public PageResponse<SiteListResponse> getSites(Long userId, Pageable pageable) {
        Page<SiteListResponse> page = siteRepository
                .findByUserIdAndIsActiveTrue(userId, pageable)
                .map(SiteListResponse::from);

        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public SiteResponse getSite(Long siteId, Long userId) {
        Site site = findSiteByIdAndUserId(siteId, userId);
        return SiteResponse.from(site);
    }

    @Transactional
    public SiteResponse updateSite(Long siteId, Long userId, SiteUpdateRequest request) {
        Site site = findSiteByIdAndUserId(siteId, userId);

        if (request.url() != null && !request.url().equals(site.getUrl())) {
            if (siteRepository.existsByUserIdAndUrl(userId, request.url())) {
                throw new BusinessException(ErrorCode.DUPLICATE_SITE, "Site with this URL already exists");
            }
        }

        site.update(request.url(), request.name(), request.description(), request.crawlIntervalHours());
        log.info("Site updated: id={}, userId={}", siteId, userId);

        return SiteResponse.from(site);
    }

    @Transactional
    public void deleteSite(Long siteId, Long userId) {
        Site site = findSiteByIdAndUserId(siteId, userId);
        site.deactivate();
        log.info("Site deactivated: id={}, userId={}", siteId, userId);
    }

    private Site findSiteByIdAndUserId(Long siteId, Long userId) {
        return siteRepository.findByIdAndUserId(siteId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SITE_NOT_FOUND));
    }
}
