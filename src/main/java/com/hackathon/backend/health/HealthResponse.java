package com.hackathon.backend.health;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * /health 응답.
 *
 * Map.of 대신 record 를 쓴다. 필드 순서와 타입이 고정되고,
 * 나중에 springdoc 을 붙일 때 스키마가 실제 응답과 일치한다.
 *
 * @param status     항상 "UP". 이 응답이 나갔다는 것 자체가 기동 성공을 뜻한다
 * @param serverTime 오프셋을 포함한 현재 시각
 * @param timeZone   JVM 기본 타임존 ID. 진단용이다
 */
public record HealthResponse(String status, ZonedDateTime serverTime, String timeZone) {

	/**
	 * @param zone JVM 기본 타임존을 넘긴다. 테스트에서 고정값을 주입할 수 있도록 인자로 받는다
	 */
	public static HealthResponse up(ZoneId zone) {
		return new HealthResponse("UP", ZonedDateTime.now(zone), zone.getId());
	}
}
