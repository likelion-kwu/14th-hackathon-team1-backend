package com.hackathon.backend.common.response;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 이 엔드포인트가 성공 시 201 을 반환한다는 표시입니다.
 *
 * ResponseEntity.created(...) 로 201 을 반환해도 springdoc 은 스펙에 200 으로
 * 적습니다. 반환 타입이 ResponseEntity 라 실제 상태 코드를 알 수 없어 기본값을
 * 쓰기 때문입니다. 그대로 두면 문서와 실제 응답이 어긋나고, 상태 코드로 분기하는
 * 프론트 코드가 연동 첫날에 깨집니다.
 *
 * @ResponseStatus 로 해결되지 않습니다. 그 애노테이션은 ResponseEntity 를 반환할
 * 때 무시되는데, Location 헤더를 실으려면 ResponseEntity 가 필요합니다.
 *
 * ApiErrorResponseCustomizer 가 이 표시를 읽어 200 항목을 201 로 옮깁니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiCreated {
}
