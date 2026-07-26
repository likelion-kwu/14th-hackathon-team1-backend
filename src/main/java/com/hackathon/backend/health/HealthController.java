package com.hackathon.backend.health;

import java.sql.Connection;
import java.time.ZoneId;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 배포 스모크 테스트와 프론트 연결 확인용 엔드포인트입니다.
 * GitHub Actions 가 배포 직후 이 경로를 호출해 배포 성공 여부를 판정합니다.
 *
 * 공통 응답 래퍼(ApiResponse)는 씌우지 않습니다. 배포 파이프라인과 모니터링이
 * 파싱하는 경로라 응답 형태를 고정해 두어야 하고, 래퍼를 도입할 때 스모크
 * 테스트가 조용히 깨지는 것을 막기 위해서입니다.
 *
 * serverTime 은 오프셋을 포함하므로 프론트가 타임존을 추측할 필요가 없습니다.
 * 단 spring.jackson.time-zone 설정이 직렬화 시점에 Asia/Seoul 로 변환하므로
 * 이 값만으로는 서버 OS 타임존을 알 수 없습니다(UTC 서버도 +09:00 으로 나옵니다).
 *
 * timeZone 은 그 사각지대를 메우는 진단 정보입니다. 배포 문서가 EC2 타임존을
 * Asia/Seoul 로 설정하기로 정해 두었으므로, 이 값이 다르면 서버가 문서와
 * 어긋난 상태라는 신호입니다. 다만 응답 시각은 Jackson 설정이 보정하므로
 * 값이 UTC 라도 API 응답 자체는 정상입니다. 스모크 테스트를 이 값으로
 * 게이트하지 않습니다 — 정상 동작하는 서버를 떨어뜨리게 됩니다.
 *
 * database 는 실제로 커넥션을 얻어 확인합니다. DB 가 끊겼는데 200 을 반환하면
 * 배포는 성공으로 판정되고 앱은 아무 요청도 처리하지 못하는 상태가 됩니다.
 * 그래서 DB 확인이 실패하면 503 을 반환해 스모크 테스트가 실패하게 합니다.
 *
 * DataSource 를 ObjectProvider 로 받는 이유는 이 컨트롤러만 띄우는 슬라이스
 * 테스트에는 DataSource 가 없기 때문입니다. 없으면 NOT_CONFIGURED 를 반환하고
 * 200 을 유지합니다.
 */
@Tag(name = "health", description = "배포 확인용 엔드포인트입니다. 공통 응답 래퍼를 쓰지 않습니다.")
@RestController
public class HealthController {

	private static final Logger log = LoggerFactory.getLogger(HealthController.class);

	/**
	 * 커넥션 유효성 확인에 허용하는 시간입니다. 단위는 초입니다.
	 *
	 * 이 값은 isValid 검사에만 적용됩니다. 그 앞의 getConnection 은 Hikari 의
	 * connection-timeout 에 걸리므로, 두 구간을 함께 짧게 유지해야 합니다.
	 * 운영 설정(application-prod.yml)에서 connection-timeout 을 2초로 두었습니다.
	 * 그 설정이 없으면 기본값 30초가 적용되어, RDS 가 응답하지 않을 때 이 요청이
	 * 30초 매달립니다.
	 */
	private static final int DB_CHECK_TIMEOUT_SECONDS = 2;

	private final ObjectProvider<DataSource> dataSourceProvider;

	public HealthController(ObjectProvider<DataSource> dataSourceProvider) {
		this.dataSourceProvider = dataSourceProvider;
	}

	@GetMapping("/health")
	public ResponseEntity<HealthResponse> health() {
		HealthResponse response = HealthResponse.of(ZoneId.systemDefault(), checkDatabase());

		HttpStatus status = response.database() == DatabaseStatus.DOWN
				? HttpStatus.SERVICE_UNAVAILABLE
				: HttpStatus.OK;
		return ResponseEntity.status(status).body(response);
	}

	private DatabaseStatus checkDatabase() {
		DataSource dataSource = dataSourceProvider.getIfAvailable();
		if (dataSource == null) {
			return DatabaseStatus.NOT_CONFIGURED;
		}

		try (Connection connection = dataSource.getConnection()) {
			return connection.isValid(DB_CHECK_TIMEOUT_SECONDS)
					? DatabaseStatus.UP
					: DatabaseStatus.DOWN;
		}
		catch (Exception e) {
			// 예외를 응답에 담지 않습니다. 접속 문자열과 자격증명이 메시지에 섞입니다.
			log.error("DB 접속 확인에 실패했습니다.", e);
			return DatabaseStatus.DOWN;
		}
	}
}
