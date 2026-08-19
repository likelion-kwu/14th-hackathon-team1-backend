package com.hackathon.backend.healthrecord.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.hackathon.backend.conversation.entity.Conversation;
import com.hackathon.backend.common.exception.NotFoundException;
import com.hackathon.backend.healthrecord.dto.HealthRecordResponse;
import com.hackathon.backend.healthrecord.entity.HealthRecord;
import com.hackathon.backend.member.entity.Member;

/**
 * findToday 가 "가공하지 않고 있는 그대로 반환한다"는 계약을 지키는지 확인합니다.
 *
 * 이 서비스는 대표 기록 선정, 타입별 슬롯 채우기, 회원 존재 확인을 하지
 * 않습니다(클래스 Javadoc 참고). 그래서 이 테스트가 지키려는 것은 다음과
 * 같습니다.
 * 1) 오늘(KST) 기록만 반환한다 — 어제·내일 기록은 섞이지 않는다
 * 2) 같은 타입이 여러 건이어도 대표 한 건을 고르지 않고 전부 반환한다
 * 3) WATER, OTHER 같은 타입도 필터링 없이 그대로 반환된다 — 슬롯 개념이 없다
 * 4) 없는 회원이어도 예외 없이 빈 목록을 반환한다 — 회원 존재 확인을 하지 않는다
 * 5) 정렬은 서비스가 직접 한다 (recordedAt 오름차순, null 은 뒤로, 동률이면 id 오름차순)
 *
 * detail 의 JSON 왕복 보존 자체는 HealthRecordRepositoryTest 에서 확인합니다.
 * 여기서는 빈 문자열이 null 로 정규화되는지만 봅니다. 그 분기가 없으면
 * "detail": 뒤에 아무것도 없는 깨진 JSON 이 응답에 나갑니다.
 */
@DataJpaTest
@Import(HealthRecordService.class)
class HealthRecordServiceTest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	@Autowired
	private HealthRecordService healthRecordService;

	@Autowired
	private TestEntityManager entityManager;

	private LocalDate today() {
		return LocalDate.now(KST);
	}

	private Member persistMember(String phone) {
		return entityManager.persist(Member.builder()
				.nickname("회원")
				.phone(phone)
				.build());
	}

	private Conversation persistConversation(Member member, LocalDate sessionDate) {
		return entityManager.persist(Conversation.builder()
				.member(member)
				.type(Conversation.ConversationType.CALL)
				.sessionDate(sessionDate)
				.build());
	}

	private HealthRecord.HealthRecordBuilder baseRecord(Member member, LocalDate recordedDate) {
		return HealthRecord.builder()
				.member(member)
				.type(HealthRecord.HealthType.SLEEP)
				.summary("요약")
				.recordedDate(recordedDate);
	}

	@Test
	@DisplayName("오늘 기록만 반환하고 어제·내일 기록은 섞이지 않습니다")
	void returnsOnlyTodayRecords() {
		Member member = persistMember("010-3000-0001");
		entityManager.persist(baseRecord(member, today()).summary("오늘 기록").build());
		entityManager.persist(baseRecord(member, today().minusDays(1)).summary("어제 기록").build());
		entityManager.persist(baseRecord(member, today().plusDays(1)).summary("내일 기록").build());
		entityManager.flush();
		entityManager.clear();

		List<HealthRecordResponse> found = healthRecordService.findToday(member.getId());

		assertThat(found).hasSize(1);
		assertThat(found.get(0).summary()).isEqualTo("오늘 기록");
	}

	@Test
	@DisplayName("같은 타입이 2건이면 대표를 고르지 않고 2건 다 반환합니다")
	void returnsAllRecordsOfSameType() {
		Member member = persistMember("010-3000-0002");
		entityManager.persist(baseRecord(member, today()).summary("수면 첫 번째").build());
		entityManager.persist(baseRecord(member, today()).summary("수면 두 번째").build());
		entityManager.flush();
		entityManager.clear();

		List<HealthRecordResponse> found = healthRecordService.findToday(member.getId());

		assertThat(found).hasSize(2);
		assertThat(found).extracting(HealthRecordResponse::summary)
				.containsExactlyInAnyOrder("수면 첫 번째", "수면 두 번째");
	}

	@Test
	@DisplayName("WATER, OTHER 타입도 필터링 없이 그대로 반환됩니다")
	void doesNotFilterByType() {
		Member member = persistMember("010-3000-0003");
		entityManager.persist(HealthRecord.builder()
				.member(member).type(HealthRecord.HealthType.WATER).summary("물 섭취").recordedDate(today()).build());
		entityManager.persist(HealthRecord.builder()
				.member(member).type(HealthRecord.HealthType.OTHER).summary("기타 기록").recordedDate(today()).build());
		entityManager.flush();
		entityManager.clear();

		List<HealthRecordResponse> found = healthRecordService.findToday(member.getId());

		assertThat(found).extracting(HealthRecordResponse::type)
				.containsExactlyInAnyOrder(HealthRecord.HealthType.WATER, HealthRecord.HealthType.OTHER);
	}

	@Test
	@DisplayName("오늘 기록이 없으면 빈 목록을 반환합니다")
	void returnsEmptyWhenNoRecordsToday() {
		Member member = persistMember("010-3000-0004");
		entityManager.persist(baseRecord(member, today().minusDays(1)).summary("어제 기록").build());
		entityManager.flush();
		entityManager.clear();

		List<HealthRecordResponse> found = healthRecordService.findToday(member.getId());

		assertThat(found).isEmpty();
	}

	@Test
	@DisplayName("존재하지 않는 memberId 도 예외 없이 빈 목록을 반환합니다")
	void returnsEmptyForUnknownMemberWithoutException() {
		List<HealthRecordResponse> found = healthRecordService.findToday(999_999L);

		assertThat(found).isEmpty();
	}

	@Test
	@DisplayName("recordedAt 오름차순, null 은 뒤로, 동률이면 id 오름차순으로 정렬됩니다")
	void sortsByRecordedAtThenId() {
		Member member = persistMember("010-3000-0005");
		LocalDate date = today();

		HealthRecord withNullTime1 = entityManager.persist(
				baseRecord(member, date).summary("시간 없음 1").build());
		HealthRecord withNullTime2 = entityManager.persist(
				baseRecord(member, date).summary("시간 없음 2").build());
		HealthRecord later = entityManager.persist(HealthRecord.builder()
				.member(member).type(HealthRecord.HealthType.SLEEP).summary("늦은 시각")
				.recordedDate(date).recordedAt(LocalDateTime.of(date, java.time.LocalTime.of(9, 0))).build());
		HealthRecord earlier = entityManager.persist(HealthRecord.builder()
				.member(member).type(HealthRecord.HealthType.SLEEP).summary("이른 시각")
				.recordedDate(date).recordedAt(LocalDateTime.of(date, java.time.LocalTime.of(7, 0))).build());
		entityManager.flush();
		entityManager.clear();

		List<HealthRecordResponse> found = healthRecordService.findToday(member.getId());

		assertThat(found).extracting(HealthRecordResponse::summary)
				.containsExactly("이른 시각", "늦은 시각", "시간 없음 1", "시간 없음 2");
	}

	@Test
	@DisplayName("conversation 이 없는 기록은 conversationId 가 null 입니다")
	void conversationIdIsNullWhenNoConversation() {
		Member member = persistMember("010-3000-0006");
		entityManager.persist(baseRecord(member, today()).summary("대화 없는 기록").build());
		entityManager.flush();
		entityManager.clear();

		List<HealthRecordResponse> found = healthRecordService.findToday(member.getId());

		assertThat(found).hasSize(1);
		assertThat(found.get(0).conversationId()).isNull();
	}

	@Test
	@DisplayName("conversation 이 있는 기록은 conversationId 를 담습니다")
	void conversationIdIsPopulatedWhenConversationExists() {
		Member member = persistMember("010-3000-0007");
		Conversation conversation = persistConversation(member, today());
		entityManager.persist(HealthRecord.builder()
				.member(member).type(HealthRecord.HealthType.SLEEP).summary("대화 있는 기록")
				.recordedDate(today()).conversation(conversation).build());
		entityManager.flush();
		entityManager.clear();

		List<HealthRecordResponse> found = healthRecordService.findToday(member.getId());

		assertThat(found).hasSize(1);
		assertThat(found.get(0).conversationId()).isEqualTo(conversation.getId());
	}

	@Test
	@DisplayName("detail 이 빈 문자열이면 null 로 정규화합니다")
	void normalizesBlankDetailToNull() {
		Member member = persistMember("010-3000-0009");

		entityManager.persist(baseRecord(member, today()).detail("").build());
		entityManager.flush();
		entityManager.clear();

		List<HealthRecordResponse> found = healthRecordService.findToday(member.getId());

		// 빈 문자열이 그대로 나가면 @JsonRawValue 가 "detail": 뒤에 아무것도 쓰지 않아
		// 응답 전체가 파싱 불가능한 JSON 이 됩니다. 추출 결과가 없을 때 빈 문자열이
		// 저장되는 것은 충분히 있을 수 있는 경우라 여기서 막습니다.
		assertThat(found).hasSize(1);
		assertThat(found.get(0).detail()).isNull();
	}

	@Test
	@DisplayName("기간 조회는 양끝 날짜를 포함하고 recordedDate, recordedAt, id 순으로 정렬합니다")
	void findsRangeInclusivelyAndSortsByDateTimeThenId() {
		Member member = persistMember("010-3000-0010");
		LocalDate from = LocalDate.of(2026, 8, 10);
		LocalDate to = LocalDate.of(2026, 8, 12);
		entityManager.persist(baseRecord(member, from).summary("시작일").build());
		entityManager.persist(baseRecord(member, to).summary("끝날 null 시간 1").build());
		entityManager.persist(baseRecord(member, to).summary("끝날 null 시간 2").build());
		entityManager.persist(HealthRecord.builder()
				.member(member).type(HealthRecord.HealthType.SLEEP).summary("끝날 늦은 시간")
				.recordedDate(to).recordedAt(LocalDateTime.of(2026, 8, 12, 9, 0)).build());
		entityManager.persist(HealthRecord.builder()
				.member(member).type(HealthRecord.HealthType.SLEEP).summary("끝날 이른 시간")
				.recordedDate(to).recordedAt(LocalDateTime.of(2026, 8, 12, 7, 0)).build());
		entityManager.persist(baseRecord(member, from.minusDays(1)).summary("범위 밖").build());
		entityManager.flush();
		entityManager.clear();

		List<HealthRecordResponse> found = healthRecordService.findRange(member.getId(), from, to);

		assertThat(found).extracting(HealthRecordResponse::summary)
				.containsExactly("시작일", "끝날 이른 시간", "끝날 늦은 시간", "끝날 null 시간 1", "끝날 null 시간 2");
	}

	@Test
	@DisplayName("from 과 to 를 생략하면 KST 기준 최근 7일 기록을 반환합니다")
	void defaultsBothRangeBoundsToRecentSevenDays() {
		Member member = persistMember("010-3000-0011");
		entityManager.persist(baseRecord(member, today().minusDays(6)).summary("7일 범위 시작").build());
		entityManager.persist(baseRecord(member, today()).summary("오늘").build());
		entityManager.persist(baseRecord(member, today().minusDays(7)).summary("범위 밖").build());
		entityManager.flush();
		entityManager.clear();

		List<HealthRecordResponse> found = healthRecordService.findRange(member.getId(), null, null);

		assertThat(found).extracting(HealthRecordResponse::summary)
				.containsExactly("7일 범위 시작", "오늘");
	}

	@Test
	@DisplayName("from 을 생략하면 to 에서 6일 전을 시작일로 사용합니다")
	void defaultsMissingFromRelativeToTo() {
		Member member = persistMember("010-3000-0012");
		LocalDate to = LocalDate.of(2026, 8, 16);
		entityManager.persist(baseRecord(member, to.minusDays(6)).summary("기본 시작일").build());
		entityManager.persist(baseRecord(member, to).summary("종료일").build());
		entityManager.persist(baseRecord(member, to.minusDays(7)).summary("범위 밖").build());
		entityManager.flush();
		entityManager.clear();

		List<HealthRecordResponse> found = healthRecordService.findRange(member.getId(), null, to);

		assertThat(found).extracting(HealthRecordResponse::summary)
				.containsExactly("기본 시작일", "종료일");
	}

	@Test
	@DisplayName("to 를 생략하면 KST 기준 오늘을 종료일로 사용합니다")
	void defaultsMissingToToTodayInKst() {
		Member member = persistMember("010-3000-0013");
		LocalDate from = today().minusDays(1);
		entityManager.persist(baseRecord(member, from).summary("시작일").build());
		entityManager.persist(baseRecord(member, today()).summary("오늘").build());
		entityManager.persist(baseRecord(member, today().plusDays(1)).summary("내일").build());
		entityManager.flush();
		entityManager.clear();

		List<HealthRecordResponse> found = healthRecordService.findRange(member.getId(), from, null);

		assertThat(found).extracting(HealthRecordResponse::summary)
				.containsExactly("시작일", "오늘");
	}

	@Test
	@DisplayName("기간 내 기록이 없거나 존재하지 않는 memberId 여도 빈 목록을 반환합니다")
	void returnsEmptyForMissingRangeRecordsAndUnknownMember() {
		Member member = persistMember("010-3000-0014");
		entityManager.persist(baseRecord(member, LocalDate.of(2026, 8, 1)).summary("범위 밖").build());
		entityManager.flush();
		entityManager.clear();

		assertThat(healthRecordService.findRange(member.getId(), LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 16)))
				.isEmpty();
		assertThat(healthRecordService.findRange(999_999L, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 16)))
				.isEmpty();
	}

	@Test
	@DisplayName("건강 기록을 확인하면 CONFIRMED 상태로 저장하고 반환합니다")
	void confirmsRecord() {
		Member member = persistMember("010-3000-0015");
		HealthRecord record = entityManager.persist(baseRecord(member, today()).summary("확인할 기록").build());
		entityManager.flush();
		entityManager.clear();

		HealthRecordResponse confirmed = healthRecordService.confirm(record.getId());
		entityManager.flush();
		entityManager.clear();

		assertThat(confirmed.status()).isEqualTo(HealthRecord.HealthStatus.CONFIRMED);
		assertThat(entityManager.find(HealthRecord.class, record.getId()).getStatus())
				.isEqualTo(HealthRecord.HealthStatus.CONFIRMED);
	}

	@Test
	@DisplayName("확인할 건강 기록이 없으면 NotFoundException 을 던집니다")
	void throwsNotFoundWhenConfirmingMissingRecord() {
		assertThatThrownBy(() -> healthRecordService.confirm(999_999L))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	@DisplayName("이미 CONFIRMED 인 건강 기록을 다시 확인해도 성공합니다")
	void confirmsAlreadyConfirmedRecordIdempotently() {
		Member member = persistMember("010-3000-0016");
		HealthRecord record = entityManager.persist(HealthRecord.builder()
				.member(member).type(HealthRecord.HealthType.SLEEP).summary("이미 확인한 기록")
				.recordedDate(today()).status(HealthRecord.HealthStatus.CONFIRMED).build());
		entityManager.flush();
		entityManager.clear();

		HealthRecordResponse first = healthRecordService.confirm(record.getId());
		HealthRecordResponse second = healthRecordService.confirm(record.getId());

		assertThat(first.status()).isEqualTo(HealthRecord.HealthStatus.CONFIRMED);
		assertThat(second.status()).isEqualTo(HealthRecord.HealthStatus.CONFIRMED);
	}
}
