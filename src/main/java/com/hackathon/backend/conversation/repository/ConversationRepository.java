package com.hackathon.backend.conversation.repository;

import com.hackathon.backend.conversation.entity.Conversation;
import com.hackathon.backend.conversation.entity.Conversation.ConversationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Conversation> findWithLockById(Long id);

    // 특정 날짜의 대화 목록 (일일 요약 생성 시 사용)
    List<Conversation> findByMemberIdAndSessionDate(Long memberId, LocalDate sessionDate);

    // 오늘 진행 중인 대화가 있는지 확인
    Optional<Conversation> findByMemberIdAndSessionDateAndStatus(
            Long memberId, LocalDate sessionDate, ConversationStatus status);

    // 회원의 전체 대화 목록 (최신순)
    List<Conversation> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
