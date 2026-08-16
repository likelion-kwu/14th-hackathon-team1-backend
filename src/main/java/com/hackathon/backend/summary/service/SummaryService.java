package com.hackathon.backend.summary.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hackathon.backend.common.exception.NotFoundException;
import com.hackathon.backend.summary.dto.DailySummaryResponse;
import com.hackathon.backend.summary.dto.MonthlySummaryResponse;
import com.hackathon.backend.summary.dto.OverallReportResponse;
import com.hackathon.backend.summary.dto.WeeklySummaryResponse;
import com.hackathon.backend.summary.repository.DailyConversationSummaryRepository;
import com.hackathon.backend.summary.repository.MonthlyConversationSummaryRepository;
import com.hackathon.backend.summary.repository.OverallReportRepository;
import com.hackathon.backend.summary.repository.WeeklyConversationSummaryRepository;

/**
 * 대화 요약·종합 리포트 조회를 담당합니다.
 *
 * 4종 모두 조회 전용입니다. 요약은 AI 압축 파이프라인이 주기적으로 미리 만들어
 * 두는 것이라, 이 서비스가 호출된 시점에 해당 기간 요약이 아직 없을 수 있습니다.
 * 그 경우 빈 객체 대신 NotFoundException 을 던집니다 — "아직 생성 전"과 "생성됐지만
 * 내용이 비었음"을 프론트가 구분할 수 있어야 하기 때문입니다.
 *
 * 조회 메서드에는 readOnly 를 붙입니다. open-in-view 가 꺼져 있으므로 DTO 변환은
 * 반드시 이 트랜잭션 안에서 끝내야 합니다.
 */
@Service
@Transactional(readOnly = true)
public class SummaryService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final DailyConversationSummaryRepository dailyConversationSummaryRepository;
	private final WeeklyConversationSummaryRepository weeklyConversationSummaryRepository;
	private final MonthlyConversationSummaryRepository monthlyConversationSummaryRepository;
	private final OverallReportRepository overallReportRepository;

	public SummaryService(
			DailyConversationSummaryRepository dailyConversationSummaryRepository,
			WeeklyConversationSummaryRepository weeklyConversationSummaryRepository,
			MonthlyConversationSummaryRepository monthlyConversationSummaryRepository,
			OverallReportRepository overallReportRepository) {
		this.dailyConversationSummaryRepository = dailyConversationSummaryRepository;
		this.weeklyConversationSummaryRepository = weeklyConversationSummaryRepository;
		this.monthlyConversationSummaryRepository = monthlyConversationSummaryRepository;
		this.overallReportRepository = overallReportRepository;
	}

	/**
	 * date 가 없으면 오늘(KST)을 씁니다. 서버 기본 타임존에 맡기면 배포 환경에 따라
	 * "오늘"이 달라질 수 있어 여기서 명시적으로 고정합니다.
	 */
	public DailySummaryResponse findDaily(Long memberId, LocalDate date) {
		LocalDate targetDate = date != null ? date : LocalDate.now(KST);
		return dailyConversationSummaryRepository.findByMemberIdAndSummaryDate(memberId, targetDate)
				.map(DailySummaryResponse::from)
				.orElseThrow(() -> new NotFoundException("일일 요약을 찾을 수 없습니다."));
	}

	/**
	 * periodStart 가 없으면 이번 주 월요일(KST)을 씁니다.
	 */
	public WeeklySummaryResponse findWeekly(Long memberId, LocalDate periodStart) {
		LocalDate targetPeriodStart = periodStart != null ? periodStart : thisWeekMonday();
		return weeklyConversationSummaryRepository.findByMemberIdAndPeriodStart(memberId, targetPeriodStart)
				.map(WeeklySummaryResponse::from)
				.orElseThrow(() -> new NotFoundException("주간 요약을 찾을 수 없습니다."));
	}

	/**
	 * periodStart 가 없으면 이번 달 1일(KST)을 씁니다.
	 */
	public MonthlySummaryResponse findMonthly(Long memberId, LocalDate periodStart) {
		LocalDate targetPeriodStart = periodStart != null ? periodStart : thisMonthFirstDay();
		return monthlyConversationSummaryRepository.findByMemberIdAndPeriodStart(memberId, targetPeriodStart)
				.map(MonthlySummaryResponse::from)
				.orElseThrow(() -> new NotFoundException("월간 요약을 찾을 수 없습니다."));
	}

	/**
	 * OverallReport 는 회원당 1건이라 조회 키가 memberId 하나뿐입니다.
	 */
	public OverallReportResponse findOverall(Long memberId) {
		return overallReportRepository.findById(memberId)
				.map(OverallReportResponse::from)
				.orElseThrow(() -> new NotFoundException("종합 리포트를 찾을 수 없습니다."));
	}

	private LocalDate thisWeekMonday() {
		return LocalDate.now(KST).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
	}

	private LocalDate thisMonthFirstDay() {
		return LocalDate.now(KST).withDayOfMonth(1);
	}
}
