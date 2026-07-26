package com.hackathon.backend.health;

import java.time.ZoneId;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 배포 스모크 테스트와 프론트 연결 확인용 엔드포인트.
 * GitHub Actions 가 배포 직후 이 경로를 호출해 배포 성공 여부를 판정합니다.
 *
 * 공통 응답 래퍼(ApiResponse)는 씌우지 않습니다. 배포 파이프라인과 모니터링이
 * 파싱하는 경로라 응답 형태를 고정해 두어야 하고, 래퍼를 나중에 도입할 때
 * 스모크 테스트가 조용히 깨지는 것을 막기 위해서입니다.
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
 */
@RestController
public class HealthController {

	@GetMapping("/health")
	public HealthResponse health() {
		return HealthResponse.up(ZoneId.systemDefault());
	}
}
