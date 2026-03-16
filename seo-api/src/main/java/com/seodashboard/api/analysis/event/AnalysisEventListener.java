package com.seodashboard.api.analysis.event;

import com.seodashboard.ai.service.ContentAnalysisExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisEventListener {

    private final ContentAnalysisExecutor contentAnalysisExecutor;

    @Async("crawlExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAnalysisStarted(AnalysisStartedEvent event) {
        log.info("Transaction committed, starting content analysis: {}", event.analysisId());
        try {
            // Small delay to ensure transaction is fully committed and visible
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        contentAnalysisExecutor.execute(event.analysisId());
    }
}
