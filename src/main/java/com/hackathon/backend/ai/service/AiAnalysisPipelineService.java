package com.hackathon.backend.ai.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import com.hackathon.backend.ai.dto.ConversationSummaryResult;
import com.hackathon.backend.ai.dto.OverallReportResult;
import com.hackathon.backend.ai.client.OpenAiChatClient;
import com.hackathon.backend.ai.entity.AiAnalysis;
import com.hackathon.backend.ai.entity.AiAnalysis.TaskType;
import com.hackathon.backend.ai.repository.AiAnalysisRepository;
import com.hackathon.backend.common.exception.NotFoundException;
import com.hackathon.backend.conversation.entity.Conversation;
import com.hackathon.backend.conversation.entity.ConversationMessage;
import com.hackathon.backend.conversation.repository.ConversationMessageRepository;
import com.hackathon.backend.conversation.repository.ConversationRepository;
import com.hackathon.backend.healthrecord.entity.HealthRecord;
import com.hackathon.backend.healthrecord.entity.HealthRecord.HealthType;
import com.hackathon.backend.healthrecord.repository.HealthRecordRepository;
import com.hackathon.backend.member.entity.Member;
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

	private static final int MAX_OPENAI_ATTEMPTS = 3;
	private static final long RETRY_JITTER_MILLIS = 500;
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
	private final ConversationRepository conversationRepository;
	private final ConversationMessageRepository conversationMessageRepository;
	private final OpenAiChatClient openAiChatClient;
	private final TransactionTemplate transactionTemplate;

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
			ObjectMapper objectMapper,
			ConversationRepository conversationRepository,
			ConversationMessageRepository conversationMessageRepository,
			OpenAiChatClient openAiChatClient, TransactionTemplate transactionTemplate) {
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
		this.conversationRepository = conversationRepository;
		this.conversationMessageRepository = conversationMessageRepository;
		this.openAiChatClient = openAiChatClient;
		this.transactionTemplate = transactionTemplate;
	}

	/**
	 * Public conversation-completion hook. Call this immediately after a conversation is completed.
	 * Model failures are retained on the AI analysis task and do not alter the completed conversation state.
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void trigger(Long conversationId) {
		Long analysisId;
		try {
			analysisId = lifecycleService.startHealthExtraction(conversationId, openAiChatClient.modelName()).orElse(null);
		} catch (DataIntegrityViolationException exception) {
			return;
		}
		if (analysisId == null) {
			return;
		}
		Conversation conversation = conversationRepository.findById(conversationId)
				.orElseThrow(() -> new NotFoundException("Conversation not found"));
		try {
			String rawResponse = completeWithRetry(healthExtractionSystemPrompt(), conversationTranscript(conversation));
			transactionTemplate.executeWithoutResult(status -> persist(analysisId, rawResponse,
					new AiAnalysisTaskContext.HealthExtraction()));
		} catch (Exception exception) {
			lifecycleService.markFailed(analysisId, errorMessage(exception));
		}
	}

	/**
	 * 대화 완료 훅과 스케줄/온디맨드 요약이 공유하는 실행 경계입니다.
	 * 호출자는 입력이 비어 있지 않음을 먼저 보장해야 하므로, 이 메서드는 항상 분석 작업을 생성합니다.
	 */
	public AiAnalysis execute(Member member, Conversation conversation, TaskType taskType, String systemPrompt,
			String userPrompt, AiAnalysisTaskContext context) {
		AiAnalysis analysis = lifecycleService.create(member, conversation, taskType);
		lifecycleService.markProcessing(analysis.getId(), openAiChatClient.modelName(), schemaVersion(taskType));
		try {
			String rawResponse = completeWithRetry(systemPrompt, userPrompt);
			persist(analysis.getId(), rawResponse, context);
		} catch (Exception e) {
			lifecycleService.markFailed(analysis.getId(), errorMessage(e));
		}
		return analysis;
	}

	private String schemaVersion(TaskType taskType) {
		return switch (taskType) {
			case HEALTH_EXTRACTION -> "health-extraction-v1";
			case DAILY_SUMMARY -> "daily-summary-v1";
			case WEEKLY_SUMMARY -> "weekly-summary-v1";
			case MONTHLY_SUMMARY -> "monthly-summary-v1";
			case OVERALL_REPORT -> "overall-report-v1";
		};
	}

	private String completeWithRetry(String systemPrompt, String userPrompt) {
		for (int attempt = 1; attempt <= MAX_OPENAI_ATTEMPTS; attempt++) {
			try {
				return openAiChatClient.complete(systemPrompt, userPrompt);
			} catch (RuntimeException e) {
				if (!isRetryable(e) || attempt == MAX_OPENAI_ATTEMPTS) {
					throw e;
				}
				waitBeforeRetry(attempt);
			}
		}
		throw new IllegalStateException("OpenAI completion attempts were exhausted");
	}

	private boolean isRetryable(RuntimeException e) {
		if (e instanceof ResourceAccessException) {
			return true;
		}
		if (e instanceof RestClientResponseException responseException) {
			int status = responseException.getStatusCode().value();
			return status == 408 || status == 429 || responseException.getStatusCode().is5xxServerError();
		}
		return false;
	}

	private void waitBeforeRetry(int completedAttempt) {
		long exponentialBackoffMillis = 1_000L << (completedAttempt - 1);
		long delayMillis = exponentialBackoffMillis + ThreadLocalRandom.current().nextLong(RETRY_JITTER_MILLIS + 1);
		try {
			Thread.sleep(delayMillis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("OpenAI retry was interrupted", e);
		}
	}

	private String conversationTranscript(Conversation conversation) {
		List<ConversationMessage> messages = conversationMessageRepository
				.findByConversationIdOrderBySequenceNoAsc(conversation.getId());
		String transcript = messages.stream()
				.map(message -> message.getRole() + ": " + message.getContent())
				.collect(Collectors.joining("\n"));
		return "Conversation date: " + conversation.getSessionDate() + "\n" + transcript;
	}

	private String healthExtractionSystemPrompt() {
		return "Extract health records from the conversation. Return only a JSON object matching this contract: "
				+ "{schemaVersion:'health-extraction-v1',records:[{type:'SLEEP|MEAL|EXERCISE|SKIN|MOOD|WATER|OTHER',"
				+ "summary:string,detail:object,recordedDate:'YYYY-MM-DD',recordedAt:'ISO-8601 datetime or null',"
				+ "confidence:number from 0 to 1,evidence:string}]}. "
				+ "Use an empty records array when no health record is stated. Detail requirements: "
				+ "SLEEP={hours:number,quality:'good|normal|bad|null',bedtime:'HH:mm|null',wakeTime:'HH:mm|null'}; "
				+ "MEAL={menu:string,timing:'아침|점심|저녁|간식|null',amount:'적음|보통|많음|null'}; "
				+ "EXERCISE={activity:string,duration:number,unit:'min',intensity:'낮음|보통|높음|null'}; "
				+ "SKIN={condition:string,area:string|null}; MOOD={emotion:string,note:string|null}; "
				+ "WATER={amount:number,unit:'ml|cup'}; OTHER={note:string}.";
	}

	private String errorMessage(Exception e) {
		String message = e.getMessage();
		return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
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
		List<HealthRecord> allHealthRecords = healthRecordRepository
				.findByMemberIdOrderByRecordedDateAsc(analysis.getMember().getId());
		if (monthlySummaries.isEmpty() && allHealthRecords.isEmpty()) {
			throw new AiResultValidationException("OVERALL_REPORT는 월간 요약 또는 건강 기록이 있을 때만 생성할 수 있습니다.");
		}

		LocalDate expectedPeriodStart = monthlySummaries.stream().map(MonthlyConversationSummary::getPeriodStart)
				.reduce((left, right) -> left.isBefore(right) ? left : right)
				.orElseGet(() -> allHealthRecords.get(0).getRecordedDate());
		expectedPeriodStart = allHealthRecords.stream().map(HealthRecord::getRecordedDate)
				.reduce(expectedPeriodStart, (left, right) -> left.isBefore(right) ? left : right);
		LocalDate expectedPeriodEnd = monthlySummaries.stream().map(MonthlyConversationSummary::getPeriodEnd)
				.reduce((left, right) -> left.isAfter(right) ? left : right)
				.orElseGet(() -> allHealthRecords.get(allHealthRecords.size() - 1).getRecordedDate());
		expectedPeriodEnd = allHealthRecords.stream().map(HealthRecord::getRecordedDate)
				.reduce(expectedPeriodEnd, (left, right) -> left.isAfter(right) ? left : right);
		Set<HealthType> availableHealthTypes = allHealthRecords.stream().map(HealthRecord::getType)
				.collect(Collectors.toUnmodifiableSet());
		OverallReportResult result = resultParser.parseOverallReport(rawResponse, expectedPeriodStart, expectedPeriodEnd,
				availableHealthTypes);

		LocalDateTime generatedAt = LocalDateTime.now();
		overallReportRepository.findById(analysis.getMember().getId()).ifPresentOrElse(
				report -> report.refresh(result.summary(), serialize(result.detail()), monthlySummaries.size(), generatedAt),
				() -> overallReportRepository.save(OverallReport.builder()
						.member(analysis.getMember()).summary(result.summary()).detail(serialize(result.detail()))
						.monthlySummaryCount(monthlySummaries.size()).generatedAt(generatedAt).build()));
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
