package com.hackathon.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

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

	@Bean
	public OpenAPI hackathonOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Hackathon Backend API")
				.description("""
						응답 형태 안내입니다.

						/api/** 는 공통 래퍼로 감싸집니다.
						성공: {"success": true, "data": ..., "error": null}
						실패: {"success": false, "data": null, "error": {"code": ..., "message": ..., "fieldErrors": [...]}}

						/health 는 배포 스모크 테스트가 파싱하는 경로라 래퍼를 씌우지 않습니다.
						""")
				.version("v1"));
	}
}
