package com.hackathon.backend.config;

import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.hackathon.backend.common.response.ErrorResponse;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Swagger UI 설정입니다. 프론트 담당자에게 URL 하나만 넘기면 되도록 합니다.
 *
 *   문서: /swagger-ui/index.html
 *   스펙: /v3/api-docs
 *
 * springdoc 3.0.x 는 Spring Framework 7 / Boot 4 를 지원합니다. 2.x 는
 * Framework 6 대상이라 동작하지 않으므로 버전을 내리지 마십시오.
 *
 * Nginx 를 앞에 세운 뒤에는 server.forward-headers-strategy: framework 가
 * 필요합니다. 없으면 Swagger UI 의 서버 URL 이 http://127.0.0.1:8080 으로 나가
 * "Try it out" 이 동작하지 않습니다.
 */
@Configuration
public class OpenApiConfig {

	/**
	 * 스펙의 servers 에 실을 배포 주소입니다. 비워두면 실지 않습니다.
	 *
	 * 값이 없을 때 springdoc 은 요청 주소를 보고 서버 URL 을 스스로 채웁니다.
	 * 브라우저로 Swagger UI 를 여는 동안에는 그것으로 충분합니다. 이 값이 필요한
	 * 경우는 스펙 JSON 을 파일로 뽑아 프론트에 넘길 때입니다. 그 JSON 에는
	 * 뽑아낸 환경의 주소(대개 localhost:8080)가 박히기 때문입니다.
	 *
	 * 주의: 값을 채우면 springdoc 의 자동 서버 URL 이 대체됩니다. 로컬에서 채워두면
	 * 로컬 Swagger UI 의 "Try it out" 이 배포 서버로 요청을 보냅니다. 로컬 설정
	 * (config/application-local.yml)에는 넣지 마십시오.
	 */
	private final String publicUrl;

	public OpenApiConfig(@Value("${app.api.public-url:}") String publicUrl) {
		this.publicUrl = publicUrl;
	}

	@Bean
	public OpenAPI hackathonOpenApi() {
		OpenAPI openApi = new OpenAPI().info(new Info()
				.title("Hackathon Backend API")
				.description("""
						응답 형태 안내입니다.

						/api/** 는 공통 래퍼로 감싸집니다.
						성공: {"success": true, "data": ..., "error": null}
						실패: {"success": false, "data": null, "error": {"code": ..., "message": ..., "fieldErrors": [...]}}

						실패 응답의 모양은 ErrorResponse 스키마 하나로 고정입니다. 상태 코드가 같아도
						error.code 로 원인이 갈리므로, 분기는 상태 코드가 아니라 error.code 로 하십시오.
						가능한 code 목록은 ErrorResponse > error > code 의 enum 에 전부 있습니다.

						회원 식별은 memberId 파라미터로 합니다. 해커톤 기간에는 로그인을 붙이지
						않기로 했으므로 이 형태가 그대로 유지됩니다. 가입 응답의 id 를 보관했다가 쓰십시오.

						태그 설명에 "구현 전" 이라고 적힌 것들은 아직 고정 예시를 반환합니다.
						요청과 응답의 형태는 확정된 것이라 지금 붙여도 나중에 고칠 필요가 없지만,
						돌아오는 값은 실제 데이터가 아닙니다.

						/health 는 배포 스모크 테스트가 파싱하는 경로라 래퍼를 씌우지 않습니다.
						""")
				.version("v1"));

		if (StringUtils.hasText(publicUrl)) {
			openApi.addServersItem(new Server().url(publicUrl).description("배포 서버"));
		}
		return openApi;
	}

	/**
	 * ErrorResponse 스키마를 components 에 등록합니다.
	 *
	 * ApiErrorResponseCustomizer 가 실패 응답을 $ref 로 가리키기만 하므로, 그
	 * 대상이 스펙 어딘가에 실제로 정의돼 있어야 합니다. springdoc 은 컨트롤러
	 * 시그니처에 나타나는 타입만 스키마로 만드는데 ErrorResponse 는 문서 전용이라
	 * 어떤 컨트롤러도 반환하지 않습니다. 그래서 여기서 직접 등록합니다.
	 *
	 * 이 빈이 없으면 스펙에 깨진 $ref 가 남고, Swagger UI 는 오류를 내지 않고
	 * 실패 응답을 빈칸으로 그립니다. 눈치채기 어려운 실패입니다.
	 *
	 * referencedSchemas 는 ErrorResponse 뿐 아니라 그것이 참조하는 ApiError,
	 * FieldError 까지 함께 돌려줍니다. 전부 등록해야 참조가 끊기지 않습니다.
	 * 이미 등록된 동명 스키마를 덮어쓰지만, 같은 클래스에서 나온 같은 정의라
	 * 문제되지 않습니다.
	 */
	@Bean
	public GlobalOpenApiCustomizer errorResponseSchemaRegistrar() {
		return openApi -> {
			ResolvedSchema resolved = ModelConverters.getInstance()
					.readAllAsResolvedSchema(new AnnotatedType(ErrorResponse.class));
			if (resolved == null || resolved.referencedSchemas == null) {
				return;
			}

			if (openApi.getComponents() == null) {
				openApi.setComponents(new Components());
			}
			Components components = openApi.getComponents();
			resolved.referencedSchemas.forEach(components::addSchemas);
		};
	}
}
