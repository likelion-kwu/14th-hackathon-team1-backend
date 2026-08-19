package com.hackathon.backend.ai.service;

import java.time.LocalDate;

/**
 * AI 응답을 저장할 때 모델 출력 밖에서 서비스 계층이 계산한 메타데이터입니다.
 */
public sealed interface AiAnalysisTaskContext permits AiAnalysisTaskContext.HealthExtraction,
		AiAnalysisTaskContext.DailySummary, AiAnalysisTaskContext.WeeklySummary,
		AiAnalysisTaskContext.MonthlySummary, AiAnalysisTaskContext.OverallReport {

	record HealthExtraction() implements AiAnalysisTaskContext {
	}

	record DailySummary(LocalDate summaryDate, int conversationCount, int tokenCount) implements AiAnalysisTaskContext {
	}

	record WeeklySummary(LocalDate periodStart, LocalDate periodEnd, int dailySummaryCount, int tokenCount)
			implements AiAnalysisTaskContext {
	}

	record MonthlySummary(LocalDate periodStart, LocalDate periodEnd, int weeklySummaryCount, int tokenCount)
			implements AiAnalysisTaskContext {
	}

	/** OVERALL_REPORT의 기간과 건강 타입은 저장 서비스가 기존 데이터에서 계산합니다. */
	record OverallReport() implements AiAnalysisTaskContext {
	}
}
