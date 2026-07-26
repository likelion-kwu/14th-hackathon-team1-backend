package com.hackathon.backend.item;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.hackathon.backend.common.exception.NotFoundException;

/**
 * 프론트가 실제로 호출하는 계약을 고정합니다.
 * 서비스는 대역으로 두고 웹 계층의 응답 형태와 상태 코드만 확인합니다.
 */
@WebMvcTest(ItemController.class)
class ItemControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ItemService itemService;

	@Test
	@DisplayName("생성에 성공하면 201 과 Location 헤더를 반환합니다")
	void createReturnsCreated() throws Exception {
		given(itemService.create(any())).willReturn(new ItemResponse(1L, "항목", OffsetDateTime.now()));

		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "항목"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/items/1"))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(1))
				.andExpect(jsonPath("$.data.name").value("항목"))
				.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	@DisplayName("createdAt 은 오프셋을 포함해 응답합니다")
	void createdAtCarriesOffset() throws Exception {
		given(itemService.create(any())).willReturn(new ItemResponse(1L, "항목", OffsetDateTime.now()));

		// 오프셋이 없으면 프론트가 브라우저 로컬 타임존으로 해석해버립니다.
		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "항목"}
								"""))
				.andExpect(jsonPath("$.data.createdAt",
						org.hamcrest.Matchers.matchesPattern("\\d{4}-\\d{2}-\\d{2}T[\\d:.]+(Z|[+-]\\d{2}:\\d{2})")));
	}

	@Test
	@DisplayName("이름이 비면 400 을 반환하고 서비스를 호출하지 않습니다")
	void rejectsBlankName() throws Exception {
		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "  "}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fieldErrors[0].field").value("name"));

		// 검증이 통과해버리면 잘못된 데이터가 저장됩니다.
		then(itemService).should(never()).create(any());
	}

	@Test
	@DisplayName("목록은 공통 래퍼의 data 배열로 반환합니다")
	void findAllReturnsArray() throws Exception {
		given(itemService.findAll()).willReturn(List.of(
				new ItemResponse(2L, "두번째", OffsetDateTime.now()),
				new ItemResponse(1L, "첫번째", OffsetDateTime.now())));

		mockMvc.perform(get("/api/items"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data[0].id").value(2));
	}

	@Test
	@DisplayName("없는 항목을 조회하면 404 를 반환합니다")
	void findByIdReturnsNotFound() throws Exception {
		given(itemService.findById(99L)).willThrow(new NotFoundException("항목을 찾을 수 없습니다."));

		mockMvc.perform(get("/api/items/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
	}

	@Test
	@DisplayName("id 가 숫자가 아니면 400 을 반환합니다")
	void rejectsNonNumericId() throws Exception {
		mockMvc.perform(get("/api/items/abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false));
	}

	@Test
	@DisplayName("삭제에 성공하면 공통 래퍼로 200 을 반환합니다")
	void deleteReturnsSuccess() throws Exception {
		mockMvc.perform(delete("/api/items/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		then(itemService).should().delete(1L);
	}

	@Test
	@DisplayName("없는 항목을 삭제하면 404 를 반환합니다")
	void deleteReturnsNotFound() throws Exception {
		willThrow(new NotFoundException("항목을 찾을 수 없습니다.")).given(itemService).delete(99L);

		mockMvc.perform(delete("/api/items/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
	}
}
