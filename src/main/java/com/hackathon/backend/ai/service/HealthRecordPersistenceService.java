package com.hackathon.backend.ai.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hackathon.backend.ai.dto.HealthExtractionResult;
import com.hackathon.backend.ai.dto.HealthExtractionResult.HealthRecordCandidate;
import com.hackathon.backend.ai.entity.AiAnalysis;
import com.hackathon.backend.conversation.entity.Conversation;
import com.hackathon.backend.healthrecord.entity.HealthRecord;
import com.hackathon.backend.healthrecord.repository.HealthRecordRepository;
import com.hackathon.backend.member.entity.Member;

import tools.jackson.databind.ObjectMapper;

@Service
public class HealthRecordPersistenceService {

	private final HealthRecordRepository healthRecordRepository;
	private final ObjectMapper objectMapper;

	public HealthRecordPersistenceService(HealthRecordRepository healthRecordRepository, ObjectMapper objectMapper) {
		this.healthRecordRepository = healthRecordRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public List<HealthRecord> persist(
			Member member, Conversation conversation, AiAnalysis analysis, HealthExtractionResult extractionResult) {
		Objects.requireNonNull(member, "member must not be null");
		Objects.requireNonNull(conversation, "conversation must not be null");
		Objects.requireNonNull(analysis, "analysis must not be null");
		Objects.requireNonNull(extractionResult, "extractionResult must not be null");

		List<HealthRecord> records = extractionResult.records().stream()
				.map(candidate -> toHealthRecord(member, conversation, analysis, candidate))
				.toList();
		healthRecordRepository.saveAll(records);
		return records;
	}

	private HealthRecord toHealthRecord(
			Member member, Conversation conversation, AiAnalysis analysis, HealthRecordCandidate candidate) {
		return HealthRecord.builder()
				.member(member)
				.conversation(conversation)
				.analysis(analysis)
				.type(candidate.type())
				.summary(candidate.summary())
				.detail(serializeDetail(candidate.detail()))
				.recordedDate(candidate.recordedDate())
				.recordedAt(candidate.recordedAt())
				.confidence(candidate.confidence())
				.evidence(candidate.evidence())
				.build();
	}

	private String serializeDetail(HealthExtractionResult.HealthDetail detail) {
		try {
			return objectMapper.writeValueAsString(detail);
		} catch (Exception e) {
			throw new IllegalStateException("health record detail serialization failed", e);
		}
	}
}
