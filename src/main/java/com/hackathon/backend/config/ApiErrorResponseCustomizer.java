package com.hackathon.backend.config;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

import com.hackathon.backend.common.response.ApiConflict;
import com.hackathon.backend.common.response.ApiCreated;
import com.hackathon.backend.common.response.ApiNotFound;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

/**
 * /api 엔드포인트의 실패 응답을 스펙에 채워 넣습니다.
 *
 * 이것이 없으면 스펙에 200 만 실립니다. GlobalExceptionHandler 가 400/404/409/500
 * 을 아무리 정확하게 만들어도 그 사실이 문서에 전혀 드러나지 않아서, 프론트는
 * 에러 처리 코드를 스펙이 아니라 구두 전달에 의존해 작성하게 됩니다.
 *
 * 엔드포인트마다 @ApiResponse 를 손으로 붙이지 않는 이유는 분량입니다. 지금
 * /api 엔드포인트가 스무 개 남짓이고 각각 최소 세 가지 실패가 있으므로, 손으로
 * 적으면 같은 블록이 예순 번 반복됩니다. 그중 하나를 빠뜨려도 컴파일은 통과하고
 * 스펙만 조용히 비어 있게 됩니다.
 *
 * 400 과 500 은 조건 없이 붙입니다. 어떤 요청이든 검증에 걸릴 수 있고 서버에서
 * 터질 수 있습니다. 404 와 409 는 해당하는 엔드포인트에만 붙도록
 * {@link ApiNotFound}, {@link ApiConflict} 표시를 읽습니다. 날 수 없는 상태 코드를
 * 문서화하면 프론트가 쓰지 않을 분기를 만들기 때문입니다.
 *
 * /health 는 제외됩니다. 공통 래퍼를 쓰지 않는 경로라 여기서 만드는 ErrorResponse
 * 모양이 맞지 않습니다. 판별은 컨트롤러 클래스의 @RequestMapping 경로가 /api 로
 * 시작하는지로 합니다. HealthController 에는 클래스 레벨 @RequestMapping 이 없어
 * 자연스럽게 걸러집니다.
 *
 * 이미 선언된 상태 코드는 덮어쓰지 않습니다. 개별 엔드포인트가 @ApiResponse 로
 * 더 구체적인 설명을 달아 두었다면 그쪽이 이깁니다.
 */
@Component
public class ApiErrorResponseCustomizer implements OperationCustomizer {

	/**
	 * OpenApiConfig 가 components 에 등록하는 스키마를 가리킵니다.
	 * 이름을 바꾸면 양쪽을 함께 고쳐야 합니다. 어긋나면 스펙에 깨진 $ref 가 남고,
	 * Swagger UI 는 조용히 빈 응답으로 그립니다.
	 */
	private static final String ERROR_SCHEMA_REF = "#/components/schemas/ErrorResponse";

	private static final String JSON = "application/json";

	@Override
	public Operation customize(Operation operation, HandlerMethod handlerMethod) {
		if (!usesCommonWrapper(handlerMethod)) {
			return operation;
		}

		ApiResponses responses = operation.getResponses();
		if (responses == null) {
			responses = new ApiResponses();
			operation.setResponses(responses);
		}

		if (handlerMethod.hasMethodAnnotation(ApiCreated.class)) {
			promoteToCreated(responses);
		}

		addIfAbsent(responses, "400",
				"요청을 처리할 수 없습니다. code 는 VALIDATION_FAILED(검증 실패), MALFORMED_REQUEST(본문 파싱 실패), "
						+ "BAD_REQUEST(리소스 상태가 요청과 맞지 않음) 중 하나입니다.",
				example("VALIDATION_FAILED", "요청 값이 올바르지 않습니다.",
						List.of(field("memberId", "0보다 커야 합니다"))));

		ApiNotFound notFound = handlerMethod.getMethodAnnotation(ApiNotFound.class);
		if (notFound != null) {
			addIfAbsent(responses, "404", notFound.value(),
					example("NOT_FOUND", notFound.value(), List.of()));
		}

		ApiConflict conflict = handlerMethod.getMethodAnnotation(ApiConflict.class);
		if (conflict != null) {
			addIfAbsent(responses, "409", conflict.value(),
					example("CONFLICT", conflict.value(), List.of()));
		}

		addIfAbsent(responses, "500",
				"서버 오류입니다. 실제 원인은 응답에 담기지 않고 서버 로그에만 남습니다.",
				example("INTERNAL_ERROR", "서버 오류가 발생했습니다.", List.of()));

		return operation;
	}

	/**
	 * 공통 응답 래퍼를 쓰는 컨트롤러인지 판별합니다.
	 *
	 * @RestController 는 @RequestMapping 을 메타 애노테이션으로 갖지 않으므로,
	 * 클래스 레벨 @RequestMapping 이 없는 HealthController 는 여기서 null 을 받아
	 * 제외됩니다.
	 */
	private boolean usesCommonWrapper(HandlerMethod handlerMethod) {
		RequestMapping mapping =
				AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequestMapping.class);
		if (mapping == null) {
			return false;
		}
		return Arrays.stream(mapping.value()).anyMatch(path -> path.startsWith("/api"));
	}

	/**
	 * springdoc 이 기본으로 넣은 200 항목을 201 로 옮깁니다.
	 *
	 * 스키마와 예시를 그대로 물려받아야 하므로 새로 만들지 않고 옮깁니다. 새로
	 * 만들면 응답 본문 스키마를 여기서 다시 조립해야 하는데, 그 타입은
	 * ApiResponse<T> 제네릭이라 애노테이션으로 표현할 수 없습니다.
	 */
	private void promoteToCreated(ApiResponses responses) {
		ApiResponse ok = responses.get("200");
		if (ok == null || responses.containsKey("201")) {
			return;
		}
		responses.remove("200");
		responses.addApiResponse("201", ok.description("생성되었습니다. Location 헤더에 생성된 리소스의 경로가 실립니다."));
	}

	private void addIfAbsent(ApiResponses responses, String status, String description, Object example) {
		if (responses.containsKey(status)) {
			return;
		}

		MediaType mediaType = new MediaType()
				.schema(new Schema<>().$ref(ERROR_SCHEMA_REF))
				.example(example);

		responses.addApiResponse(status, new ApiResponse()
				.description(description)
				.content(new Content().addMediaType(JSON, mediaType)));
	}

	/**
	 * 응답 예시를 Map 으로 만듭니다.
	 *
	 * JSON 문자열을 그대로 넘기지 않습니다. 문자열은 스펙에 따옴표로 감싸인
	 * 문자열 리터럴로 실려서, Swagger UI 가 JSON 이 아니라 이스케이프된 한 줄로
	 * 그립니다. Map 으로 넘겨야 객체로 직렬화됩니다.
	 *
	 * HashMap 이 아니라 LinkedHashMap 인 이유는 두 가지입니다. 응답과 같은 필드
	 * 순서를 유지하고, null 값을 허용해야 하기 때문입니다(data 는 항상 null 입니다).
	 */
	private static Map<String, Object> example(String code, String message, List<Map<String, String>> fieldErrors) {
		Map<String, Object> error = new LinkedHashMap<>();
		error.put("code", code);
		error.put("message", message);
		error.put("fieldErrors", fieldErrors);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("success", false);
		body.put("data", null);
		body.put("error", error);
		return body;
	}

	private static Map<String, String> field(String name, String message) {
		Map<String, String> fieldError = new LinkedHashMap<>();
		fieldError.put("field", name);
		fieldError.put("message", message);
		return fieldError;
	}
}
