package com.hackathon.backend.healthrecord.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hackathon.backend.common.exception.NotFoundException;
import com.hackathon.backend.healthrecord.dto.HealthRecordResponse;
import com.hackathon.backend.healthrecord.entity.HealthRecord;
import com.hackathon.backend.healthrecord.repository.HealthRecordRepository;

/**
 * 건강 기록 조회·확인을 담당합니다.
 *
 * 대표 기록 선정이나 타입별 슬롯 채우기 같은 가공은 팀 문서 어디에도 근거가
 * 없어 하지 않습니다. 리포지토리가 정렬 없이 반환하므로(HealthRecordRepository
 * 주석 참고) 정렬은 여기서 합니다.
 */
@Service
@Transactional(readOnly = true)
public class HealthRecordService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	/**
	 * recordedAt 오름차순, null 은 뒤로, 동률이면 id 오름차순입니다.
	 * 프론트가 DB 반환 순서에 의존하지 않도록 하는 최소한의 안정성입니다.
	 */
	private static final Comparator<HealthRecord> ORDER =
			Comparator.comparing(HealthRecord::getRecordedAt, Comparator.nullsLast(Comparator.naturalOrder()))
					.thenComparing(HealthRecord::getId);

	/**
	 * 기간 조회는 하루짜리 조회와 달리 여러 날짜가 섞이므로 recordedDate 를
	 * 먼저 비교합니다. 그렇지 않으면 하루 중 이른 시각 기록이 다른 날의 늦은
	 * 시각 기록보다 먼저 나오는 등 날짜 순서가 뒤섞입니다.
	 */
	private static final Comparator<HealthRecord> RANGE_ORDER =
			Comparator.comparing(HealthRecord::getRecordedDate)
					.thenComparing(HealthRecord::getRecordedAt, Comparator.nullsLast(Comparator.naturalOrder()))
					.thenComparing(HealthRecord::getId);

	private final HealthRecordRepository healthRecordRepository;

	public HealthRecordService(HealthRecordRepository healthRecordRepository) {
		this.healthRecordRepository = healthRecordRepository;
	}

	/**
	 * 특정 회원의 오늘(KST) 건강 기록을 있는 그대로 반환합니다.
	 *
	 * 없는 memberId 도 회원 존재 여부를 확인하지 않고 빈 배열을 반환합니다.
	 * 회원 조회는 다른 담당자 소유의 도메인이라 여기서 건드리지 않습니다.
	 */
	public List<HealthRecordResponse> findToday(Long memberId) {
		LocalDate today = LocalDate.now(KST);
		return healthRecordRepository.findByMemberIdAndRecordedDate(memberId, today).stream()
				.sorted(ORDER)
				.map(HealthRecordResponse::from)
				.toList();
	}

	/**
	 * 양쪽 끝을 포함하는 기간으로 조회합니다. to 를 생략하면 오늘(KST),
	 * from 을 생략하면 to 에서 6일 전입니다(둘 다 생략하면 최근 7일).
	 *
	 * findToday 와 마찬가지로 회원 존재 확인은 하지 않고, from 이 to 보다
	 * 늦으면 빈 목록을 반환합니다 — 뒤집힌 기간에 대한 처리 규칙이 팀에서
	 * 합의된 적이 없어 별도 검증을 추가하지 않습니다.
	 */
	public List<HealthRecordResponse> findRange(Long memberId, LocalDate from, LocalDate to) {
		LocalDate resolvedTo = to != null ? to : LocalDate.now(KST);
		LocalDate resolvedFrom = from != null ? from : resolvedTo.minusDays(6);
		return healthRecordRepository.findByMemberIdAndRecordedDateBetween(memberId, resolvedFrom, resolvedTo).stream()
				.sorted(RANGE_ORDER)
				.map(HealthRecordResponse::from)
				.toList();
	}

	/**
	 * 건강 기록을 사용자가 확인 처리합니다. status 를 CONFIRMED 로 올립니다.
	 * 이미 CONFIRMED 인 기록에 다시 호출해도 예외 없이 성공합니다 — 확인
	 * 버튼이 두 번 눌리는 것은 정상적인 흐름입니다.
	 */
	@Transactional
	public HealthRecordResponse confirm(Long healthRecordId) {
		HealthRecord record = healthRecordRepository.findById(healthRecordId)
				.orElseThrow(() -> new NotFoundException("해당 건강 기록이 없습니다."));
		record.confirm();
		return HealthRecordResponse.from(record);
	}
}
