package com.hackathon.backend.common.exception;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.hackathon.backend.common.response.ApiError;
import com.hackathon.backend.common.response.ApiResponse;
import com.hackathon.backend.common.response.ErrorCode;

import jakarta.validation.ConstraintViolationException;

/**
 * 예외를 공통 응답 형태로 변환합니다.
 *
 * 프론트가 에러 처리 코드를 한 번만 작성하면 되도록 하는 것이 목적입니다.
 * 핸들러가 없으면 Spring 기본 오류 응답(ProblemDetail)이 나가서 성공 응답과
 * 형태가 달라집니다.
 *
 * ResponseEntityExceptionHandler 를 상속하는 이유는 Spring MVC 표준 예외 20여 개를
 * 올바른 상태 코드로 처리하기 위해서입니다. 상속하지 않고 Exception 만 잡으면
 * 후보 핸들러가 하나뿐이어서 프레임워크의 4xx 예외까지 전부 그쪽으로 갑니다.
 * 실제로 매핑이 없는 경로 요청(NoResourceFoundException, 404)이 500 으로
 * 응답되는 것을 확인했습니다.
 *
 * 응답 메시지 정책입니다.
 * - 검증 실패와 잘못된 요청 본문: 원인이 클라이언트에 있으므로 사유를 알려줍니다
 * - NotFoundException: 던지는 쪽이 정한 메시지를 전달합니다. 그래서 그 메시지에
 *   내부 식별자나 테이블명을 넣지 않아야 합니다
 * - 그 외: 고정 문구를 내보내고 실제 원인은 서버 로그에만 남깁니다.
 *   스택트레이스나 SQL 이 응답으로 새어 나가는 것을 막기 위해서입니다
 *
 * CORS preflight 거부(403)는 이 핸들러를 거치지 않습니다. DefaultCorsProcessor 가
 * 응답을 직접 쓰기 때문입니다. 따라서 그 응답만 공통 형태가 아닙니다.
 * 브라우저가 preflight 응답 본문을 읽지 않으므로 실질적인 문제는 없습니다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/**
	 * 요청 본문(@RequestBody) 검증 실패입니다. 어떤 필드가 왜 실패했는지 함께 내려줍니다.
	 */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		List<ApiError.FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
				.map(error -> new ApiError.FieldError(error.getField(), error.getDefaultMessage()))
				.toList();

		return badRequest(ApiError.ofFields(ErrorCode.VALIDATION_FAILED, fieldErrors));
	}

	/**
	 * 쿼리 파라미터나 경로 변수(@RequestParam, @PathVariable)의 검증 실패입니다.
	 *
	 * 이것도 검증 실패이므로 요청 본문 검증과 같은 코드로 통일합니다. 오버라이드하지
	 * 않으면 handleExceptionInternal 로 빠져서 코드가 BAD_REQUEST 로, fieldErrors 가
	 * 빈 배열로 나갑니다. 값이 어디에 실려 왔는지에 따라 응답 형태가 갈리면
	 * 프론트가 분기를 두 번 작성해야 합니다.
	 */
	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException e,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		List<ApiError.FieldError> fieldErrors = e.getParameterValidationResults().stream()
				.flatMap(result -> result.getResolvableErrors().stream()
						.map(error -> new ApiError.FieldError(
								result.getMethodParameter().getParameterName(),
								error.getDefaultMessage())))
				.toList();

		return badRequest(ApiError.ofFields(ErrorCode.VALIDATION_FAILED, fieldErrors));
	}

	/**
	 * 본문이 JSON 으로 파싱되지 않는 경우입니다. 타입 불일치나 깨진 JSON 이 여기로 옵니다.
	 * 검증 실패와 구분되는 코드를 주어야 프론트가 원인을 알 수 있습니다.
	 */
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {

		log.warn("요청 본문을 읽을 수 없습니다: {}", e.getMessage());
		return badRequest(ApiError.of(ErrorCode.MALFORMED_REQUEST));
	}

	/**
	 * 위에서 개별로 다루지 않은 Spring MVC 표준 예외입니다.
	 *
	 * 상태 코드는 상위 클래스가 판정한 값을 그대로 쓰고 본문만 공통 형태로 감쌉니다.
	 * headers 를 그대로 넘기는 것이 중요합니다. 405 응답의 Allow 헤더처럼 상위
	 * 클래스가 채워둔 값이 여기서 유실되면 응답이 규격에 맞지 않게 됩니다.
	 *
	 * 5xx 는 여기서 직접 로그를 남깁니다. 상위 클래스는 로그를 남기지 않고,
	 * @ExceptionHandler(Exception.class) 는 상위 클래스가 먼저 잡은 예외에 도달하지
	 * 않기 때문입니다. 특히 open-in-view 를 끈 상태에서 발생하는
	 * LazyInitializationException 은 직렬화 시점에 터져
	 * HttpMessageNotWritableException(500) 으로 감싸지는데, 이 로그가 없으면
	 * journalctl 에 아무 단서도 남지 않습니다.
	 *
	 * 반환값이 null 일 수 있습니다. 응답이 이미 커밋된 뒤에는 상위 클래스가
	 * null 을 반환합니다.
	 */
	@Override
	protected @Nullable ResponseEntity<Object> handleExceptionInternal(Exception e, @Nullable Object body,
			HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

		if (statusCode.is5xxServerError()) {
			log.error("서버 오류로 처리된 예외입니다. status={}", statusCode.value(), e);
		} else {
			log.warn("클라이언트 오류로 처리된 예외입니다. status={}, message={}", statusCode.value(), e.getMessage());
		}

		ApiError error = ApiError.of(codeOf(statusCode));
		return super.handleExceptionInternal(e, ApiResponse.failure(error), headers, statusCode, request);
	}

	/**
	 * 서비스 계층의 @Validated 빈에서 나오는 검증 실패입니다.
	 *
	 * 컨트롤러 파라미터 검증은 Spring 내장 처리로 HandlerMethodValidationException 이
	 * 되지만, AOP 프록시를 거치는 빈은 ConstraintViolationException 을 던집니다.
	 * 이 예외는 Spring MVC 표준 예외 목록에 없어서, 처리하지 않으면 클라이언트
	 * 잘못인데도 500 으로 응답됩니다.
	 *
	 * 위반 경로(propertyPath)의 마지막 노드를 필드 이름으로 씁니다.
	 * 전체 경로는 "echo.keyword" 처럼 메서드명을 포함해 내부 구현을 드러냅니다.
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
		List<ApiError.FieldError> fieldErrors = e.getConstraintViolations().stream()
				.map(violation -> new ApiError.FieldError(
						lastNodeOf(violation.getPropertyPath().toString()),
						violation.getMessage()))
				.toList();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.failure(ApiError.ofFields(ErrorCode.VALIDATION_FAILED, fieldErrors)));
	}

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleNotFound(NotFoundException e) {
		ApiError error = ApiError.of(ErrorCode.NOT_FOUND, e.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(error));
	}

	/**
	 * Spring MVC 표준 예외에 해당하지 않는 모든 예외입니다.
	 *
	 * 예외를 그대로 넘겨 스택트레이스를 남깁니다. 배포 후 문제가 생기면
	 * journalctl -u hackathon 에서 이 로그부터 봅니다.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
		log.error("처리되지 않은 예외가 발생했습니다.", e);

		ApiError error = ApiError.of(ErrorCode.INTERNAL_ERROR);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.failure(error));
	}

	private String lastNodeOf(String propertyPath) {
		int lastDot = propertyPath.lastIndexOf('.');
		return lastDot < 0 ? propertyPath : propertyPath.substring(lastDot + 1);
	}

	private ResponseEntity<Object> badRequest(ApiError error) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
	}

	/**
	 * 상태 코드를 프론트가 분기에 쓸 코드로 변환합니다.
	 * 4xx 를 하나로 뭉개면 상태 코드가 code 보다 많은 정보를 주는 상태가 되므로,
	 * 구분이 필요한 것들은 개별 코드를 부여합니다.
	 */
	private ErrorCode codeOf(HttpStatusCode statusCode) {
		int value = statusCode.value();
		if (value == HttpStatus.NOT_FOUND.value()) {
			return ErrorCode.NOT_FOUND;
		}
		if (value == HttpStatus.METHOD_NOT_ALLOWED.value()) {
			return ErrorCode.METHOD_NOT_ALLOWED;
		}
		if (value == HttpStatus.UNSUPPORTED_MEDIA_TYPE.value() || value == HttpStatus.NOT_ACCEPTABLE.value()) {
			return ErrorCode.UNSUPPORTED_MEDIA_TYPE;
		}
		if (value == HttpStatus.PAYLOAD_TOO_LARGE.value()) {
			return ErrorCode.PAYLOAD_TOO_LARGE;
		}
		if (statusCode.is4xxClientError()) {
			return ErrorCode.BAD_REQUEST;
		}
		return ErrorCode.INTERNAL_ERROR;
	}
}
