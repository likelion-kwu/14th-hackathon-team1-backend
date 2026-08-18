package com.hackathon.backend.common.response;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 이 엔드포인트가 409 를 반환할 수 있다는 표시입니다.
 *
 * 유니크 제약이 걸린 값을 쓰는 엔드포인트에 붙입니다. 지금은 Member.phone 이
 * 유일한 유니크 컬럼이라 회원 가입 하나뿐입니다.
 *
 * 자세한 배경은 ApiNotFound 주석을 보십시오.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiConflict {

	/** 이 엔드포인트에서 409 가 나는 상황을 적습니다. 그대로 문서에 나갑니다. */
	String value() default "이미 존재하는 값입니다.";
}
