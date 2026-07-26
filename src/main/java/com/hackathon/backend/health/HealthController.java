package com.hackathon.backend.health;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 배포 스모크 테스트와 프론트 연결 확인용 엔드포인트.
 * GitHub Actions가 배포 직후 이 경로를 호출해 배포 성공 여부를 판정한다.
 *
 * serverTime은 타임존 설정이 살아있는지 확인하는 용도다.
 * EC2 기본 타임존이 UTC라 설정이 빠지면 이 값이 9시간 어긋난다.
 */
@RestController
public class HealthController {

	@GetMapping("/health")
	public Map<String, Object> health() {
		return Map.of(
				"status", "UP",
				"serverTime", LocalDateTime.now());
	}
}
