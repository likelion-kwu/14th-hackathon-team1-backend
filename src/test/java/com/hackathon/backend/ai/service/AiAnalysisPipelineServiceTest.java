package com.hackathon.backend.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.hackathon.backend.ai.entity.AiAnalysis;
import com.hackathon.backend.ai.client.OpenAiChatClient;
import com.hackathon.backend.conversation.entity.Conversation;
import com.hackathon.backend.healthrecord.entity.HealthRecord;
import com.hackathon.backend.member.entity.Member;
import com.hackathon.backend.summary.entity.MonthlyConversationSummary;
import com.hackathon.backend.summary.entity.OverallReport;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@DataJpaTest
@Import({ AiAnalysisLifecycleService.class, AiResultParser.class, HealthRecordPersistenceService.class,
		AiAnalysisPipelineService.class, AiAnalysisPipelineServiceTest.JacksonConfiguration.class })
class AiAnalysisPipelineServiceTest {

	@Autowired
	private AiAnalysisLifecycleService lifecycleService;

	@Autowired
	private AiAnalysisPipelineService pipelineService;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void preservesInvalidRawResponseAndMarksAnalysisFailed() {
		Member member = entityManager.persist(Member.builder().nickname("회원").phone("010-5000-0010").build());
		AiAnalysis analysis = lifecycleService.create(member, null, AiAnalysis.TaskType.HEALTH_EXTRACTION);
		lifecycleService.markProcessing(analysis.getId(), "test-model", "health-extraction-v1");

		pipelineService.persist(analysis.getId(), "not-json", new AiAnalysisTaskContext.HealthExtraction());
		entityManager.flush();
		entityManager.clear();

		AiAnalysis failed = entityManager.find(AiAnalysis.class, analysis.getId());
		assertThat(failed.getStatus()).isEqualTo(AiAnalysis.AnalysisStatus.FAILED);
		assertThat(failed.getRawResponse()).isEqualTo("not-json");
		assertThat(failed.getErrorMessage()).isNotBlank();
	}

	@Test
	void routesHealthExtractionToHealthRecordPersistence() {
		Member member = entityManager.persist(Member.builder().nickname("회원").phone("010-5000-0011").build());
		Conversation conversation = entityManager.persist(Conversation.builder().member(member)
				.type(Conversation.ConversationType.CHAT).sessionDate(LocalDate.of(2026, 8, 20)).build());
		AiAnalysis analysis = lifecycleService.create(member, conversation, AiAnalysis.TaskType.HEALTH_EXTRACTION);
		lifecycleService.markProcessing(analysis.getId(), "test-model", "health-extraction-v1");

		pipelineService.persist(analysis.getId(), """
				{"schemaVersion":"health-extraction-v1","records":[{
				"type":"WATER","summary":"물 300ml를 마셨습니다.","detail":{"amount":300,"unit":"ml"},
				"recordedDate":"2026-08-20","recordedAt":null,"confidence":0.9,"evidence":"물을 마셨다고 말했습니다."
				}]}""", new AiAnalysisTaskContext.HealthExtraction());
		entityManager.flush();
		entityManager.clear();

		AiAnalysis succeeded = entityManager.find(AiAnalysis.class, analysis.getId());
		assertThat(succeeded.getStatus()).isEqualTo(AiAnalysis.AnalysisStatus.SUCCESS);
		assertThat(entityManager.getEntityManager().createQuery("select h from HealthRecord h", HealthRecord.class).getResultList())
				.singleElement().extracting(HealthRecord::getType).isEqualTo(HealthRecord.HealthType.WATER);
	}

	@Test
	void triggerCallsOpenAiAndPersistsHealthExtraction() {
		Member member = entityManager.persist(Member.builder().nickname("회원").phone("010-5000-0013").build());
		Conversation conversation = entityManager.persist(Conversation.builder().member(member)
				.type(Conversation.ConversationType.CHAT).sessionDate(LocalDate.of(2026, 8, 20)).build());
		entityManager.persist(com.hackathon.backend.conversation.entity.ConversationMessage.builder()
				.conversation(conversation).role(com.hackathon.backend.conversation.entity.ConversationMessage.MessageRole.USER)
				.content("물 300ml를 마셨어요.").sequenceNo(1).build());
		entityManager.flush();

		pipelineService.trigger(conversation.getId());
		entityManager.flush();
		entityManager.clear();

		AiAnalysis analysis = entityManager.getEntityManager().createQuery("select a from AiAnalysis a", AiAnalysis.class)
				.getSingleResult();
		assertThat(analysis.getStatus()).isEqualTo(AiAnalysis.AnalysisStatus.SUCCESS);
		assertThat(analysis.getModelName()).isEqualTo("gpt-4o-mini");
		assertThat(entityManager.getEntityManager().createQuery("select h from HealthRecord h", HealthRecord.class).getResultList())
				.singleElement().extracting(HealthRecord::getType).isEqualTo(HealthRecord.HealthType.WATER);
	}

	@Test
	void calculatesOverallReportValidationContextFromExistingData() {
		Member member = entityManager.persist(Member.builder().nickname("회원").phone("010-5000-0012").build());
		entityManager.persist(MonthlyConversationSummary.builder().member(member).periodStart(LocalDate.of(2026, 1, 1))
				.periodEnd(LocalDate.of(2026, 1, 31)).summary("1월 요약").build());
		entityManager.persist(MonthlyConversationSummary.builder().member(member).periodStart(LocalDate.of(2026, 2, 1))
				.periodEnd(LocalDate.of(2026, 2, 28)).summary("2월 요약").build());
		entityManager.persist(HealthRecord.builder().member(member).type(HealthRecord.HealthType.WATER).summary("물 섭취")
				.detail("{\"amount\":300}").recordedDate(LocalDate.of(2026, 2, 10)).confidence(BigDecimal.ONE)
				.evidence("물 섭취 기록").build());
		AiAnalysis analysis = lifecycleService.create(member, null, AiAnalysis.TaskType.OVERALL_REPORT);
		lifecycleService.markProcessing(analysis.getId(), "test-model", "overall-report-v1");

		pipelineService.persist(analysis.getId(), """
				{"schemaVersion":"overall-report-v1","summary":"종합 리포트","detail":{
				"period":{"from":"2026-01-01","to":"2026-02-28"},"highlights":[],
				"healthTrends":[{"type":"WATER","trend":"stable","note":"기록이 유지됩니다."}],"recommendations":[]
				}}""", new AiAnalysisTaskContext.OverallReport());
		entityManager.flush();
		entityManager.clear();

		AiAnalysis succeeded = entityManager.find(AiAnalysis.class, analysis.getId());
		OverallReport report = entityManager.find(OverallReport.class, member.getId());
		assertThat(succeeded.getStatus()).isEqualTo(AiAnalysis.AnalysisStatus.SUCCESS);
		assertThat(report.getMonthlySummaryCount()).isEqualTo(2);
		assertThat(report.getDetail()).contains("WATER");
	}

	@TestConfiguration
	static class JacksonConfiguration {
		@Bean
		ObjectMapper objectMapper() {
			return JsonMapper.builder().build();
		}

		@Bean
		OpenAiChatClient openAiChatClient() {
			return (systemPrompt, userPrompt) -> """
					{"schemaVersion":"health-extraction-v1","records":[{
					"type":"WATER","summary":"물 300ml를 마셨습니다.","detail":{"amount":300,"unit":"ml"},
					"recordedDate":"2026-08-20","recordedAt":null,"confidence":0.9,"evidence":"물 300ml"
					}]}""";
		}
	}
}
