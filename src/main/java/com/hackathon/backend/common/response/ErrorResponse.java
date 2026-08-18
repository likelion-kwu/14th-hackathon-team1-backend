package com.hackathon.backend.common.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 실패 응답의 문서화 전용 타입입니다. 런타임에는 쓰이지 않습니다.
 *
 * 실제 실패 응답은 ApiResponse.failure(...) 가 만듭니다. 그 타입은 제네릭이라
 * 스펙에 ApiResponseVoid, ApiResponseItemResponse 처럼 성공 응답과 뒤섞인
 * 이름으로 흩어집니다. 프론트가 "실패는 이 모양 하나" 라는 것을 문서에서
 * 한눈에 보게 하려고 이름 붙은 스키마를 따로 둡니다.
 *
 * 필드 구성은 ApiResponse 와 반드시 같아야 합니다. 한쪽만 바꾸면 문서와 실제
 * 응답이 갈라지고, 그 사실이 컴파일러에 잡히지 않습니다.
 *
 * @param success 항상 false 입니다
 * @param data    항상 null 입니다
 * @param error   오류 정보입니다
 */
@Schema(name = "ErrorResponse", description = "실패 응답입니다. success 는 항상 false, data 는 항상 null 입니다.")
public record ErrorResponse(

		@Schema(description = "요청 처리 성공 여부입니다. 실패 응답이므로 항상 false 입니다.", example = "false")
		boolean success,

		@Schema(description = "실패 응답이므로 항상 null 입니다.", nullable = true)
		Object data,

		ApiError error) {
}
