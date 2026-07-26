package com.hackathon.testfixture;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.backend.common.exception.NotFoundException;
import com.hackathon.backend.common.response.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * 전역 예외 핸들러를 검증하기 위한 테스트 전용 컨트롤러입니다.
 * 실제 엔드포인트에 의존하면 그 엔드포인트가 바뀔 때 예외 처리 테스트가
 * 함께 깨지므로, 검증 대상을 분리합니다.
 *
 * 패키지를 com.hackathon.backend 밖에 두는 이유가 있습니다. 테스트 소스도
 * 테스트 런타임 클래스패스에 있으므로, 애플리케이션과 같은 패키지 트리에 두면
 * @SpringBootTest 의 컴포넌트 스캔에 걸립니다. 그러면 이 픽스처 경로 3개가
 * /v3/api-docs 스펙에 섞이고, 다른 테스트의 컨텍스트가 이 픽스처에 결합됩니다.
 * 실제로 스펙에 /test/validate, /test/not-found, /test/boom 이 노출되는 것을
 * 확인했습니다.
 */
@RestController
public class ExceptionTestController {

	private final ValidatedFixtureService validatedService;

	ExceptionTestController(ValidatedFixtureService validatedService) {
		this.validatedService = validatedService;
	}

	public record Request(@NotBlank(message = "name 은 필수입니다") String name) {
	}

	@PostMapping("/test/validate")
	public ApiResponse<String> validate(@Valid @RequestBody Request request) {
		return ApiResponse.success(request.name());
	}

	@GetMapping("/test/search")
	public ApiResponse<String> search(@RequestParam @NotBlank(message = "keyword 는 필수입니다") String keyword) {
		return ApiResponse.success(keyword);
	}

	/**
	 * 서비스 계층의 @Validated 빈에서 나오는 ConstraintViolationException 경로입니다.
	 * 컨트롤러 파라미터 검증(HandlerMethodValidationException)과 다른 예외가 나옵니다.
	 */
	@GetMapping("/test/service-validate")
	public ApiResponse<String> serviceValidate(@RequestParam String keyword) {
		return ApiResponse.success(validatedService.echo(keyword));
	}

	@PostMapping("/test/not-found")
	public ApiResponse<Void> notFound() {
		throw new NotFoundException("항목을 찾을 수 없습니다");
	}

	/** 중복 키 위반입니다. 409 로 변환되어야 합니다. */
	@PostMapping("/test/duplicate")
	public ApiResponse<Void> duplicate() {
		throw new DuplicateKeyException("Duplicate entry 'x' for key 'items.uk_items_name'");
	}

	/**
	 * 중복 키가 아닌 제약 위반입니다(NOT NULL 등).
	 * 클라이언트 잘못이 아니므로 409 가 아니라 500 이어야 합니다.
	 */
	@PostMapping("/test/integrity")
	public ApiResponse<Void> integrity() {
		throw new DataIntegrityViolationException("Column 'name' cannot be null");
	}

	/** 동시 수정 충돌입니다. 재시도하면 성공할 수 있으므로 코드가 구분되어야 합니다. */
	@PostMapping("/test/optimistic")
	public ApiResponse<Void> optimistic() {
		throw new OptimisticLockingFailureException("행이 이미 변경되었습니다");
	}

	@PostMapping("/test/boom")
	public ApiResponse<Void> boom() {
		throw new IllegalStateException("내부 구현이 드러나는 메시지");
	}
}
