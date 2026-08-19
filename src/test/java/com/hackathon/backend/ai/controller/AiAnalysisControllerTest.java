package com.hackathon.backend.ai.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.hackathon.backend.ai.dto.AiAnalysisResponse;
import com.hackathon.backend.ai.entity.AiAnalysis;
import com.hackathon.backend.ai.service.AiAnalysisService;
import com.hackathon.backend.common.exception.NotFoundException;

@WebMvcTest(AiAnalysisController.class)
class AiAnalysisControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AiAnalysisService aiAnalysisService;

	@Test
	@DisplayName("분석 상태를 공통 성공 응답으로 반환합니다")
	void returnsAnalysisStatus() throws Exception {
		given(aiAnalysisService.findByConversation(1L, AiAnalysis.TaskType.HEALTH_EXTRACTION))
				.willReturn(new AiAnalysisResponse(
						10L, 2L, 1L, AiAnalysis.TaskType.HEALTH_EXTRACTION,
						AiAnalysis.AnalysisStatus.PROCESSING, "test-model", "v1",
						LocalDateTime.of(2026, 8, 20, 21, 0), LocalDateTime.of(2026, 8, 20, 21, 1)));

		mockMvc.perform(get("/api/ai-analyses")
						.param("conversationId", "1")
						.param("taskType", "HEALTH_EXTRACTION"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(10))
				.andExpect(jsonPath("$.data.status").value("PROCESSING"));
	}

	@Test
	@DisplayName("해당 분석 작업이 없으면 404를 반환합니다")
	void returnsNotFoundWhenAnalysisDoesNotExist() throws Exception {
		given(aiAnalysisService.findByConversation(1L, AiAnalysis.TaskType.HEALTH_EXTRACTION))
				.willThrow(new NotFoundException("해당 조건의 분석 작업이 아직 없습니다."));

		mockMvc.perform(get("/api/ai-analyses")
						.param("conversationId", "1")
						.param("taskType", "HEALTH_EXTRACTION"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
	}
}
