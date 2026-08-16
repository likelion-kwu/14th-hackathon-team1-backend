package com.hackathon.backend.healthrecord.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hackathon.backend.healthrecord.dto.HealthRecordResponse;
import com.hackathon.backend.healthrecord.entity.HealthRecord;
import com.hackathon.backend.healthrecord.repository.HealthRecordRepository;

/**
 * 건강 기록 조회를 담당합니다.
 *
 * P0 범위는 "건강 기록 조회" 하나뿐입니다(회의록 §4). 대표 기록 선정이나
 * 타입별 슬롯 채우기 같은 가공은 팀 문서 어디에도 근거가 없어 하지 않습니다.
 * 리포지토리가 정렬 없이 반환하므로(HealthRecordRepository 주석 참고) 정렬은
 * 여기서 합니다.
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
}
