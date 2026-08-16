package com.hackathon.backend.summary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.hackathon.backend.common.exception.NotFoundException;
import com.hackathon.backend.member.entity.Member;
import com.hackathon.backend.summary.dto.DailySummaryResponse;
import com.hackathon.backend.summary.dto.OverallReportResponse;
import com.hackathon.backend.summary.dto.MonthlySummaryResponse;
import com.hackathon.backend.summary.dto.WeeklySummaryResponse;
import com.hackathon.backend.summary.entity.DailyConversationSummary;
import com.hackathon.backend.summary.entity.MonthlyConversationSummary;
import com.hackathon.backend.summary.entity.OverallReport;
import com.hackathon.backend.summary.entity.WeeklyConversationSummary;

/**
 * 요약·종합 리포트 조회 서비스를 실제 리포지토리(H2) 위에서 검증합니다.
 *
 * 이 테스트가 지키려는 것은 두 가지입니다.
 * 1) 요약이 없으면 빈 객체가 아니라 NotFoundException 을 던진다 — "아직 생성 전"과
 *    "생성됐지만 내용이 비었음"을 프론트가 구분해야 하기 때문입니다 (스펙 §3.3).
 * 2) date/periodStart 를 생략했을 때의 기본값(오늘, 이번 주 월요일, 이번 달 1일)이
 *    실제로 그 날짜의 데이터를 찾아낸다.
 */
@DataJpaTest
@Import(SummaryService.class)
class SummaryServiceTest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	@Autowired
	private SummaryService summaryService;

	@Autowired
	private TestEntityManager entityManager;

	private Member persistMember(String phone) {
		return entityManager.persist(Member.builder()
				.nickname("회원")
				.phone(phone)
				.build());
	}

	private LocalDate today() {
		return LocalDate.now(KST);
	}

	private LocalDate thisWeekMonday() {
		return LocalDate.now(KST).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
	}

	private LocalDate thisMonthFirstDay() {
		return LocalDate.now(KST).withDayOfMonth(1);
	}

	@Nested
	@DisplayName("findDaily")
	class FindDaily {

		@Test
		@DisplayName("날짜를 지정하면 해당 날짜의 일일 요약을 반환합니다")
		void returnsSummaryForGivenDate() {
			Member member = persistMember("010-1000-0001");
			LocalDate date = LocalDate.of(2026, 8, 1);
			entityManager.persist(DailyConversationSummary.builder()
					.member(member).summaryDate(date).summary("8월 1일 요약")
					.conversationCount(3).tokenCount(1500).build());
			entityManager.flush();
			entityManager.clear();

			DailySummaryResponse response = summaryService.findDaily(member.getId(), date);

			assertThat(response.memberId()).isEqualTo(member.getId());
			assertThat(response.summaryDate()).isEqualTo(date);
			assertThat(response.summary()).isEqualTo("8월 1일 요약");
			assertThat(response.conversationCount()).isEqualTo(3);
			assertThat(response.tokenCount()).isEqualTo(1500);
		}

		@Test
		@DisplayName("date 를 생략하면 오늘(KST) 요약을 반환합니다")
		void defaultsToToday() {
			Member member = persistMember("010-1000-0002");
			entityManager.persist(DailyConversationSummary.builder()
					.member(member).summaryDate(today()).summary("오늘 요약").build());
			entityManager.flush();
			entityManager.clear();

			DailySummaryResponse response = summaryService.findDaily(member.getId(), null);

			assertThat(response.summaryDate()).isEqualTo(today());
			assertThat(response.summary()).isEqualTo("오늘 요약");
		}

		@Test
		@DisplayName("다른 회원의 같은 날짜 요약과 섞이지 않습니다")
		void doesNotMixOtherMembers() {
			Member member1 = persistMember("010-1000-0003");
			Member member2 = persistMember("010-1000-0004");
			LocalDate date = LocalDate.of(2026, 8, 1);
			entityManager.persist(DailyConversationSummary.builder()
					.member(member1).summaryDate(date).summary("회원1 요약").build());
			entityManager.persist(DailyConversationSummary.builder()
					.member(member2).summaryDate(date).summary("회원2 요약").build());
			entityManager.flush();
			entityManager.clear();

			DailySummaryResponse response = summaryService.findDaily(member1.getId(), date);

			assertThat(response.summary()).isEqualTo("회원1 요약");
		}

		@Test
		@DisplayName("일일 요약이 없으면 NotFoundException 을 던집니다")
		void throwsNotFoundWhenMissing() {
			Member member = persistMember("010-1000-0005");
			entityManager.flush();
			entityManager.clear();

			assertThatThrownBy(() -> summaryService.findDaily(member.getId(), LocalDate.of(2026, 8, 1)))
					.isInstanceOf(NotFoundException.class);
		}
	}

	@Nested
	@DisplayName("findWeekly")
	class FindWeekly {

		@Test
		@DisplayName("기간 시작일을 지정하면 해당 주간 요약을 반환합니다")
		void returnsSummaryForGivenPeriod() {
			Member member = persistMember("010-1001-0001");
			LocalDate periodStart = LocalDate.of(2026, 8, 10);
			entityManager.persist(WeeklyConversationSummary.builder()
					.member(member).periodStart(periodStart).periodEnd(LocalDate.of(2026, 8, 16))
					.summary("주간 요약").dailySummaryCount(7).tokenCount(9800).build());
			entityManager.flush();
			entityManager.clear();

			WeeklySummaryResponse response = summaryService.findWeekly(member.getId(), periodStart);

			assertThat(response.memberId()).isEqualTo(member.getId());
			assertThat(response.periodStart()).isEqualTo(periodStart);
			assertThat(response.periodEnd()).isEqualTo(LocalDate.of(2026, 8, 16));
			assertThat(response.summary()).isEqualTo("주간 요약");
			assertThat(response.tokenCount()).isEqualTo(9800);
		}

		@Test
		@DisplayName("periodStart 를 생략하면 이번 주 월요일 기준으로 조회합니다")
		void defaultsToThisWeekMonday() {
			Member member = persistMember("010-1001-0002");
			LocalDate monday = thisWeekMonday();
			entityManager.persist(WeeklyConversationSummary.builder()
					.member(member).periodStart(monday).periodEnd(monday.plusDays(6))
					.summary("이번 주 요약").build());
			entityManager.flush();
			entityManager.clear();

			WeeklySummaryResponse response = summaryService.findWeekly(member.getId(), null);

			assertThat(response.periodStart()).isEqualTo(monday);
			assertThat(response.summary()).isEqualTo("이번 주 요약");
		}

		@Test
		@DisplayName("dailySummaryCount 는 엔티티의 dailySummaryCount 를 담습니다")
		void mapsSourceCountFromDailySummaryCount() {
			Member member = persistMember("010-1001-0003");
			LocalDate periodStart = LocalDate.of(2026, 8, 10);
			entityManager.persist(WeeklyConversationSummary.builder()
					.member(member).periodStart(periodStart).periodEnd(LocalDate.of(2026, 8, 16))
					.summary("주간 요약").dailySummaryCount(5).build());
			entityManager.flush();
			entityManager.clear();

			WeeklySummaryResponse response = summaryService.findWeekly(member.getId(), periodStart);

			// 엔티티 필드명은 dailySummaryCount 이고 응답 필드명도 dailySummaryCount 로 그대로 나갑니다.
			assertThat(response.dailySummaryCount()).isEqualTo(5);
		}

		@Test
		@DisplayName("주간 요약이 없으면 NotFoundException 을 던집니다")
		void throwsNotFoundWhenMissing() {
			Member member = persistMember("010-1001-0004");
			entityManager.flush();
			entityManager.clear();

			assertThatThrownBy(() -> summaryService.findWeekly(member.getId(), LocalDate.of(2026, 8, 10)))
					.isInstanceOf(NotFoundException.class);
		}
	}

	@Nested
	@DisplayName("findMonthly")
	class FindMonthly {

		@Test
		@DisplayName("기간 시작일을 지정하면 해당 월간 요약을 반환합니다")
		void returnsSummaryForGivenPeriod() {
			Member member = persistMember("010-1002-0001");
			LocalDate periodStart = LocalDate.of(2026, 8, 1);
			entityManager.persist(MonthlyConversationSummary.builder()
					.member(member).periodStart(periodStart).periodEnd(LocalDate.of(2026, 8, 31))
					.summary("월간 요약").weeklySummaryCount(4).tokenCount(30000).build());
			entityManager.flush();
			entityManager.clear();

			MonthlySummaryResponse response = summaryService.findMonthly(member.getId(), periodStart);

			assertThat(response.memberId()).isEqualTo(member.getId());
			assertThat(response.periodStart()).isEqualTo(periodStart);
			assertThat(response.periodEnd()).isEqualTo(LocalDate.of(2026, 8, 31));
			assertThat(response.summary()).isEqualTo("월간 요약");
			assertThat(response.tokenCount()).isEqualTo(30000);
		}

		@Test
		@DisplayName("periodStart 를 생략하면 이번 달 1일 기준으로 조회합니다")
		void defaultsToThisMonthFirstDay() {
			Member member = persistMember("010-1002-0002");
			LocalDate firstDay = thisMonthFirstDay();
			entityManager.persist(MonthlyConversationSummary.builder()
					.member(member).periodStart(firstDay).periodEnd(firstDay.plusMonths(1).minusDays(1))
					.summary("이번 달 요약").build());
			entityManager.flush();
			entityManager.clear();

			MonthlySummaryResponse response = summaryService.findMonthly(member.getId(), null);

			assertThat(response.periodStart()).isEqualTo(firstDay);
			assertThat(response.summary()).isEqualTo("이번 달 요약");
		}

		@Test
		@DisplayName("weeklySummaryCount 는 엔티티의 weeklySummaryCount 를 담습니다")
		void mapsSourceCountFromWeeklySummaryCount() {
			Member member = persistMember("010-1002-0003");
			LocalDate periodStart = LocalDate.of(2026, 8, 1);
			entityManager.persist(MonthlyConversationSummary.builder()
					.member(member).periodStart(periodStart).periodEnd(LocalDate.of(2026, 8, 31))
					.summary("월간 요약").weeklySummaryCount(3).build());
			entityManager.flush();
			entityManager.clear();

			MonthlySummaryResponse response = summaryService.findMonthly(member.getId(), periodStart);

			// 엔티티 필드명은 weeklySummaryCount 이고 응답 필드명도 weeklySummaryCount 로 그대로 나갑니다.
			assertThat(response.weeklySummaryCount()).isEqualTo(3);
		}

		@Test
		@DisplayName("월간 요약이 없으면 NotFoundException 을 던집니다")
		void throwsNotFoundWhenMissing() {
			Member member = persistMember("010-1002-0004");
			entityManager.flush();
			entityManager.clear();

			assertThatThrownBy(() -> summaryService.findMonthly(member.getId(), LocalDate.of(2026, 8, 1)))
					.isInstanceOf(NotFoundException.class);
		}
	}

	@Nested
	@DisplayName("findOverall")
	class FindOverall {

		@Test
		@DisplayName("회원의 종합 리포트를 반환합니다")
		void returnsReport() {
			Member member = persistMember("010-1003-0001");
			entityManager.persist(OverallReport.builder()
					.member(member).summary("종합 리포트").detail("{\"score\":80}")
					.monthlySummaryCount(3).build());
			entityManager.flush();
			entityManager.clear();

			OverallReportResponse response = summaryService.findOverall(member.getId());

			assertThat(response.memberId()).isEqualTo(member.getId());
			assertThat(response.summary()).isEqualTo("종합 리포트");
			assertThat(response.detail()).isEqualTo("{\"score\":80}");
			assertThat(response.monthlySummaryCount()).isEqualTo(3);
		}

		@Test
		@DisplayName("다른 회원의 리포트와 섞이지 않습니다")
		void doesNotMixOtherMembers() {
			Member member1 = persistMember("010-1003-0002");
			Member member2 = persistMember("010-1003-0003");
			entityManager.persist(OverallReport.builder().member(member1).summary("회원1 리포트").build());
			entityManager.persist(OverallReport.builder().member(member2).summary("회원2 리포트").build());
			entityManager.flush();
			entityManager.clear();

			OverallReportResponse response = summaryService.findOverall(member1.getId());

			assertThat(response.summary()).isEqualTo("회원1 리포트");
		}

		@Test
		@DisplayName("종합 리포트가 없으면 NotFoundException 을 던집니다")
		void throwsNotFoundWhenMissing() {
			Member member = persistMember("010-1003-0004");
			entityManager.flush();
			entityManager.clear();

			assertThatThrownBy(() -> summaryService.findOverall(member.getId()))
					.isInstanceOf(NotFoundException.class);
		}

		@Test
		@DisplayName("detail 이 빈 문자열이면 null 로 정규화합니다")
		void normalizesBlankDetailToNull() {
			Member member = persistMember("010-1003-0005");
			entityManager.persist(OverallReport.builder()
					.member(member).summary("종합 리포트").detail("").build());
			entityManager.flush();
			entityManager.clear();

			OverallReportResponse response = summaryService.findOverall(member.getId());

			// 빈 문자열이 그대로 나가면 @JsonRawValue 가 "detail": 뒤에 아무것도 쓰지
			// 않아 응답 전체가 파싱 불가능한 JSON 이 됩니다.
			assertThat(response.detail()).isNull();
		}
	}
}
