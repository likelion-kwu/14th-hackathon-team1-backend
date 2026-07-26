package com.hackathon.backend.common.response;

/**
 * 실패 응답의 오류 코드입니다.
 *
 * 프론트가 이 값으로 분기하는 것이 공통 응답 래퍼의 존재 이유이므로,
 * 가능한 코드 목록을 한 곳에 모읍니다. 문자열 리터럴로 흩어두면
 * "어떤 code 가 올 수 있는지" 를 답할 수 있는 단일 지점이 없어집니다.
 *
 * enum 이므로 Swagger 스펙에도 열거값으로 나가고, 예외 핸들러의 두 경로
 * (예외 종류에서 직접 만드는 쪽과 상태 코드에서 유도하는 쪽)가 같은 값을
 * 쓰도록 컴파일러가 보장합니다.
 */
public enum ErrorCode {

	/** 요청 값 검증 실패입니다. fieldErrors 에 필드별 사유가 담깁니다. */
	VALIDATION_FAILED("요청 값이 올바르지 않습니다."),

	/** 본문이 JSON 으로 파싱되지 않습니다. 깨진 JSON 이나 타입 불일치입니다. */
	MALFORMED_REQUEST("요청 본문을 해석할 수 없습니다."),

	/** 요청한 리소스가 없습니다. */
	NOT_FOUND("요청한 리소스를 찾을 수 없습니다."),

	/** 경로는 있지만 해당 HTTP 메서드를 지원하지 않습니다. */
	METHOD_NOT_ALLOWED("허용되지 않은 요청 방식입니다."),

	/** Content-Type 이나 Accept 를 처리할 수 없습니다. */
	UNSUPPORTED_MEDIA_TYPE("지원하지 않는 요청 형식입니다."),

	/** 요청 본문이 허용 크기를 초과했습니다. */
	PAYLOAD_TOO_LARGE("요청 크기가 허용 범위를 초과했습니다."),

	/** 유니크 제약 위반입니다. 같은 값으로 다시 요청하면 또 실패합니다. */
	CONFLICT("이미 존재하는 값입니다."),

	/** 다른 요청과 동시에 같은 데이터를 수정했습니다. 재시도하면 성공할 수 있습니다. */
	CONCURRENT_MODIFICATION("다른 요청과 충돌했습니다. 다시 시도해 주세요."),

	/** 위에 해당하지 않는 클라이언트 오류입니다. */
	BAD_REQUEST("잘못된 요청입니다."),

	/** 서버 오류입니다. 실제 원인은 응답에 담지 않고 서버 로그에만 남깁니다. */
	INTERNAL_ERROR("서버 오류가 발생했습니다.");

	private final String defaultMessage;

	ErrorCode(String defaultMessage) {
		this.defaultMessage = defaultMessage;
	}

	public String defaultMessage() {
		return defaultMessage;
	}
}
