package com.hackathon.backend.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.hackathon.backend.ai.entity.AiAnalysis;
import com.hackathon.backend.common.exception.NotFoundException;
import com.hackathon.backend.conversation.entity.Conversation;
import com.hackathon.backend.member.entity.Member;

@DataJpaTest
@Import(AiAnalysisLifecycleService.class)
class AiAnalysisLifecycleServiceTest {

	@Autowired
	private AiAnalysisLifecycleService lifecycleService;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("작업을 생성하고 처리, 성공 상태로 전이합니다")
	void createsAndCompletesAnalysis() {
		Member member = persistMember("010-5000-0001");
		Conversation conversation = persistConversation(member);

		AiAnalysis analysis = lifecycleService.create(member, conversation, AiAnalysis.TaskType.HEALTH_EXTRACTION);
		lifecycleService.markProcessing(analysis.getId(), "test-model", "health-extraction-v1");
		lifecycleService.markSuccess(analysis.getId(), "{\"records\":[]}");
		entityManager.flush();
		entityManager.clear();

		AiAnalysis found = entityManager.find(AiAnalysis.class, analysis.getId());
		assertThat(found.getStatus()).isEqualTo(AiAnalysis.AnalysisStatus.SUCCESS);
		assertThat(found.getModelName()).isEqualTo("test-model");
		assertThat(found.getSchemaVersion()).isEqualTo("health-extraction-v1");
		assertThat(found.getRawResponse()).isEqualTo("{\"records\":[]}");
		assertThat(found.getErrorMessage()).isNull();
	}

	@Test
	@DisplayName("처리 중이 아닌 작업은 성공으로 종료할 수 없습니다")
	void rejectsSuccessBeforeProcessing() {
		AiAnalysis analysis = lifecycleService.create(persistMember("010-5000-0002"), null,
				AiAnalysis.TaskType.DAILY_SUMMARY);

		assertThatThrownBy(() -> lifecycleService.markSuccess(analysis.getId(), "{}"))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("실패 사유를 남기고 종료 상태에서 재시작하지 못하게 합니다")
	void recordsFailureAndRejectsRestart() {
		AiAnalysis analysis = lifecycleService.create(persistMember("010-5000-0003"), null,
				AiAnalysis.TaskType.OVERALL_REPORT);
		lifecycleService.markFailed(analysis.getId(), "응답 형식이 올바르지 않습니다.");

		assertThatThrownBy(() -> lifecycleService.markProcessing(analysis.getId(), "test-model", "v1"))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("없는 작업의 상태를 변경하면 404 예외를 던집니다")
	void rejectsMissingAnalysis() {
		assertThatThrownBy(() -> lifecycleService.markFailed(999L, "실패"))
				.isInstanceOf(NotFoundException.class);
	}

	private Member persistMember(String phone) {
		return entityManager.persist(Member.builder().nickname("회원").phone(phone).build());
	}

	private Conversation persistConversation(Member member) {
		return entityManager.persist(Conversation.builder()
				.member(member)
				.type(Conversation.ConversationType.CHAT)
				.sessionDate(LocalDate.of(2026, 8, 20))
				.build());
	}
}
