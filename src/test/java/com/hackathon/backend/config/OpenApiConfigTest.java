package com.hackathon.backend.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * springdoc 이 Boot 4 에서 실제로 동작하는지 확인합니다.
 *
 * springdoc 2.x 는 Spring Framework 6 대상이라 Boot 4 에서 동작하지 않습니다.
 * 누군가 버전을 내리거나 Boot 를 올릴 때 이 테스트가 먼저 깨져서 알려줍니다.
 * 슬라이스 테스트로는 springdoc 자동 구성이 올라오지 않아 전체 컨텍스트를 띄웁니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("API 스펙을 반환합니다")
	void servesApiDocs() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("Hackathon Backend API"))
				.andExpect(jsonPath("$.paths['/health']").exists())
				// 테스트 픽스처 컨트롤러가 스펙에 섞이지 않아야 합니다.
				.andExpect(jsonPath("$.paths['/test/validate']").doesNotExist());
	}

	@Test
	@DisplayName("Swagger UI 를 제공합니다")
	void servesSwaggerUi() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isOk());
	}
}
