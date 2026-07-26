package com.hackathon.backend.common.response;

/**
 * /api/** 응답의 공통 형태입니다.
 *
 * 프론트가 성공/실패 분기를 한 번만 작성하면 되도록 형태를 통일합니다.
 * success 가 true 면 data 가 채워지고 error 는 null 입니다. 반대도 마찬가지입니다.
 *
 * /health 에는 이 래퍼를 씌우지 않습니다. 배포 스모크 테스트와 모니터링이
 * 파싱하는 경로라 형태를 고정해 두어야 합니다.
 *
 * 전역 ResponseBodyAdvice 로 자동 래핑하지 않고 컨트롤러가 명시적으로 감쌉니다.
 * 자동 래핑은 /health 나 springdoc 의 /v3/api-docs 까지 함께 감싸버려서
 * 배포 파이프라인과 Swagger 를 조용히 깨뜨립니다.
 *
 * @param success 요청 처리 성공 여부입니다
 * @param data    성공 시의 응답 본문입니다. 실패 시에는 null 입니다
 * @param error   실패 시의 오류 정보입니다. 성공 시에는 null 입니다
 */
public record ApiResponse<T>(boolean success, T data, ApiError error) {

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, data, null);
	}

	public static <T> ApiResponse<T> failure(ApiError error) {
		return new ApiResponse<>(false, null, error);
	}
}
