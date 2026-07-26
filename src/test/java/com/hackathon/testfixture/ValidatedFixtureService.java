package com.hackathon.testfixture;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * 서비스 계층에서 @Validated 로 메서드 파라미터를 검증하는 경우를 재현합니다.
 *
 * 컨트롤러 파라미터 검증은 Spring 내장 처리로 HandlerMethodValidationException 이
 * 나오지만, 이렇게 AOP 프록시를 거치는 빈에서는 ConstraintViolationException 이
 * 나옵니다. 후자는 Spring MVC 표준 예외 목록에 없어서 별도 처리가 없으면
 * 클라이언트 잘못인데도 500 이 됩니다.
 */
@Validated
@Service
public class ValidatedFixtureService {

	public String echo(@NotBlank(message = "keyword 는 필수입니다") String keyword) {
		return keyword;
	}
}
