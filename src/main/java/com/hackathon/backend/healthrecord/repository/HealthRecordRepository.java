package com.hackathon.backend.healthrecord.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hackathon.backend.healthrecord.entity.HealthRecord;

/**
 * 건강 기록 조회입니다.
 *
 * 쓰기 경로는 AI 추출 파이프라인이 담당하므로 여기서는 조회만 씁니다.
 */
public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {

	/**
	 * 특정 회원의 특정 날짜 기록을 모두 가져옵니다.
	 *
	 * 조건에 맞는 행을 가공 없이 전부 반환합니다. 같은 날 같은 타입이 여러 번
	 * 추출될 수 있는데(통화 2회, 재추출 등) 그중 하나를 고르거나 타입으로 거르지
	 * 않습니다. 그런 규칙은 팀에서 합의된 적이 없고, 쿼리에 숨겨두면 나중에
	 * 바꾸기도 테스트하기도 어려워집니다.
	 *
	 * 하루치라 건수가 적어 정렬 없이 가져오고 순서는 서비스에서 맞춥니다.
	 */
	List<HealthRecord> findByMemberIdAndRecordedDate(Long memberId, LocalDate recordedDate);
}
