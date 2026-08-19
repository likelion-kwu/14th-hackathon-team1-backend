package com.hackathon.backend.ai.scheduler;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hackathon.backend.ai.service.AiSummaryGenerationService;

/** KST 기준으로 완료된 원천 데이터만 압축합니다. OVERALL_REPORT는 온디맨드 전용입니다. */
@Component
public class AiSummaryScheduler {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private final AiSummaryGenerationService summaryGenerationService;

	public AiSummaryScheduler(AiSummaryGenerationService summaryGenerationService) {
		this.summaryGenerationService = summaryGenerationService;
	}

	@Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
	public void generateDaily() {
		summaryGenerationService.generateDailyForAllMembers(LocalDate.now(KST).minusDays(1));
	}

	@Scheduled(cron = "0 25 0 * * MON", zone = "Asia/Seoul")
	public void generateWeekly() {
		LocalDate previousWeekMonday = LocalDate.now(KST).with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
		summaryGenerationService.generateWeeklyForAllMembers(previousWeekMonday);
	}

	@Scheduled(cron = "0 40 0 1 * *", zone = "Asia/Seoul")
	public void generateMonthly() {
		LocalDate previousMonthFirstDay = LocalDate.now(KST).minusMonths(1).withDayOfMonth(1);
		summaryGenerationService.generateMonthlyForAllMembers(previousMonthFirstDay);
	}
}
