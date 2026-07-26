package com.hackathon.backend.common.response;

import java.util.List;

/**
 * 실패 응답의 오류 정보입니다.
 *
 * code 는 프론트가 분기에 쓰는 값이므로 HTTP 상태 코드와 별개로 둡니다.
 * 상태 코드만으로는 같은 400 안에서 어떤 실패인지 구분할 수 없습니다.
 *
 * message 는 사람이 읽는 용도입니다. 예외의 원문 메시지를 그대로 넣지 않습니다.
 * 스택트레이스나 SQL 이 섞여 나갈 수 있습니다. 예외를 던지는 쪽이 메시지를
 * 정하는 경우(NotFoundException)에는 그 메시지에 내부 식별자나 테이블명을
 * 넣지 않도록 주의해야 합니다.
 *
 * @param code        오류 구분 코드입니다
 * @param message     사람이 읽는 설명입니다. 항상 값이 있습니다
 * @param fieldErrors 검증 실패 시의 필드별 사유입니다. 그 외에는 빈 목록입니다
 */
public record ApiError(ErrorCode code, String message, List<FieldError> fieldErrors) {

	public static ApiError of(ErrorCode code) {
		return new ApiError(code, code.defaultMessage(), List.of());
	}

	/**
	 * 기본 문구 대신 구체적인 메시지를 쓸 때 사용합니다.
	 * message 가 비어 있으면 기본 문구로 대체합니다. 응답의 message 가 null 이 되면
	 * 프론트가 매번 null 검사를 해야 하기 때문입니다.
	 */
	public static ApiError of(ErrorCode code, String message) {
		String resolved = (message == null || message.isBlank()) ? code.defaultMessage() : message;
		return new ApiError(code, resolved, List.of());
	}

	public static ApiError ofFields(ErrorCode code, List<FieldError> fieldErrors) {
		return new ApiError(code, code.defaultMessage(), fieldErrors);
	}

	/**
	 * @param field   검증에 실패한 필드 이름입니다
	 * @param message 해당 필드의 실패 사유입니다
	 */
	public record FieldError(String field, String message) {
	}
}
