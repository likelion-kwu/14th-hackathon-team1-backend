package com.hackathon.backend.conversation.service;

import com.hackathon.backend.ai.service.AiAnalysisPipelineService;
import com.hackathon.backend.common.exception.NotFoundException;
import com.hackathon.backend.conversation.dto.ConversationResponse;
import com.hackathon.backend.conversation.entity.Conversation;
import com.hackathon.backend.conversation.entity.Conversation.ConversationStatus;
import com.hackathon.backend.conversation.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final AiAnalysisPipelineService aiAnalysisPipelineService;

    public record CompleteResult(ConversationResponse response, boolean newlyCompleted) {}

    @Transactional
    public CompleteResult complete(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("해당 대화가 없습니다."));

        ConversationStatus status = conversation.getStatus();

        if (status == ConversationStatus.COMPLETED) {
            return new CompleteResult(toResponse(conversation), false);
        }

        if (status != ConversationStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "진행 중인 대화만 종료할 수 있습니다.");
        }

        conversation.complete();
        return new CompleteResult(toResponse(conversation), true);
    }

    public void triggerAiAnalysis(Long conversationId) {
        try {
            aiAnalysisPipelineService.trigger(conversationId);
        } catch (Exception e) {
            log.warn("AI trigger failed for conversationId={}", conversationId, e);
        }
    }

    private ConversationResponse toResponse(Conversation conversation) {
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
}