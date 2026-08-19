package com.hackathon.backend.ai.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hackathon.backend.ai.dto.AiAnalysisResponse;
import com.hackathon.backend.ai.entity.AiAnalysis;
import com.hackathon.backend.ai.entity.AiAnalysis.TaskType;
import com.hackathon.backend.common.exception.NotFoundException;
import com.hackathon.backend.conversation.entity.Conversation;
import com.hackathon.backend.conversation.entity.Conversation.ConversationStatus;
import com.hackathon.backend.conversation.entity.ConversationMessage;
import com.hackathon.backend.conversation.repository.ConversationMessageRepository;
import com.hackathon.backend.conversation.repository.ConversationRepository;
import com.hackathon.backend.healthrecord.entity.HealthRecord;
import com.hackathon.backend.healthrecord.entity.HealthRecord.HealthType;
import com.hackathon.backend.healthrecord.repository.HealthRecordRepository;
import com.hackathon.backend.member.entity.Member;
import com.hackathon.backend.member.repository.MemberRepository;
import com.hackathon.backend.summary.entity.DailyConversationSummary;
import com.hackathon.backend.summary.entity.MonthlyConversationSummary;
import com.hackathon.backend.summary.entity.WeeklyConversationSummary;
import com.hackathon.backend.summary.repository.DailyConversationSummaryRepository;
import com.hackathon.backend.summary.repository.MonthlyConversationSummaryRepository;
import com.hackathon.backend.summary.repository.WeeklyConversationSummaryRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * DAILY/WEEKLY/MONTHLY 압축과 온디맨드 OVERALL_REPORT의 입력을 만들고 실행합니다.
 * 입력이 없으면 이 계층에서 반환하므로 AiAnalysis와 AI 호출이 만들어지지 않습니다.
 */
@Service
@Transactional
public class AiSummaryGenerationService {

	private final MemberRepository memberRepository;
	private final ConversationRepository conversationRepository;
	private final ConversationMessageRepository conversationMessageRepository;
	private final DailyConversationSummaryRepository dailySummaryRepository;
	private final WeeklyConversationSummaryRepository weeklySummaryRepository;
	private final MonthlyConversationSummaryRepository monthlySummaryRepository;
	private final HealthRecordRepository healthRecordRepository;
	private final AiAnalysisPipelineService pipelineService;
	private final ObjectMapper objectMapper;

	public AiSummaryGenerationService(MemberRepository memberRepository, ConversationRepository conversationRepository,
			ConversationMessageRepository conversationMessageRepository, DailyConversationSummaryRepository dailySummaryRepository,
			WeeklyConversationSummaryRepository weeklySummaryRepository, MonthlyConversationSummaryRepository monthlySummaryRepository,
			HealthRecordRepository healthRecordRepository, AiAnalysisPipelineService pipelineService, ObjectMapper objectMapper) {
		this.memberRepository = memberRepository;
		this.conversationRepository = conversationRepository;
		this.conversationMessageRepository = conversationMessageRepository;
		this.dailySummaryRepository = dailySummaryRepository;
		this.weeklySummaryRepository = weeklySummaryRepository;
		this.monthlySummaryRepository = monthlySummaryRepository;
		this.healthRecordRepository = healthRecordRepository;
		this.pipelineService = pipelineService;
		this.objectMapper = objectMapper;
	}

	public void generateDailyForAllMembers(LocalDate summaryDate) {
		memberRepository.findAll().forEach(member -> generateDaily(member, summaryDate));
	}

	public void generateWeeklyForAllMembers(LocalDate periodStart) {
		memberRepository.findAll().forEach(member -> generateWeekly(member, periodStart));
	}

	public void generateMonthlyForAllMembers(LocalDate periodStart) {
		memberRepository.findAll().forEach(member -> generateMonthly(member, periodStart));
	}

	/** 사용자 요청으로 종합 리포트를 새로 만들거나 갱신합니다. */
	public AiAnalysisResponse generateOverall(Long memberId) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new NotFoundException("Member not found"));
		List<MonthlyConversationSummary> monthlySummaries = monthlySummaryRepository
				.findByMemberIdOrderByPeriodStartAsc(memberId);
		List<HealthRecord> healthRecords = healthRecordRepository.findByMemberIdOrderByRecordedDateAsc(memberId);
		if (monthlySummaries.isEmpty() && healthRecords.isEmpty()) {
			throw new NotFoundException("종합 리포트를 생성할 데이터가 없습니다.");
		}

		LocalDate periodStart = earliest(monthlySummaries, healthRecords);
		LocalDate periodEnd = latest(monthlySummaries, healthRecords);
		Map<String, Object> input = new LinkedHashMap<>();
		input.put("periodStart", periodStart);
		input.put("periodEnd", periodEnd);
		input.put("monthlySummaries", monthlySummaries.stream()
				.map(summary -> Map.of("periodStart", summary.getPeriodStart(), "summary", summary.getSummary())).toList());
		input.put("healthStats", healthStats(healthRecords));

		AiAnalysis analysis = pipelineService.execute(member, null, TaskType.OVERALL_REPORT, overallSystemPrompt(), json(input),
				new AiAnalysisTaskContext.OverallReport());
		return AiAnalysisResponse.from(analysis);
	}

	private void generateDaily(Member member, LocalDate summaryDate) {
		if (dailySummaryRepository.findByMemberIdAndSummaryDate(member.getId(), summaryDate).isPresent()) {
			return;
		}
		List<Conversation> conversations = conversationRepository.findByMemberIdAndSessionDate(member.getId(), summaryDate).stream()
				.filter(conversation -> conversation.getStatus() == ConversationStatus.COMPLETED).toList();
		if (conversations.isEmpty()) {
			return;
		}

		List<Map<String, Object>> inputConversations = new ArrayList<>();
		int tokenCount = 0;
		for (Conversation conversation : conversations) {
			List<ConversationMessage> messages = conversationMessageRepository
					.findByConversationIdOrderBySequenceNoAsc(conversation.getId()).stream()
					.filter(message -> message.getRole() != ConversationMessage.MessageRole.SYSTEM).toList();
			tokenCount += messages.stream().mapToInt(ConversationMessage::getTokenCount).sum();
			inputConversations.add(Map.of(
					"conversationId", conversation.getId(),
					"type", conversation.getType().name(),
					"messages", messages.stream().map(message -> Map.of(
							"role", message.getRole().name(), "content", message.getContent())).toList()));
		}

		pipelineService.execute(member, null, TaskType.DAILY_SUMMARY, dailySystemPrompt(),
				json(Map.of("summaryDate", summaryDate, "conversations", inputConversations)),
				new AiAnalysisTaskContext.DailySummary(summaryDate, conversations.size(), tokenCount));
	}

	private void generateWeekly(Member member, LocalDate periodStart) {
		if (weeklySummaryRepository.findByMemberIdAndPeriodStart(member.getId(), periodStart).isPresent()) {
			return;
		}
		LocalDate periodEnd = periodStart.plusDays(6);
		List<DailyConversationSummary> dailySummaries = dailySummaryRepository
				.findByMemberIdAndSummaryDateBetweenOrderBySummaryDateAsc(member.getId(), periodStart, periodEnd);
		if (dailySummaries.isEmpty()) {
			return;
		}
		int tokenCount = dailySummaries.stream().mapToInt(DailyConversationSummary::getTokenCount).sum();
		pipelineService.execute(member, null, TaskType.WEEKLY_SUMMARY, weeklySystemPrompt(), json(Map.of(
				"periodStart", periodStart, "periodEnd", periodEnd,
				"dailySummaries", dailySummaries.stream().map(summary -> Map.of(
						"date", summary.getSummaryDate(), "summary", summary.getSummary())).toList())),
				new AiAnalysisTaskContext.WeeklySummary(periodStart, periodEnd, dailySummaries.size(), tokenCount));
	}

	private void generateMonthly(Member member, LocalDate periodStart) {
		if (monthlySummaryRepository.findByMemberIdAndPeriodStart(member.getId(), periodStart).isPresent()) {
			return;
		}
		LocalDate periodEnd = periodStart.with(TemporalAdjusters.lastDayOfMonth());
		List<WeeklyConversationSummary> weeklySummaries = weeklySummaryRepository
				.findByMemberIdAndPeriodStartBetweenOrderByPeriodStartAsc(member.getId(), periodStart, periodEnd);
		if (weeklySummaries.isEmpty()) {
			return;
		}
		int tokenCount = weeklySummaries.stream().mapToInt(WeeklyConversationSummary::getTokenCount).sum();
		pipelineService.execute(member, null, TaskType.MONTHLY_SUMMARY, monthlySystemPrompt(), json(Map.of(
				"periodStart", periodStart, "periodEnd", periodEnd,
				"weeklySummaries", weeklySummaries.stream().map(summary -> Map.of(
						"periodStart", summary.getPeriodStart(), "summary", summary.getSummary())).toList())),
				new AiAnalysisTaskContext.MonthlySummary(periodStart, periodEnd, weeklySummaries.size(), tokenCount));
	}

	private List<Map<String, Object>> healthStats(List<HealthRecord> healthRecords) {
		Map<HealthType, List<HealthRecord>> byType = new EnumMap<>(HealthType.class);
		healthRecords.forEach(record -> byType.computeIfAbsent(record.getType(), ignored -> new ArrayList<>()).add(record));
		return byType.entrySet().stream().map(entry -> {
			List<BigDecimal> values = entry.getValue().stream().map(record -> numericValue(record, entry.getKey()))
					.filter(value -> value != null).toList();
			BigDecimal average = values.isEmpty() ? null : values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
					.divide(BigDecimal.valueOf(values.size()), 2, java.math.RoundingMode.HALF_UP);
			Map<String, Object> stat = new LinkedHashMap<>();
			stat.put("type", entry.getKey().name());
			stat.put("recordCount", entry.getValue().size());
			stat.put("avgValue", average);
			stat.put("avgUnit", averageUnit(entry.getKey()));
			return stat;
		}).toList();
	}

	private BigDecimal numericValue(HealthRecord record, HealthType type) {
		String field = switch (type) {
			case SLEEP -> "hours";
			case EXERCISE -> "duration";
			case WATER -> "amount";
			default -> null;
		};
		if (field == null || record.getDetail() == null) {
			return null;
		}
		try {
			JsonNode value = objectMapper.readTree(record.getDetail()).get(field);
			return value != null && value.isNumber() ? value.decimalValue() : null;
		} catch (Exception e) {
			throw new IllegalStateException("HealthRecord detail is not valid JSON", e);
		}
	}

	private String averageUnit(HealthType type) {
		return switch (type) {
			case SLEEP -> "hours";
			case EXERCISE -> "min";
			case WATER -> "ml";
			default -> null;
		};
	}

	private LocalDate earliest(List<MonthlyConversationSummary> monthlySummaries, List<HealthRecord> healthRecords) {
		return java.util.stream.Stream.concat(monthlySummaries.stream().map(MonthlyConversationSummary::getPeriodStart),
				healthRecords.stream().map(HealthRecord::getRecordedDate)).min(Comparator.naturalOrder()).orElseThrow();
	}

	private LocalDate latest(List<MonthlyConversationSummary> monthlySummaries, List<HealthRecord> healthRecords) {
		return java.util.stream.Stream.concat(monthlySummaries.stream().map(MonthlyConversationSummary::getPeriodEnd),
				healthRecords.stream().map(HealthRecord::getRecordedDate)).max(Comparator.naturalOrder()).orElseThrow();
	}

	private String json(Object input) {
		try {
			return objectMapper.writeValueAsString(input);
		} catch (Exception e) {
			throw new IllegalStateException("AI input serialization failed", e);
		}
	}

	private String dailySystemPrompt() {
		return "Summarize the supplied elderly-care conversations factually. Prioritize health, activities, and mood. "
				+ "Return JSON only: {schemaVersion:'daily-summary-v1',summary:string}.";
	}

	private String weeklySystemPrompt() {
		return "Compress the supplied daily summaries factually. Return JSON only: "
				+ "{schemaVersion:'weekly-summary-v1',summary:string}.";
	}

	private String monthlySystemPrompt() {
		return "Compress the supplied weekly summaries factually. Return JSON only: "
				+ "{schemaVersion:'monthly-summary-v1',summary:string}.";
	}

	private String overallSystemPrompt() {
		return "Create an elderly-care overall report from the supplied monthly summaries and health statistics. "
				+ "Do not diagnose or prescribe. Use only health types present in healthStats; use improving/worsening only with at least three months of evidence. "
				+ "Return JSON only matching overall-report-v1, including detail.period, highlights, healthTrends, and recommendations.";
	}
}
