package com.hackathon.backend.healthrecord.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.hackathon.backend.healthrecord.entity.HealthRecord;
import com.hackathon.backend.member.entity.Member;

/**
 * findByMemberIdAndRecordedDate 가 회원·날짜로 정확히 거르는지만 확인합니다.
 *
 * 이 리포지토리는 대표 기록 선정이나 타입별 필터링을 하지 않습니다(클래스
 * Javadoc 참고). 그런 가공이 없다는 것, 즉 조건에 맞는 모든 행을 가공 없이
 * 있는 그대로 반환한다는 계약을 지키는 테스트입니다.
 *
 * 저장 후에는 반드시 flush + clear 를 합니다. @DataJpaTest 는 트랜잭션 안에서
 * 돌기 때문에 clear 없이 조회하면 SELECT 가 나가지 않고 1차 캐시의 같은
 * 인스턴스가 돌아와, 컬럼 매핑을 전혀 검증하지 못합니다.
 *
 * detail 왕복 보존도 함께 확인합니다. 이 컬럼은 한때 JSON 타입으로 선언돼 있어
 * 저장한 원문과 조회 값이 달라지는 결함이 있었습니다. TEXT 로 바꿔 해결했고,
 * 누군가 다시 JSON 으로 되돌리면 이 테스트가 먼저 깨집니다.
 */
@DataJpaTest
class HealthRecordRepositoryTest {

	@Autowired
	private HealthRecordRepository healthRecordRepository;

	@Autowired
	private TestEntityManager entityManager;

	private Member persistMember(String phone) {
		return entityManager.persist(Member.builder()
				.nickname("회원")
				.phone(phone)
				.build());
	}

	private HealthRecord newRecord(Member member, LocalDate recordedDate, String summary) {
		return HealthRecord.builder()
				.member(member)
				.type(HealthRecord.HealthType.SLEEP)
				.summary(summary)
				.recordedDate(recordedDate)
				.build();
	}

	@Test
	@DisplayName("회원과 날짜가 모두 일치하는 기록만 반환합니다")
	void findsByMemberIdAndRecordedDate() {
		Member member1 = persistMember("010-2000-0001");
		Member member2 = persistMember("010-2000-0002");
		LocalDate target = LocalDate.of(2026, 8, 16);
		LocalDate otherDate = LocalDate.of(2026, 8, 15);

		entityManager.persist(newRecord(member1, target, "회원1 오늘 기록"));
		entityManager.persist(newRecord(member1, otherDate, "회원1 어제 기록"));
		entityManager.persist(newRecord(member2, target, "회원2 오늘 기록"));
		entityManager.flush();
		entityManager.clear();

		List<HealthRecord> found = healthRecordRepository.findByMemberIdAndRecordedDate(member1.getId(), target);

		assertThat(found).hasSize(1);
		assertThat(found.get(0).getSummary()).isEqualTo("회원1 오늘 기록");
		assertThat(found.get(0).getMember().getId()).isEqualTo(member1.getId());
		assertThat(found.get(0).getRecordedDate()).isEqualTo(target);
	}

	@Test
	@DisplayName("같은 회원·같은 날짜에 여러 건이 있으면 전부 반환합니다")
	void returnsAllMatchingRecords() {
		Member member = persistMember("010-2000-0003");
		LocalDate target = LocalDate.of(2026, 8, 16);

		entityManager.persist(newRecord(member, target, "첫 번째 기록"));
		entityManager.persist(newRecord(member, target, "두 번째 기록"));
		entityManager.flush();
		entityManager.clear();

		List<HealthRecord> found = healthRecordRepository.findByMemberIdAndRecordedDate(member.getId(), target);

		assertThat(found).hasSize(2);
	}

	@Test
	@DisplayName("조건에 맞는 기록이 없으면 빈 목록을 반환합니다")
	void returnsEmptyWhenNotFound() {
		Member member = persistMember("010-2000-0004");
		entityManager.flush();
		entityManager.clear();

		List<HealthRecord> found = healthRecordRepository
				.findByMemberIdAndRecordedDate(member.getId(), LocalDate.of(2026, 8, 16));

		assertThat(found).isEmpty();
	}

	@Test
	@DisplayName("존재하지 않는 memberId 로 조회해도 빈 목록을 반환합니다")
	void returnsEmptyForUnknownMember() {
		List<HealthRecord> found = healthRecordRepository
				.findByMemberIdAndRecordedDate(999_999L, LocalDate.of(2026, 8, 16));

		assertThat(found).isEmpty();
	}

	@Test
	@DisplayName("detail 에 넣은 JSON 문자열이 재조회 후에도 그대로 보존됩니다")
	void preservesDetailJsonAfterReload() {
		Member member = persistMember("010-2000-0009");
		LocalDate target = LocalDate.of(2026, 8, 16);
		String detail = "{\"bedtime\":\"03:00\",\"quality\":\"poor\"}";

		entityManager.persist(HealthRecord.builder()
				.member(member)
				.type(HealthRecord.HealthType.SLEEP)
				.summary("새벽 3시 취침")
				.detail(detail)
				.recordedDate(target)
				.build());
		entityManager.flush();
		entityManager.clear();

		List<HealthRecord> found = healthRecordRepository.findByMemberIdAndRecordedDate(member.getId(), target);

		// 이스케이프나 따옴표 감싸기 없이 저장한 문자열과 완전히 같아야 합니다.
		// 값이 "{\"bedtime\":...}" 처럼 한 겹 더 감싸여 돌아오면 컬럼 타입이
		// TEXT 가 아니라 JSON 으로 되돌아간 것입니다.
		assertThat(found).hasSize(1);
		assertThat(found.get(0).getDetail()).isEqualTo(detail);
	}
}
