package com.hackathon.backend.item;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB 까지 붙인 상태로 실제 요청 흐름을 확인합니다.
 *
 * 슬라이스 테스트는 서비스를 대역으로 두므로 JPA 매핑, 트랜잭션, 예외 변환이
 * 실제로 이어지는지는 검증하지 못합니다. 특히 유니크 제약 위반이 409 로
 * 변환되는 경로는 여기서만 확인됩니다.
 *
 * @Transactional 로 각 테스트 후 롤백해 테스트 간 데이터 간섭을 막습니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ItemApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("생성한 항목을 목록과 단건 조회로 다시 읽을 수 있습니다")
	void createThenRead() throws Exception {
		String location = mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "통합 테스트 항목"}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getHeader("Location");

		mockMvc.perform(get(location))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value("통합 테스트 항목"))
				// 한글이 ??? 로 깨지지 않는지 함께 확인합니다.
				.andExpect(jsonPath("$.data.createdAt").exists());

		mockMvc.perform(get("/api/items"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].name").value("통합 테스트 항목"));
	}

	@Test
	@DisplayName("목록은 최신순으로 반환합니다")
	void findAllReturnsNewestFirst() throws Exception {
		// 컨트롤러 테스트는 목이 준 순서를 그대로 보므로 정렬은 여기서만 검증됩니다.
		createItem("먼저");
		createItem("나중");

		mockMvc.perform(get("/api/items"))
				.andExpect(jsonPath("$.data[0].name").value("나중"))
				.andExpect(jsonPath("$.data[1].name").value("먼저"));
	}

	@Test
	@DisplayName("이름 필드 자체가 없으면 400 을 반환합니다")
	void missingNameFieldReturnsBadRequest() throws Exception {
		// null 과 빈 문자열은 검증 경로가 다릅니다.
		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	@DisplayName("이름이 중복되면 409 를 반환합니다")
	void duplicateNameReturnsConflict() throws Exception {
		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "중복 항목"}
								"""))
				.andExpect(status().isCreated());

		// DataIntegrityViolationException 이 500 이 아니라 409 로 변환되어야 합니다.
		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "중복 항목"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("CONFLICT"));
	}

	@Test
	@DisplayName("충돌 응답에 제약 조건 이름이 노출되지 않습니다")
	void conflictHidesConstraintDetails() throws Exception {
		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "노출 확인"}
								"""))
				.andExpect(status().isCreated());

		String body = mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "노출 확인"}
								"""))
				.andReturn()
				.getResponse()
				.getContentAsString();

		// items 같은 흔한 단어는 정상 필드값에도 나올 수 있으므로 검사하지 않습니다.
		// 응답 형태 자체의 고정은 GlobalExceptionHandlerTest 가 담당합니다.
		org.assertj.core.api.Assertions.assertThat(body)
				.doesNotContainIgnoringCase("constraint")
				.doesNotContainIgnoringCase("unique");
	}

	@Test
	@DisplayName("같은 항목을 두 번 삭제하면 두 번째는 404 입니다")
	void deletingTwiceReturnsNotFound() throws Exception {
		String location = createItem("두번 삭제");

		mockMvc.perform(delete(location)).andExpect(status().isOk());
		mockMvc.perform(delete(location)).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("삭제한 항목은 조회되지 않습니다")
	void deleteThenNotFound() throws Exception {
		String location = mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "삭제 대상"}
								"""))
				.andReturn()
				.getResponse()
				.getHeader("Location");

		mockMvc.perform(delete(location)).andExpect(status().isOk());
		mockMvc.perform(get(location)).andExpect(status().isNotFound());
	}

	/**
	 * 요청 본문을 만들어 생성하고 Location 을 돌려주는 보조 메서드입니다.
	 */
	private String createItem(String name) throws Exception {
		return mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"" + name + "\"}"))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getHeader("Location");
	}

	@Test
	@DisplayName("DB 가 붙어 있으면 /health 의 database 가 UP 입니다")
	void healthReportsDatabaseUp() throws Exception {
		mockMvc.perform(get("/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.database").value("UP"));
	}
}
