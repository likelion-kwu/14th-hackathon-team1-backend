package com.hackathon.backend.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 성공과 실패가 서로의 필드를 남기지 않는지, 그리고 message 가 항상 채워지는지
 * 확인합니다. 둘이 섞이거나 message 가 null 이면 프론트가 success 만 보고
 * 분기할 수 없게 됩니다.
 */
class ApiResponseTest {

	@Test
	@DisplayName("성공 응답에는 error 가 없습니다")
	void successHasNoError() {
		ApiResponse<String> response = ApiResponse.success("값");

		assertThat(response.success()).isTrue();
		assertThat(response.data()).isEqualTo("값");
		assertThat(response.error()).isNull();
	}

	@Test
	@DisplayName("실패 응답에는 data 가 없습니다")
	void failureHasNoData() {
		ApiResponse<String> response = ApiResponse.failure(ApiError.of(ErrorCode.NOT_FOUND));

		assertThat(response.success()).isFalse();
		assertThat(response.data()).isNull();
		assertThat(response.error().code()).isEqualTo(ErrorCode.NOT_FOUND);
	}

	@Test
	@DisplayName("코드만 주면 그 코드의 기본 문구를 씁니다")
	void usesDefaultMessage() {
		ApiError error = ApiError.of(ErrorCode.INTERNAL_ERROR);

		assertThat(error.message()).isEqualTo(ErrorCode.INTERNAL_ERROR.defaultMessage());
	}

	@Test
	@DisplayName("메시지가 null 이거나 공백이면 기본 문구로 대체합니다")
	void blankMessageFallsBackToDefault() {
		// NotFoundException("") 처럼 던져도 응답의 message 가 비지 않아야 합니다.
		assertThat(ApiError.of(ErrorCode.NOT_FOUND, null).message())
				.isEqualTo(ErrorCode.NOT_FOUND.defaultMessage());
		assertThat(ApiError.of(ErrorCode.NOT_FOUND, "   ").message())
				.isEqualTo(ErrorCode.NOT_FOUND.defaultMessage());
	}

	@Test
	@DisplayName("검증 실패가 아닌 오류의 fieldErrors 는 null 이 아니라 빈 목록입니다")
	void fieldErrorsDefaultsToEmptyList() {
		// null 이면 프론트가 매번 null 검사를 해야 합니다.
		assertThat(ApiError.of(ErrorCode.BAD_REQUEST).fieldErrors()).isNotNull().isEmpty();
	}

	@Test
	@DisplayName("검증 실패는 필드별 사유를 담습니다")
	void carriesFieldErrors() {
		ApiError error = ApiError.ofFields(ErrorCode.VALIDATION_FAILED,
				List.of(new ApiError.FieldError("name", "name 은 필수입니다")));

		assertThat(error.fieldErrors()).hasSize(1);
		assertThat(error.fieldErrors().get(0).field()).isEqualTo("name");
		assertThat(error.message()).isEqualTo(ErrorCode.VALIDATION_FAILED.defaultMessage());
	}
}
