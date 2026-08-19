package com.hackathon.backend.ai.dto;

/** daily-summary-v1, weekly-summary-v1, monthly-summary-v1 모델 출력입니다. */
public record ConversationSummaryResult(String schemaVersion, String summary) {
}
