package com.hackathon.backend.ai.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hackathon.backend.ai.dto.AiAnalysisResponse;
import com.hackathon.backend.ai.entity.AiAnalysis.TaskType;
import com.hackathon.backend.ai.repository.AiAnalysisRepository;
import com.hackathon.backend.common.exception.NotFoundException;

/**
 * AI 분석 작업 상태를 조회합니다.
 *
 * 작업이 아직 없다는 것은 추출 결과가 없다는 것과 다릅니다. 전자는 파이프라인이
 * 시작되지 않았거나 작업 생성에 실패한 상태이므로 404로 구분합니다.
 */
@Service
@Transactional(readOnly = true)
public class AiAnalysisService {

	private final AiAnalysisRepository aiAnalysisRepository;

	public AiAnalysisService(AiAnalysisRepository aiAnalysisRepository) {
		this.aiAnalysisRepository = aiAnalysisRepository;
	}

	public AiAnalysisResponse findByConversation(Long conversationId, TaskType taskType) {
		return aiAnalysisRepository.findByConversationIdAndTaskType(conversationId, taskType)
				.map(AiAnalysisResponse::from)
				.orElseThrow(() -> new NotFoundException("해당 조건의 분석 작업이 아직 없습니다."));
	}
}
