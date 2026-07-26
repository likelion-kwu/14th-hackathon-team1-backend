package com.hackathon.backend.common.exception;

/**
 * 요청한 리소스가 없을 때 던집니다. 전역 예외 핸들러가 404 로 변환합니다.
 *
 * 서비스 계층이 HTTP 상태 코드를 직접 다루지 않도록 하는 것이 목적입니다.
 * ResponseStatusException 을 서비스에서 던지면 웹 계층 관심사가 도메인으로
 * 새어 들어옵니다.
 */
public class NotFoundException extends RuntimeException {

	public NotFoundException(String message) {
		super(message);
	}
}
