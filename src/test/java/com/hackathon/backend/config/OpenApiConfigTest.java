package com.hackathon.backend.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

	/**
	 * 프론트에 넘긴 엔드포인트가 스펙에서 사라지지 않았는지 확인합니다.
	 *
	 * 컨트롤러를 옮기거나 @Hidden 을 잘못 붙이면 스펙에서 조용히 빠집니다.
	 * 앱은 정상 기동하고 테스트도 통과하므로, 프론트가 문서를 열어보기 전까지
	 * 아무도 모릅니다.
	 */
	@ParameterizedTest
	@CsvSource({
			"/api/members,post",
			"/api/members/{memberId},get",
			"/api/members/{memberId}/notification,patch",
			"/api/members/{memberId}/fcm-token,put",
			"/api/members/{memberId}/streak,get",
			"/api/conversations,get",
			"/api/conversations,post",
			"/api/conversations/{conversationId},get",
			"/api/conversations/{conversationId}/messages,get",
			"/api/conversations/{conversationId}/messages,post",
			"/api/conversations/{conversationId}/complete,patch",
			"/api/health-records,get",
			"/api/health-records/today,get",
			"/api/health-records/{healthRecordId}/confirm,patch",
			"/api/ai-analyses,get",
			"/api/summaries/daily,get",
			"/api/summaries/weekly,get",
			"/api/summaries/monthly,get",
			"/api/summaries/overall,get",
	})
	@DisplayName("프론트에 공개하는 엔드포인트가 스펙에 모두 실립니다")
	void publishesEveryEndpoint(String path, String method) throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['%s'].%s".formatted(path, method)).exists());
	}

	/**
	 * 실패 응답이 스펙에 실리는지 확인합니다.
	 *
	 * 이것이 이 프로젝트에서 가장 조용히 깨지는 부분입니다. ApiErrorResponseCustomizer
	 * 가 동작하지 않아도 앱은 정상이고 성공 응답도 정상이라, 스펙에서 400/404/500
	 * 이 통째로 사라진 것을 아무도 눈치채지 못합니다.
	 */
	@Test
	@DisplayName("/api 엔드포인트에는 공통 실패 응답이 붙습니다")
	void documentsCommonErrorResponses() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.components.schemas.ErrorResponse").exists())
				// 400 과 500 은 조건 없이 모든 /api 엔드포인트에 붙습니다.
				.andExpect(jsonPath("$.paths['/api/summaries/daily'].get.responses.400").exists())
				.andExpect(jsonPath("$.paths['/api/summaries/daily'].get.responses.500").exists())
				// @ApiNotFound 를 붙인 곳에만 404 가 붙습니다.
				.andExpect(jsonPath("$.paths['/api/summaries/daily'].get.responses.404").exists())
				// @ApiConflict 를 붙인 곳에만 409 가 붙습니다.
				.andExpect(jsonPath("$.paths['/api/members'].post.responses.409").exists())
				// 실패 응답은 ErrorResponse 하나를 가리켜야 합니다.
				.andExpect(jsonPath("$.paths['/api/summaries/daily'].get.responses.400"
						+ ".content['application/json'].schema.$ref")
						.value("#/components/schemas/ErrorResponse"));
	}

	/**
	 * 실제 응답 상태 코드와 문서가 어긋나지 않는지 확인합니다.
	 *
	 * ResponseEntity 를 반환하면 springdoc 은 기본값 200 을 적습니다. @ApiCreated
	 * 표시가 빠지면 201 을 반환하는 API 가 문서에는 200 으로 실려, 상태 코드로
	 * 분기하는 프론트 코드가 깨집니다.
	 */
	@Test
	@DisplayName("회원 가입은 201 로 문서화됩니다")
	void documentsCreatedStatus() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/members'].post.responses.201").exists())
				.andExpect(jsonPath("$.paths['/api/members'].post.responses.200").doesNotExist());
	}

	/**
	 * /health 는 공통 래퍼를 쓰지 않으므로 공통 실패 응답도 붙으면 안 됩니다.
	 * 붙으면 문서가 존재하지 않는 응답 형태를 약속하게 됩니다.
	 */
	@Test
	@DisplayName("/health 에는 공통 실패 응답을 붙이지 않습니다")
	void leavesHealthUntouched() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/health'].get.responses.400").doesNotExist())
				.andExpect(jsonPath("$.paths['/health'].get.responses.500").doesNotExist());
	}

	/**
	 * 샘플 CRUD 는 프론트에 보이면 안 됩니다. 쓰면 안 되는 API 가 명세에 있으면
	 * 반드시 누군가 씁니다.
	 */
	@Test
	@DisplayName("샘플 CRUD 는 스펙에 노출되지 않습니다")
	void hidesSampleCrud() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/items']").doesNotExist());
	}
}
