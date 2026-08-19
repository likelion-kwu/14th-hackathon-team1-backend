package com.hackathon.backend.ai.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hackathon.backend.ai.entity.AiAnalysis;
import com.hackathon.backend.ai.entity.AiAnalysis.TaskType;
import com.hackathon.backend.ai.repository.AiAnalysisRepository;
import com.hackathon.backend.common.exception.NotFoundException;
import com.hackathon.backend.conversation.entity.Conversation;
import com.hackathon.backend.member.entity.Member;

/**
 * AI 분석 작업의 생성과 상태 전이를 담당합니다.
 *
 * 실제 모델 호출과 결과 저장은 별도 파이프라인이 담당합니다. 이 서비스는 호출 전후
 * 상태를 일관되게 남겨 조회 API와 재처리 로직이 같은 작업 이력을 보게 합니다.
 */
@Service
@Transactional
public class AiAnalysisLifecycleService {

	private final AiAnalysisRepository aiAnalysisRepository;

	public AiAnalysisLifecycleService(AiAnalysisRepository aiAnalysisRepository) {
		this.aiAnalysisRepository = aiAnalysisRepository;
	}

	public AiAnalysis create(Member member, Conversation conversation, TaskType taskType) {
		return aiAnalysisRepository.save(AiAnalysis.builder()
				.member(member)
				.conversation(conversation)
				.taskType(taskType)
				.build());
	}

	public void markProcessing(Long analysisId, String modelName, String schemaVersion) {
		findById(analysisId).startProcessing(modelName, schemaVersion);
	}

	public void markSuccess(Long analysisId, String rawResponse) {
		findById(analysisId).succeed(rawResponse);
	}

	public void markFailed(Long analysisId, String errorMessage) {
		findById(analysisId).fail(errorMessage);
	}

	/**
	 * 파싱 실패에도 모델 원문을 보존합니다. rawResponse는 JSON 유효성과 무관하게 TEXT로 저장됩니다.
	 */
	public void markFailed(Long analysisId, String rawResponse, String errorMessage) {
		findById(analysisId).fail(rawResponse, errorMessage);
	}

	private AiAnalysis findById(Long analysisId) {
		return aiAnalysisRepository.findById(analysisId)
				.orElseThrow(() -> new NotFoundException("해당 분석 작업을 찾을 수 없습니다."));
	}
}
