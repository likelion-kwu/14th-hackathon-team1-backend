package com.hackathon.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 브라우저에서 오는 요청의 CORS 를 허용합니다.
 *
 * allowedMethods 에 OPTIONS 를 반드시 포함해야 합니다. 빠지면 preflight 가 막혀
 * GET 은 되는데 POST 만 CORS 에러가 나는 상태가 됩니다. 이 증상은 curl 로
 * 재현되지 않아서 원인을 찾는 데 시간이 걸립니다.
 *
 * 매핑을 /api/** 가 아니라 /** 로 두는 이유는 /health 도 브라우저에서 직접
 * 호출해 확인하기 때문입니다.
 *
 * allowCredentials 는 켜지 않습니다. 쿠키나 Authorization 헤더를 쓰지 않기
 * 때문입니다. 인증을 붙이는 시점에 켜면 됩니다. origin 을 명시 목록으로 주고
 * 있으므로 켜도 제약은 생기지 않습니다. allowCredentials 가 거부하는 것은
 * allowedOrigins 에 리터럴 "*" 이 들어 있는 경우뿐입니다.
 *
 * 프론트를 Vercel 에 올리면 프리뷰 도메인이 브랜치마다 바뀝니다. 그때는
 * allowedOrigins 대신 allowedOriginPatterns 로 전환합니다. 전환하면 프로퍼티
 * 이름(allowed-origins)이 실제 내용과 어긋나므로 이름도 함께 바꿔야 하고,
 * 패턴 오타는 조용한 매칭 실패로만 나타나므로 테스트로 확인해야 합니다.
 *
 * preflight 거부 응답(403)은 DefaultCorsProcessor 가 직접 쓰기 때문에 공통 응답
 * 래퍼가 적용되지 않습니다. 브라우저가 그 본문을 읽지 않으므로 문제는 없습니다.
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig implements WebMvcConfigurer {

	private final CorsProperties corsProperties;

	public CorsConfig(CorsProperties corsProperties) {
		this.corsProperties = corsProperties;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		// allowedOrigins 가 아니라 allowedOriginPatterns 를 씁니다.
		//
		// 두 메서드는 정확히 일치하는 origin 에 대해 똑같이 동작하므로 지금 허용
		// 범위는 달라지지 않습니다. 차이는 와일드카드를 쓸 수 있다는 것뿐입니다.
		//
		// 프론트를 Vercel 에 올리면 배포마다 프리뷰 주소가 바뀝니다
		// (https://<프로젝트>-git-<브랜치>-<팀>.vercel.app). allowedOrigins 로는
		// 그 주소를 미리 적을 수 없어 배포할 때마다 설정을 고쳐야 합니다.
		// 패턴이면 https://*.vercel.app 한 줄로 끝납니다.
		//
		// 지금 와일드카드를 넣어두지는 않았습니다. 프론트 배포 주소가 아직
		// 정해지지 않았고, 필요해지면 CORS_ALLOWED_ORIGINS 에 패턴을 추가하면 됩니다.
		registry.addMapping("/**")
				.allowedOriginPatterns(corsProperties.allowedOrigins().toArray(String[]::new))
				.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
				.allowedHeaders("*")
				.maxAge(3600);
	}
}
