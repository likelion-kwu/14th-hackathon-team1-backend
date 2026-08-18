package com.hackathon.backend.common.response;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 이 엔드포인트가 404 를 반환할 수 있다는 표시입니다.
 *
 * ApiErrorResponseCustomizer 가 이 표시를 읽어 스펙에 404 응답을 추가합니다.
 * 매 메서드마다 @ApiResponse(responseCode = "404", content = @Content(...)) 를
 * 손으로 적으면 엔드포인트 수만큼 같은 다섯 줄이 반복되고, 하나를 빠뜨려도
 * 아무도 모릅니다.
 *
 * 400 과 500 은 이 표시가 없어도 모든 /api 엔드포인트에 자동으로 붙습니다.
 * 어떤 요청이든 검증에 걸리거나 서버에서 터질 수 있기 때문입니다. 반면 404 는
 * "없을 수 있는 것을 찾는" 엔드포인트에만 해당하므로 명시적으로 표시합니다.
 * 가입(POST /api/members)처럼 404 가 날 수 없는 곳에 404 를 문서화하면
 * 프론트가 쓰지 않을 분기를 만듭니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiNotFound {

	/** 이 엔드포인트에서 404 가 나는 상황을 적습니다. 그대로 문서에 나갑니다. */
	String value() default "요청한 리소스를 찾을 수 없습니다.";
}
