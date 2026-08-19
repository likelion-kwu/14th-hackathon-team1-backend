package com.hackathon.backend.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.hackathon.backend.ai.entity.AiAnalysis;
import com.hackathon.backend.member.entity.Member;

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

	@TestConfiguration
	static class JacksonConfiguration {
		@Bean
		ObjectMapper objectMapper() {
			return JsonMapper.builder().build();
		}
	}
}
