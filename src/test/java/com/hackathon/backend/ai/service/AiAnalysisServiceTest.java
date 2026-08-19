package com.hackathon.backend.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.hackathon.backend.ai.dto.AiAnalysisResponse;
import com.hackathon.backend.ai.entity.AiAnalysis;
import com.hackathon.backend.common.exception.NotFoundException;
import com.hackathon.backend.conversation.entity.Conversation;
import com.hackathon.backend.member.entity.Member;

@DataJpaTest
@Import(AiAnalysisService.class)
class AiAnalysisServiceTest {

	@Autowired
	private AiAnalysisService aiAnalysisService;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("대화와 작업 종류가 일치하는 분석 상태를 반환합니다")
	void findsAnalysisByConversationAndTaskType() {
		Member member = persistMember("010-4000-0001");
		Conversation conversation = persistConversation(member);
		AiAnalysis analysis = entityManager.persist(AiAnalysis.builder()
				.member(member)
				.conversation(conversation)
				.taskType(AiAnalysis.TaskType.HEALTH_EXTRACTION)
				.status(AiAnalysis.AnalysisStatus.SUCCESS)
				.modelName("test-model")
				.schemaVersion("v1")
				.build());
		entityManager.flush();
		entityManager.clear();

		AiAnalysisResponse response = aiAnalysisService.findByConversation(
				conversation.getId(), AiAnalysis.TaskType.HEALTH_EXTRACTION);

		assertThat(response.id()).isEqualTo(analysis.getId());
		assertThat(response.memberId()).isEqualTo(member.getId());
		assertThat(response.conversationId()).isEqualTo(conversation.getId());
		assertThat(response.status()).isEqualTo(AiAnalysis.AnalysisStatus.SUCCESS);
		assertThat(response.modelName()).isEqualTo("test-model");
	}

	@Test
	@DisplayName("작업 종류가 다르면 분석 작업이 없는 것으로 처리합니다")
	void rejectsDifferentTaskType() {
		Member member = persistMember("010-4000-0002");
		Conversation conversation = persistConversation(member);
		entityManager.persist(AiAnalysis.builder()
				.member(member).conversation(conversation)
				.taskType(AiAnalysis.TaskType.HEALTH_EXTRACTION).build());
		entityManager.flush();

		assertThatThrownBy(() -> aiAnalysisService.findByConversation(
				conversation.getId(), AiAnalysis.TaskType.DAILY_SUMMARY))
				.isInstanceOf(NotFoundException.class);
	}

	private Member persistMember(String phone) {
		return entityManager.persist(Member.builder().nickname("회원").phone(phone).build());
	}

	private Conversation persistConversation(Member member) {
		return entityManager.persist(Conversation.builder()
				.member(member)
				.type(Conversation.ConversationType.CHAT)
				.sessionDate(java.time.LocalDate.of(2026, 8, 20))
				.build());
	}
}
