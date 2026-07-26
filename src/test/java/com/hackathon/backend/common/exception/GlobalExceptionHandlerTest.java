package com.hackathon.backend.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.hackathon.testfixture.ExceptionTestController;

/**
 * 프론트가 에러 처리 코드를 한 번만 작성하면 되도록, 실패 응답의 형태를 고정합니다.
 *
 * 픽스처 컨트롤러는 컴포넌트 스캔 범위 밖(com.hackathon.testfixture)에 있으므로
 * @Import 로 직접 등록합니다. 스캔 범위 안에 두면 다른 테스트의 컨텍스트와
 * /v3/api-docs 스펙에 픽스처 경로가 섞입니다.
 */
@WebMvcTest(ExceptionTestController.class)
@Import({ ExceptionTestController.class, com.hackathon.testfixture.ValidatedFixtureService.class })
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("검증 실패는 400 과 필드별 사유를 반환합니다")
	void validationFailureReturnsFieldErrors() throws Exception {
		mockMvc.perform(post("/test/validate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": ""}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.data").doesNotExist())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fieldErrors[0].field").value("name"))
				.andExpect(jsonPath("$.error.fieldErrors[0].message").value("name 은 필수입니다"));
	}

	@Test
	@DisplayName("깨진 JSON 은 검증 실패와 다른 코드로 400 을 반환합니다")
	void malformedJsonReturnsDistinctCode() throws Exception {
		mockMvc.perform(post("/test/validate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
	}

	@Test
	@DisplayName("NotFoundException 은 404 를 반환하고 메시지를 전달합니다")
	void notFoundReturns404() throws Exception {
		mockMvc.perform(post("/test/not-found"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
				.andExpect(jsonPath("$.error.message").value("항목을 찾을 수 없습니다"))
				// 직접 던진 예외 경로도 같은 응답 형태를 지켜야 합니다.
				.andExpect(jsonPath("$.data").doesNotExist())
				.andExpect(jsonPath("$.error.fieldErrors").isArray());
	}

	@Test
	@DisplayName("그 외 예외는 500 을 반환하고 내부 메시지를 노출하지 않습니다")
	void unexpectedExceptionHidesInternals() throws Exception {
		String body = mockMvc.perform(post("/test/boom"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
				.andExpect(jsonPath("$.error.message").value("서버 오류가 발생했습니다."))
				.andReturn()
				.getResponse()
				.getContentAsString();

		// 예외 메시지가 응답으로 새어 나가지 않는지 확인합니다.
		// 여기서 통과해야 스택트레이스나 SQL 노출도 함께 막힙니다.
		org.assertj.core.api.Assertions.assertThat(body).doesNotContain("내부 구현이 드러나는 메시지");
	}

	@Test
	@DisplayName("쿼리 파라미터 검증 실패도 본문 검증과 같은 코드를 씁니다")
	void queryParamValidationUsesSameCode() throws Exception {
		// 값이 본문에 실렸는지 쿼리에 실렸는지에 따라 응답 형태가 갈리면
		// 프론트가 분기를 두 번 작성해야 합니다.
		mockMvc.perform(get("/test/search").param("keyword", ""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fieldErrors[0].message").value("keyword 는 필수입니다"));
	}

	@Test
	@DisplayName("서비스 계층 검증 실패도 같은 코드를 씁니다")
	void serviceValidationUsesSameCode() throws Exception {
		// ConstraintViolationException 은 Spring MVC 표준 예외가 아니어서
		// 처리하지 않으면 500 이 됩니다.
		mockMvc.perform(get("/test/service-validate").param("keyword", " "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fieldErrors[0].field").value("keyword"))
				.andExpect(jsonPath("$.error.fieldErrors[0].message").value("keyword 는 필수입니다"));
	}

	@Test
	@DisplayName("중복 키 위반은 409 CONFLICT 를 반환합니다")
	void duplicateKeyReturnsConflict() throws Exception {
		mockMvc.perform(post("/test/duplicate"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("CONFLICT"));
	}

	@Test
	@DisplayName("중복 키가 아닌 제약 위반은 500 을 반환합니다")
	void nonDuplicateIntegrityViolationReturns500() throws Exception {
		// NOT NULL 이나 FK 위반은 검증 누락이나 스키마 문제, 즉 서버 버그입니다.
		// 409 로 내려보내면 warn 로그에 묻혀 알림에 걸리지 않습니다.
		mockMvc.perform(post("/test/integrity"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"));
	}

	@Test
	@DisplayName("동시 수정 충돌은 재시도 가능한 코드로 구분합니다")
	void optimisticLockingUsesDistinctCode() throws Exception {
		// 중복 키와 달리 같은 요청을 다시 보내면 성공할 수 있으므로
		// 프론트가 재시도 여부를 판단할 수 있어야 합니다.
		mockMvc.perform(post("/test/optimistic"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("CONCURRENT_MODIFICATION"));
	}

	@Test
	@DisplayName("충돌 응답에 제약 조건 이름이 노출되지 않습니다")
	void conflictHidesConstraintName() throws Exception {
		String body = mockMvc.perform(post("/test/duplicate"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		// 예외 메시지를 그대로 담으면 테이블·제약 이름으로 스키마가 드러납니다.
		org.assertj.core.api.Assertions.assertThat(body)
				.doesNotContain("uk_items_name")
				.doesNotContain("Duplicate entry");
	}

	@Test
	@DisplayName("매핑이 없는 경로는 404 를 반환합니다")
	void unmappedPathReturns404() throws Exception {
		// 프레임워크가 던지는 4xx 예외를 Exception 핸들러가 삼켜 500 으로 바꾸는
		// 회귀를 잡습니다. 실제로 이 경로가 500 INTERNAL_ERROR 로 응답되던 버그가 있었습니다.
		mockMvc.perform(get("/api/does-not-exist"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
	}

	@Test
	@DisplayName("허용되지 않은 메서드는 405 를 반환합니다")
	void wrongMethodReturns405() throws Exception {
		// /test/validate 는 POST 만 받습니다.
		mockMvc.perform(get("/test/validate"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"))
				// 상위 클래스가 채운 Allow 헤더가 본문 교체 과정에서 유실되지 않아야 합니다.
				.andExpect(header().string("Allow", org.hamcrest.Matchers.containsString("POST")));
	}

	@Test
	@DisplayName("실패 응답도 성공 응답과 같은 필드 구조를 가집니다")
	void failureShapeMatchesSuccessShape() throws Exception {
		// 프론트가 success 하나만 보고 분기할 수 있어야 합니다.
		mockMvc.perform(get("/api/does-not-exist"))
				.andExpect(jsonPath("$.success").exists())
				.andExpect(jsonPath("$.data").doesNotExist())
				.andExpect(jsonPath("$.error.code").exists())
				.andExpect(jsonPath("$.error.message").exists())
				.andExpect(jsonPath("$.error.fieldErrors").isArray())
				// ProblemDetail 의 필드가 섞여 나오지 않는지 확인합니다.
				.andExpect(jsonPath("$.type").doesNotExist())
				.andExpect(jsonPath("$.title").doesNotExist())
				.andExpect(jsonPath("$.detail").doesNotExist());
	}

	@Test
	@DisplayName("성공 응답은 error 없이 data 를 담습니다")
	void successResponseShape() throws Exception {
		mockMvc.perform(post("/test/validate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "hackathon"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").value("hackathon"))
				.andExpect(jsonPath("$.error").doesNotExist());
	}
}
