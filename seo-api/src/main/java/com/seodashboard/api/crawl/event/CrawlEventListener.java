package com.seodashboard.api.crawl.event;

import com.seodashboard.crawler.service.CrawlExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlEventListener {

    private final CrawlExecutionService crawlExecutionService;

    @Async("crawlExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCrawlStarted(CrawlStartedEvent event) {
        log.info("Transaction committed, starting crawl for job: {}", event.crawlJobId());
        try {
            // Small delay to ensure transaction is fully committed and visible
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        crawlExecutionService.executeCrawl(event.crawlJobId());
    }
}
