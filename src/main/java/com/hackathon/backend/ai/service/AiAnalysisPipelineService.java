package com.hackathon.backend.ai.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hackathon.backend.ai.dto.ConversationSummaryResult;
import com.hackathon.backend.ai.dto.OverallReportResult;
import com.hackathon.backend.ai.entity.AiAnalysis;
import com.hackathon.backend.ai.entity.AiAnalysis.TaskType;
import com.hackathon.backend.ai.repository.AiAnalysisRepository;
import com.hackathon.backend.common.exception.NotFoundException;
import com.hackathon.backend.healthrecord.entity.HealthRecord;
import com.hackathon.backend.healthrecord.entity.HealthRecord.HealthType;
import com.hackathon.backend.healthrecord.repository.HealthRecordRepository;
import com.hackathon.backend.summary.entity.DailyConversationSummary;
import com.hackathon.backend.summary.entity.MonthlyConversationSummary;
import com.hackathon.backend.summary.entity.OverallReport;
import com.hackathon.backend.summary.entity.WeeklyConversationSummary;
import com.hackathon.backend.summary.repository.DailyConversationSummaryRepository;
import com.hackathon.backend.summary.repository.MonthlyConversationSummaryRepository;
import com.hackathon.backend.summary.repository.OverallReportRepository;
import com.hackathon.backend.summary.repository.WeeklyConversationSummaryRepository;

import tools.jackson.databind.ObjectMapper;

/**
 * 모델 원문을 계약 DTO로 검증한 뒤 TaskType에 맞는 기존 엔티티에 저장합니다.
 * 파싱 실패는 FAILED 전이와 함께 원문을 보존하고, 정상적인 빈 HEALTH_EXTRACTION 결과는 성공으로 처리합니다.
 */
@Service
@Transactional
public class AiAnalysisPipelineService {

	private final AiAnalysisRepository aiAnalysisRepository;
	private final AiAnalysisLifecycleService lifecycleService;
	private final AiResultParser resultParser;
	private final HealthRecordPersistenceService healthRecordPersistenceService;
	private final DailyConversationSummaryRepository dailyConversationSummaryRepository;
	private final WeeklyConversationSummaryRepository weeklyConversationSummaryRepository;
	private final MonthlyConversationSummaryRepository monthlyConversationSummaryRepository;
	private final OverallReportRepository overallReportRepository;
	private final HealthRecordRepository healthRecordRepository;
	private final ObjectMapper objectMapper;

	public AiAnalysisPipelineService(
			AiAnalysisRepository aiAnalysisRepository,
			AiAnalysisLifecycleService lifecycleService,
			AiResultParser resultParser,
			HealthRecordPersistenceService healthRecordPersistenceService,
			DailyConversationSummaryRepository dailyConversationSummaryRepository,
			WeeklyConversationSummaryRepository weeklyConversationSummaryRepository,
			MonthlyConversationSummaryRepository monthlyConversationSummaryRepository,
			OverallReportRepository overallReportRepository,
			HealthRecordRepository healthRecordRepository,
			ObjectMapper objectMapper) {
		this.aiAnalysisRepository = aiAnalysisRepository;
		this.lifecycleService = lifecycleService;
		this.resultParser = resultParser;
		this.healthRecordPersistenceService = healthRecordPersistenceService;
		this.dailyConversationSummaryRepository = dailyConversationSummaryRepository;
		this.weeklyConversationSummaryRepository = weeklyConversationSummaryRepository;
		this.monthlyConversationSummaryRepository = monthlyConversationSummaryRepository;
		this.overallReportRepository = overallReportRepository;
		this.healthRecordRepository = healthRecordRepository;
		this.objectMapper = objectMapper;
	}

	public void persist(Long analysisId, String rawResponse, AiAnalysisTaskContext context) {
		AiAnalysis analysis = findAnalysis(analysisId);
		try {
			switch (analysis.getTaskType()) {
				case HEALTH_EXTRACTION -> persistHealthExtraction(analysis, rawResponse, require(context,
						AiAnalysisTaskContext.HealthExtraction.class));
				case DAILY_SUMMARY -> persistDailySummary(analysis, rawResponse,
						require(context, AiAnalysisTaskContext.DailySummary.class));
				case WEEKLY_SUMMARY -> persistWeeklySummary(analysis, rawResponse,
						require(context, AiAnalysisTaskContext.WeeklySummary.class));
				case MONTHLY_SUMMARY -> persistMonthlySummary(analysis, rawResponse,
						require(context, AiAnalysisTaskContext.MonthlySummary.class));
				case OVERALL_REPORT -> persistOverallReport(analysis, rawResponse,
						require(context, AiAnalysisTaskContext.OverallReport.class));
			}
			lifecycleService.markSuccess(analysisId, rawResponse);
		} catch (AiResultValidationException e) {
			lifecycleService.markFailed(analysisId, rawResponse, e.getMessage());
		}
	}

	private void persistHealthExtraction(AiAnalysis analysis, String rawResponse, AiAnalysisTaskContext.HealthExtraction context) {
		if (analysis.getConversation() == null) {
			throw new AiResultValidationException("HEALTH_EXTRACTION에는 conversation이 필요합니다.");
		}
		healthRecordPersistenceService.persist(analysis.getMember(), analysis.getConversation(), analysis,
				resultParser.parseHealthExtraction(rawResponse));
	}

	private void persistDailySummary(AiAnalysis analysis, String rawResponse, AiAnalysisTaskContext.DailySummary context) {
		ConversationSummaryResult result = resultParser.parseConversationSummary(TaskType.DAILY_SUMMARY, rawResponse);
		if (context.summaryDate() == null || context.conversationCount() < 0 || context.tokenCount() < 0) {
			throw new AiResultValidationException("일일 요약 저장 메타데이터가 유효하지 않습니다.");
		}
		dailyConversationSummaryRepository.save(DailyConversationSummary.builder()
				.member(analysis.getMember()).summaryDate(context.summaryDate()).summary(result.summary())
				.conversationCount(context.conversationCount()).tokenCount(context.tokenCount()).build());
	}

	private void persistWeeklySummary(AiAnalysis analysis, String rawResponse, AiAnalysisTaskContext.WeeklySummary context) {
		ConversationSummaryResult result = resultParser.parseConversationSummary(TaskType.WEEKLY_SUMMARY, rawResponse);
		validatePeriod(context.periodStart(), context.periodEnd(), context.dailySummaryCount(), context.tokenCount(), "주간");
		weeklyConversationSummaryRepository.save(WeeklyConversationSummary.builder()
				.member(analysis.getMember()).periodStart(context.periodStart()).periodEnd(context.periodEnd()).summary(result.summary())
				.dailySummaryCount(context.dailySummaryCount()).tokenCount(context.tokenCount()).build());
	}

	private void persistMonthlySummary(AiAnalysis analysis, String rawResponse, AiAnalysisTaskContext.MonthlySummary context) {
		ConversationSummaryResult result = resultParser.parseConversationSummary(TaskType.MONTHLY_SUMMARY, rawResponse);
		validatePeriod(context.periodStart(), context.periodEnd(), context.weeklySummaryCount(), context.tokenCount(), "월간");
		monthlyConversationSummaryRepository.save(MonthlyConversationSummary.builder()
				.member(analysis.getMember()).periodStart(context.periodStart()).periodEnd(context.periodEnd()).summary(result.summary())
				.weeklySummaryCount(context.weeklySummaryCount()).tokenCount(context.tokenCount()).build());
	}

	private void persistOverallReport(AiAnalysis analysis, String rawResponse, AiAnalysisTaskContext.OverallReport context) {
		List<MonthlyConversationSummary> monthlySummaries = monthlyConversationSummaryRepository
				.findByMemberIdOrderByPeriodStartAsc(analysis.getMember().getId());
		if (monthlySummaries.isEmpty()) {
			throw new AiResultValidationException("OVERALL_REPORT는 월간 요약 또는 건강 기록이 있을 때만 생성할 수 있습니다.");
		}

		LocalDate expectedPeriodStart = monthlySummaries.get(0).getPeriodStart();
		LocalDate expectedPeriodEnd = monthlySummaries.get(monthlySummaries.size() - 1).getPeriodEnd();
		List<HealthRecord> healthRecords = healthRecordRepository.findByMemberIdAndRecordedDateBetween(
				analysis.getMember().getId(), expectedPeriodStart, expectedPeriodEnd);
		Set<HealthType> availableHealthTypes = healthRecords.stream().map(HealthRecord::getType)
				.collect(Collectors.toUnmodifiableSet());
		OverallReportResult result = resultParser.parseOverallReport(rawResponse, expectedPeriodStart, expectedPeriodEnd,
				availableHealthTypes);

		overallReportRepository.save(OverallReport.builder()
				.member(analysis.getMember()).summary(result.summary())
				.detail(serialize(result.detail())).monthlySummaryCount(monthlySummaries.size()).generatedAt(LocalDateTime.now())
				.build());
	}

	private void validatePeriod(LocalDate periodStart, LocalDate periodEnd, int sourceCount, int tokenCount, String label) {
		if (periodStart == null || periodEnd == null || periodEnd.isBefore(periodStart) || sourceCount < 0 || tokenCount < 0) {
			throw new AiResultValidationException(label + " 요약 저장 메타데이터가 유효하지 않습니다.");
		}
	}

	private String serialize(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (Exception e) {
			throw new IllegalStateException("overall report detail serialization failed", e);
		}
	}

	private AiAnalysis findAnalysis(Long analysisId) {
		return aiAnalysisRepository.findById(analysisId)
				.orElseThrow(() -> new NotFoundException("해당 분석 작업을 찾을 수 없습니다."));
	}

	private <T extends AiAnalysisTaskContext> T require(AiAnalysisTaskContext context, Class<T> expectedType) {
		if (!expectedType.isInstance(context)) {
			throw new AiResultValidationException("TaskType과 저장 메타데이터 타입이 일치하지 않습니다.");
		}
		return expectedType.cast(context);
	}
}
