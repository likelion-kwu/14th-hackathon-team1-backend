package com.hackathon.backend.ai.service;

/** AI 모델 응답이 약속된 JSON 계약을 따르지 않을 때 발생합니다. */
public class AiResultValidationException extends RuntimeException {

	public AiResultValidationException(String message) {
		super(message);
	}

	public AiResultValidationException(String message, Throwable cause) {
		super(message, cause);
	}
}
