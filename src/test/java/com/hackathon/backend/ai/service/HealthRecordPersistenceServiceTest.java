package com.hackathon.backend.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import com.hackathon.backend.ai.dto.HealthExtractionResult;
import com.hackathon.backend.ai.dto.HealthExtractionResult.HealthRecordCandidate;
import com.hackathon.backend.ai.dto.HealthExtractionResult.WaterDetail;
import com.hackathon.backend.ai.entity.AiAnalysis;
import com.hackathon.backend.conversation.entity.Conversation;
import com.hackathon.backend.healthrecord.entity.HealthRecord;
import com.hackathon.backend.healthrecord.entity.HealthRecord.HealthType;
import com.hackathon.backend.healthrecord.repository.HealthRecordRepository;
import com.hackathon.backend.member.entity.Member;

import tools.jackson.databind.json.JsonMapper;

class HealthRecordPersistenceServiceTest {

	private final HealthRecordRepository healthRecordRepository = mock(HealthRecordRepository.class);
	private final HealthRecordPersistenceService service = new HealthRecordPersistenceService(
			healthRecordRepository, JsonMapper.builder().build());

	@Test
	void persistsParsedHealthRecordsWithAiAnalysisReference() {
		Member member = mock(Member.class);
		Conversation conversation = mock(Conversation.class);
		AiAnalysis analysis = mock(AiAnalysis.class);
		HealthExtractionResult result = new HealthExtractionResult("health-extraction-v1", List.of(
				new HealthRecordCandidate(HealthType.WATER, "water intake", new WaterDetail(BigDecimal.valueOf(300), "ml"),
						LocalDate.of(2026, 8, 20), null, BigDecimal.valueOf(0.9), "drank water")));

		List<HealthRecord> persisted = service.persist(member, conversation, analysis, result);

		then(healthRecordRepository).should().saveAll(persisted);
		HealthRecord record = persisted.get(0);
		assertThat(record.getMember()).isSameAs(member);
		assertThat(record.getConversation()).isSameAs(conversation);
		assertThat(record.getAnalysis()).isSameAs(analysis);
		assertThat(record.getType()).isEqualTo(HealthType.WATER);
		assertThat(record.getDetail()).isEqualTo("{\"amount\":300,\"unit\":\"ml\"}");
	}
}
