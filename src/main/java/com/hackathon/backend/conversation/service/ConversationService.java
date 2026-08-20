package com.hackathon.backend.conversation.service;

import com.hackathon.backend.ai.service.AiAnalysisPipelineService;
import com.hackathon.backend.common.exception.NotFoundException;
import com.hackathon.backend.conversation.dto.ConversationResponse;
import com.hackathon.backend.conversation.entity.Conversation;
import com.hackathon.backend.conversation.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final AiAnalysisPipelineService aiAnalysisPipelineService;

    @Transactional
    public ConversationResponse complete(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("해당 대화가 없습니다."));

        // 이미 완료면 멱등 처리 (이슈 요건)
        if (conversation.getStatus() != Conversation.ConversationStatus.COMPLETED) {
            conversation.complete();
        }

        // member가 LAZY라 트랜잭션 안에서 접근해야 NPE 안 남
        return new ConversationResponse(
                conversation.getId(),
                conversation.getMember().getId(),
                conversation.getType(),
                conversation.getStatus(),
                conversation.getSessionDate(),
                conversation.getStartedAt(),
                conversation.getEndedAt(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    public void triggerAiAnalysis(Long conversationId) {
        try {
            aiAnalysisPipelineService.trigger(conversationId);
        } catch (Exception e) {
            // AI 실패해도 대화 종료는 이미 커밋됨 — 여기서 삼킴 (이슈 격리 요건)
            log.warn("AI trigger failed for conversationId={}", conversationId, e);
        }
    }
}