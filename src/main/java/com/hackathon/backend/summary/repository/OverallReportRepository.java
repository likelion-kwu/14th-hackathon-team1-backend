package com.hackathon.backend.summary.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hackathon.backend.summary.entity.OverallReport;

/**
 * 종합 리포트 조회입니다.
 *
 * OverallReport 는 회원과 1:1 이고 PK 자체가 memberId 이므로(§엔티티의 @MapsId)
 * 별도 파생 쿼리 없이 JpaRepository 의 findById 로 조회 키를 그대로 만족합니다.
 */
public interface OverallReportRepository extends JpaRepository<OverallReport, Long> {
}
