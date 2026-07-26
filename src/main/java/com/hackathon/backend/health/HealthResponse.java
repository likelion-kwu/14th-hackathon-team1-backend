package com.hackathon.backend.health;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * /health 응답입니다.
 *
 * Map.of 대신 record 를 씁니다. 필드 순서와 타입이 고정되고,
 * springdoc 스키마가 실제 응답과 일치합니다.
 *
 * @param status     전체 상태입니다. DB 까지 정상이면 "UP", DB 가 끊겼으면 "DOWN" 입니다
 * @param serverTime 오프셋을 포함한 현재 시각입니다
 * @param timeZone   JVM 기본 타임존 ID 입니다. 진단용입니다
 * @param database   DB 접속 확인 결과입니다
 */
public record HealthResponse(String status, ZonedDateTime serverTime, String timeZone, DatabaseStatus database) {

	static final String UP = "UP";
	static final String DOWN = "DOWN";

	/**
	 * DataSource 가 없는 경우(NOT_CONFIGURED)는 UP 으로 봅니다. DB 를 붙이기 전
	 * 단계나 슬라이스 테스트의 기동 확인을 막지 않기 위해서입니다.
	 *
	 * @param zone     JVM 기본 타임존을 넘깁니다. 테스트에서 고정값을 주입할 수 있도록 인자로 받습니다
	 * @param database DB 확인 결과입니다
	 */
	static HealthResponse of(ZoneId zone, DatabaseStatus database) {
		String status = database == DatabaseStatus.DOWN ? DOWN : UP;
		return new HealthResponse(status, ZonedDateTime.now(zone), zone.getId(), database);
	}
}
