package com.hackathon.backend.summary.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import com.hackathon.backend.member.entity.Member;
import com.hackathon.backend.summary.entity.DailyConversationSummary;
import com.hackathon.backend.summary.entity.MonthlyConversationSummary;
import com.hackathon.backend.summary.entity.OverallReport;
import com.hackathon.backend.summary.entity.WeeklyConversationSummary;

/**
 * 요약·리포트 4종 리포지토리의 조회 메서드와 제약 조건을 확인합니다.
 *
 * 저장 후에는 반드시 flush + clear 를 합니다. @DataJpaTest 는 트랜잭션 안에서 돌기
 * 때문에 clear 없이 조회하면 SELECT 가 나가지 않고 1차 캐시의 같은 인스턴스가
 * 돌아와, 컬럼 매핑을 전혀 검증하지 못합니다 (ItemRepositoryTest 주석과 같은 이유).
 */
@DataJpaTest
class SummaryRepositoryTest {

	@Autowired
	private DailyConversationSummaryRepository dailyConversationSummaryRepository;

	@Autowired
	private WeeklyConversationSummaryRepository weeklyConversationSummaryRepository;

	@Autowired
	private MonthlyConversationSummaryRepository monthlyConversationSummaryRepository;

	@Autowired
	private OverallReportRepository overallReportRepository;

	@Autowired
	private TestEntityManager entityManager;

	private Member persistMember(String phone) {
		Member member = entityManager.persist(Member.builder()
				.nickname("회원")
				.phone(phone)
				.build());
		return member;
	}

	@Nested
	@DisplayName("DailyConversationSummaryRepository")
	class DailyConversationSummaryRepositoryTest {

		@Test
		@DisplayName("회원과 날짜로 일일 요약을 조회합니다")
		void findsByMemberIdAndSummaryDate() {
			Member member = persistMember("010-0000-0001");
			entityManager.persist(DailyConversationSummary.builder()
					.member(member)
					.summaryDate(LocalDate.of(2026, 8, 16))
					.summary("오늘 요약")
					.conversationCount(2)
					.tokenCount(1200)
					.build());
			entityManager.flush();
			entityManager.clear();

			DailyConversationSummary found = dailyConversationSummaryRepository
					.findByMemberIdAndSummaryDate(member.getId(), LocalDate.of(2026, 8, 16))
					.orElseThrow();

			assertThat(found.getSummary()).isEqualTo("오늘 요약");
			assertThat(found.getConversationCount()).isEqualTo(2);
			assertThat(found.getTokenCount()).isEqualTo(1200);
		}

		@Test
		@DisplayName("다른 회원의 같은 날짜 요약은 섞이지 않습니다")
		void doesNotMixOtherMembers() {
			Member member1 = persistMember("010-0000-0002");
			Member member2 = persistMember("010-0000-0003");
			LocalDate date = LocalDate.of(2026, 8, 16);
			entityManager.persist(DailyConversationSummary.builder()
					.member(member1).summaryDate(date).summary("회원1 요약").build());
			entityManager.persist(DailyConversationSummary.builder()
					.member(member2).summaryDate(date).summary("회원2 요약").build());
			entityManager.flush();
			entityManager.clear();

			DailyConversationSummary found = dailyConversationSummaryRepository
					.findByMemberIdAndSummaryDate(member1.getId(), date)
					.orElseThrow();

			assertThat(found.getSummary()).isEqualTo("회원1 요약");
		}

		@Test
		@DisplayName("같은 회원·같은 날짜로 두 번 저장하면 거부합니다")
		void rejectsDuplicateMemberAndDate() {
			Member member = persistMember("010-0000-0004");
			LocalDate date = LocalDate.of(2026, 8, 16);
			dailyConversationSummaryRepository.saveAndFlush(DailyConversationSummary.builder()
					.member(member).summaryDate(date).summary("첫 번째").build());

			assertThatThrownBy(() -> dailyConversationSummaryRepository.saveAndFlush(
					DailyConversationSummary.builder().member(member).summaryDate(date).summary("두 번째").build()))
					.isInstanceOf(DataIntegrityViolationException.class);
		}

		@Test
		@DisplayName("데이터가 없으면 빈 결과를 반환합니다")
		void returnsEmptyWhenNotFound() {
			Member member = persistMember("010-0000-0005");
			entityManager.flush();
			entityManager.clear();

			assertThat(dailyConversationSummaryRepository
					.findByMemberIdAndSummaryDate(member.getId(), LocalDate.of(2026, 8, 16)))
					.isEmpty();
		}
	}

	@Nested
	@DisplayName("WeeklyConversationSummaryRepository")
	class WeeklyConversationSummaryRepositoryTest {

		@Test
		@DisplayName("회원과 기간 시작일로 주간 요약을 조회합니다")
		void findsByMemberIdAndPeriodStart() {
			Member member = persistMember("010-0001-0001");
			entityManager.persist(WeeklyConversationSummary.builder()
					.member(member)
					.periodStart(LocalDate.of(2026, 8, 10))
					.periodEnd(LocalDate.of(2026, 8, 16))
					.summary("주간 요약")
					.dailySummaryCount(7)
					.tokenCount(9800)
					.build());
			entityManager.flush();
			entityManager.clear();

			WeeklyConversationSummary found = weeklyConversationSummaryRepository
					.findByMemberIdAndPeriodStart(member.getId(), LocalDate.of(2026, 8, 10))
					.orElseThrow();

			assertThat(found.getSummary()).isEqualTo("주간 요약");
			assertThat(found.getPeriodEnd()).isEqualTo(LocalDate.of(2026, 8, 16));
			assertThat(found.getDailySummaryCount()).isEqualTo(7);
		}

		@Test
		@DisplayName("다른 회원·다른 기간의 요약과 섞이지 않습니다")
		void doesNotMixOtherMembersOrPeriods() {
			Member member1 = persistMember("010-0001-0002");
			Member member2 = persistMember("010-0001-0003");
			entityManager.persist(WeeklyConversationSummary.builder()
					.member(member1).periodStart(LocalDate.of(2026, 8, 10)).periodEnd(LocalDate.of(2026, 8, 16))
					.summary("회원1 8월 2주").build());
			entityManager.persist(WeeklyConversationSummary.builder()
					.member(member1).periodStart(LocalDate.of(2026, 8, 3)).periodEnd(LocalDate.of(2026, 8, 9))
					.summary("회원1 8월 1주").build());
			entityManager.persist(WeeklyConversationSummary.builder()
					.member(member2).periodStart(LocalDate.of(2026, 8, 10)).periodEnd(LocalDate.of(2026, 8, 16))
					.summary("회원2 8월 2주").build());
			entityManager.flush();
			entityManager.clear();

			WeeklyConversationSummary found = weeklyConversationSummaryRepository
					.findByMemberIdAndPeriodStart(member1.getId(), LocalDate.of(2026, 8, 10))
					.orElseThrow();

			assertThat(found.getSummary()).isEqualTo("회원1 8월 2주");
		}

		@Test
		@DisplayName("같은 회원·같은 기간 시작일로 두 번 저장하면 거부합니다")
		void rejectsDuplicateMemberAndPeriod() {
			Member member = persistMember("010-0001-0004");
			LocalDate periodStart = LocalDate.of(2026, 8, 10);
			weeklyConversationSummaryRepository.saveAndFlush(WeeklyConversationSummary.builder()
					.member(member).periodStart(periodStart).periodEnd(LocalDate.of(2026, 8, 16))
					.summary("첫 번째").build());

			assertThatThrownBy(() -> weeklyConversationSummaryRepository.saveAndFlush(
					WeeklyConversationSummary.builder().member(member).periodStart(periodStart)
							.periodEnd(LocalDate.of(2026, 8, 16)).summary("두 번째").build()))
					.isInstanceOf(DataIntegrityViolationException.class);
		}
	}

	@Nested
	@DisplayName("MonthlyConversationSummaryRepository")
	class MonthlyConversationSummaryRepositoryTest {

		@Test
		@DisplayName("회원과 기간 시작일로 월간 요약을 조회합니다")
		void findsByMemberIdAndPeriodStart() {
			Member member = persistMember("010-0002-0001");
			entityManager.persist(MonthlyConversationSummary.builder()
					.member(member)
					.periodStart(LocalDate.of(2026, 8, 1))
					.periodEnd(LocalDate.of(2026, 8, 31))
					.summary("월간 요약")
					.weeklySummaryCount(4)
					.tokenCount(30000)
					.build());
			entityManager.flush();
			entityManager.clear();

			MonthlyConversationSummary found = monthlyConversationSummaryRepository
					.findByMemberIdAndPeriodStart(member.getId(), LocalDate.of(2026, 8, 1))
					.orElseThrow();

			assertThat(found.getSummary()).isEqualTo("월간 요약");
			assertThat(found.getPeriodEnd()).isEqualTo(LocalDate.of(2026, 8, 31));
			assertThat(found.getWeeklySummaryCount()).isEqualTo(4);
		}

		@Test
		@DisplayName("다른 회원의 같은 기간 요약과 섞이지 않습니다")
		void doesNotMixOtherMembers() {
			Member member1 = persistMember("010-0002-0002");
			Member member2 = persistMember("010-0002-0003");
			LocalDate periodStart = LocalDate.of(2026, 8, 1);
			entityManager.persist(MonthlyConversationSummary.builder()
					.member(member1).periodStart(periodStart).periodEnd(LocalDate.of(2026, 8, 31))
					.summary("회원1 8월").build());
			entityManager.persist(MonthlyConversationSummary.builder()
					.member(member2).periodStart(periodStart).periodEnd(LocalDate.of(2026, 8, 31))
					.summary("회원2 8월").build());
			entityManager.flush();
			entityManager.clear();

			MonthlyConversationSummary found = monthlyConversationSummaryRepository
					.findByMemberIdAndPeriodStart(member1.getId(), periodStart)
					.orElseThrow();

			assertThat(found.getSummary()).isEqualTo("회원1 8월");
		}

		@Test
		@DisplayName("같은 회원·같은 기간 시작일로 두 번 저장하면 거부합니다")
		void rejectsDuplicateMemberAndPeriod() {
			Member member = persistMember("010-0002-0004");
			LocalDate periodStart = LocalDate.of(2026, 8, 1);
			monthlyConversationSummaryRepository.saveAndFlush(MonthlyConversationSummary.builder()
					.member(member).periodStart(periodStart).periodEnd(LocalDate.of(2026, 8, 31))
					.summary("첫 번째").build());

			assertThatThrownBy(() -> monthlyConversationSummaryRepository.saveAndFlush(
					MonthlyConversationSummary.builder().member(member).periodStart(periodStart)
							.periodEnd(LocalDate.of(2026, 8, 31)).summary("두 번째").build()))
					.isInstanceOf(DataIntegrityViolationException.class);
		}
	}

	@Nested
	@DisplayName("OverallReportRepository")
	class OverallReportRepositoryTest {

		@Test
		@DisplayName("회원 id 로 종합 리포트를 조회합니다")
		void findsById() {
			Member member = persistMember("010-0003-0001");
			entityManager.persist(OverallReport.builder()
					.member(member)
					.summary("종합 리포트 본문")
					.monthlySummaryCount(3)
					.build());
			entityManager.flush();
			entityManager.clear();

			OverallReport found = overallReportRepository.findById(member.getId()).orElseThrow();

			assertThat(found.getSummary()).isEqualTo("종합 리포트 본문");
			assertThat(found.getMonthlySummaryCount()).isEqualTo(3);
			assertThat(found.getMemberId()).isEqualTo(member.getId());
		}

		@Test
		@DisplayName("detail 에 넣은 JSON 문자열이 재조회 후에도 보존됩니다")
		void preservesDetailJsonString() {
			Member member = persistMember("010-0003-0002");
			String detailJson = "{\"bedtime\":\"03:00\",\"quality\":\"poor\"}";
			entityManager.persist(OverallReport.builder()
					.member(member)
					.summary("종합 리포트 본문")
					.detail(detailJson)
					.build());
			entityManager.flush();
			entityManager.clear();

			OverallReport found = overallReportRepository.findById(member.getId()).orElseThrow();

			assertThat(found.getDetail()).isEqualTo(detailJson);
		}

		@Test
		@DisplayName("회원당 리포트는 1건만 존재합니다 (MapsId 로 PK 를 공유)")
		void isOneToOneWithMember() {
			Member member = persistMember("010-0003-0003");
			OverallReport saved = entityManager.persist(OverallReport.builder()
					.member(member)
					.summary("첫 리포트")
					.build());
			entityManager.flush();

			// PK 자체가 memberId 이므로 같은 회원으로 새 엔티티를 또 저장하려 하면
			// 별도 행이 아니라 PK 중복으로 거부됩니다.
			assertThat(saved.getMemberId()).isEqualTo(member.getId());
			assertThatThrownBy(() -> {
				entityManager.persist(OverallReport.builder().member(member).summary("두 번째 리포트").build());
				entityManager.flush();
			}).isInstanceOf(RuntimeException.class);
		}
	}
}
