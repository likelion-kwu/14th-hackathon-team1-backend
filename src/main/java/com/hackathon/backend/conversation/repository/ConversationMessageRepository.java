package com.hackathon.backend.conversation.repository;

import com.hackathon.backend.conversation.entity.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    // 대화의 모든 메시지를 순서대로 조회 (AI 컨텍스트 구성 시 사용)
    List<ConversationMessage> findByConversationIdOrderBySequenceNoAsc(Long conversationId);

    // 대화 토큰 합산 (Compaction 임계 판단용)
    // @Query 없이 하려면 서비스 레이어에서 stream().mapToInt().sum() 으로 처리
    long countByConversationId(Long conversationId);
}
