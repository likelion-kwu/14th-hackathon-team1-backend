package com.hackathon.backend.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

/**
 * CORS 허용 origin 설정입니다.
 *
 * 코드에 origin 을 박아두면 프론트 배포 주소가 정해질 때마다 재빌드해야 하므로
 * 설정으로 뺍니다. 공통 application.yml 에 로컬 기본값이 있고, 운영에서는
 * application-prod.yml 이 CORS_ALLOWED_ORIGINS 환경변수로 덮습니다.
 *
 * @Validated + @NotEmpty 를 걸어 빈 목록을 기동 실패로 만듭니다. 검증이 없으면
 * /etc/hackathon.env 에 CORS_ALLOWED_ORIGINS= 만 있어도 기동에 성공하고
 * /health 도 200 을 반환해서, 스모크 테스트를 전부 통과한 뒤 프론트의 모든
 * 요청만 CORS 로 막히는 조용한 실패가 됩니다. systemd 의 환경변수 검증은
 * 값의 부재만 잡고 빈 값은 잡지 못합니다.
 *
 * @DefaultValue 는 설정 자체가 없을 때 null 대신 빈 목록이 되도록 합니다.
 * null 이면 이 클래스를 쓰는 쪽에서 NPE 가 납니다.
 *
 * @param allowedOrigins 쉼표로 구분된 origin 목록입니다. Spring 이 List 로 변환하며
 *                       항목의 앞뒤 공백은 제거됩니다
 */
@Validated
@ConfigurationProperties("app.cors")
public record CorsProperties(@NotEmpty(message = "app.cors.allowed-origins 를 설정해야 합니다") @DefaultValue List<String> allowedOrigins) {
}
