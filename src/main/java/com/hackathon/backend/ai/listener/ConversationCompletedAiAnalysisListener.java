package com.hackathon.backend.ai.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.hackathon.backend.ai.entity.AiAnalysis.TaskType;
import com.hackathon.backend.ai.repository.AiAnalysisRepository;
import com.hackathon.backend.ai.service.AiAnalysisPipelineService;
import com.hackathon.backend.conversation.event.ConversationCompletedEvent;

@Component
public class ConversationCompletedAiAnalysisListener {

	private static final Logger log = LoggerFactory.getLogger(ConversationCompletedAiAnalysisListener.class);
	private final AiAnalysisRepository aiAnalysisRepository;
	private final AiAnalysisPipelineService aiAnalysisPipelineService;

	public ConversationCompletedAiAnalysisListener(AiAnalysisRepository aiAnalysisRepository,
			AiAnalysisPipelineService aiAnalysisPipelineService) {
		this.aiAnalysisRepository = aiAnalysisRepository;
		this.aiAnalysisPipelineService = aiAnalysisPipelineService;
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void triggerHealthExtraction(ConversationCompletedEvent event) {
		if (aiAnalysisRepository.findByConversationIdAndTaskType(event.conversationId(), TaskType.HEALTH_EXTRACTION)
				.isPresent()) {
			return;
		}
		try {
			aiAnalysisPipelineService.trigger(event.conversationId());
		} catch (Exception exception) {
			log.error("Health extraction could not start for conversationId={}", event.conversationId(), exception);
		}
	}
}
