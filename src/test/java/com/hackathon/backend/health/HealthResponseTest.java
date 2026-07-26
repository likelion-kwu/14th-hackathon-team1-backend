package com.hackathon.backend.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * timeZone 필드가 실제로 주어진 타임존을 반영하는지 확인합니다.
 * MockMvc 테스트에서 ZoneId.systemDefault() 로 비교하면 프로덕션 코드와 같은 식을
 * 되풀이하는 자기충족적 단정이 되므로, 고정값을 주입해 여기서 검증합니다.
 */
class HealthResponseTest {

	@Test
	@DisplayName("주어진 타임존 ID 를 그대로 노출한다")
	void exposesGivenZone() {
		HealthResponse response = HealthResponse.of(ZoneId.of("UTC"), DatabaseStatus.UP);

		assertThat(response.status()).isEqualTo("UP");
		assertThat(response.timeZone()).isEqualTo("UTC");
		assertThat(response.serverTime().getZone()).isEqualTo(ZoneId.of("UTC"));
	}

	@Test
	@DisplayName("DB 가 DOWN 이면 전체 status 도 DOWN 이다")
	void statusFollowsDatabase() {
		// 배포 스모크 테스트가 실패해야 "배포는 성공인데 앱은 못 쓰는" 상태를 잡습니다.
		assertThat(HealthResponse.of(ZoneId.of("Asia/Seoul"), DatabaseStatus.DOWN).status()).isEqualTo("DOWN");
		assertThat(HealthResponse.of(ZoneId.of("Asia/Seoul"), DatabaseStatus.UP).status()).isEqualTo("UP");
	}

	@Test
	@DisplayName("DataSource 가 없으면 status 는 UP 을 유지한다")
	void notConfiguredKeepsUp() {
		// 슬라이스 테스트나 DB 를 붙이기 전 단계의 기동 확인을 막지 않아야 합니다.
		assertThat(HealthResponse.of(ZoneId.of("Asia/Seoul"), DatabaseStatus.NOT_CONFIGURED).status()).isEqualTo("UP");
	}

	@Test
	@DisplayName("타임존이 다르면 serverTime 의 오프셋도 함께 바뀐다")
	void serverTimeFollowsZone() {
		HealthResponse seoul = HealthResponse.of(ZoneId.of("Asia/Seoul"), DatabaseStatus.UP);
		HealthResponse utc = HealthResponse.of(ZoneId.of("UTC"), DatabaseStatus.UP);

		assertThat(seoul.serverTime().getOffset().getTotalSeconds()).isEqualTo(9 * 3600);
		assertThat(utc.serverTime().getOffset().getTotalSeconds()).isZero();
	}
}
