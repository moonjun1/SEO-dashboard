package com.seodashboard.ai.client;

import com.seodashboard.ai.dto.AiAnalysisResult;
import com.seodashboard.ai.dto.MetaSuggestion;

import java.util.List;

public interface AiClient {

    AiAnalysisResult analyzeContent(String content, List<String> targetKeywords);

    List<MetaSuggestion> generateMeta(String content, List<String> targetKeywords, int count);
}
