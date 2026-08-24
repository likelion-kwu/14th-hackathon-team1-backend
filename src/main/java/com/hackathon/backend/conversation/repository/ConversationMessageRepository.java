package com.hackathon.backend.conversation.repository;

import com.hackathon.backend.conversation.entity.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    // 대화의 모든 메시지를 순서대로 조회 (AI 컨텍스트 구성 시 사용)
    List<ConversationMessage> findByConversationIdOrderBySequenceNoAsc(Long conversationId);

    // 대화 토큰 합산 (Compaction 임계 판단용)
    // @Query 없이 하려면 서비스 레이어에서 stream().mapToInt().sum() 으로 처리
    long countByConversationId(Long conversationId);

    // 멱등성 키로 사용자 메시지 조회 (재요청 중복 방지)
    Optional<ConversationMessage> findByClientMessageId(String clientMessageId);

    // sequenceNo로 메시지 조회 (멱등성 응답 재구성 시 AI 응답 메시지 조회용)
    Optional<ConversationMessage> findByConversationIdAndSequenceNo(Long conversationId, int sequenceNo);
}
