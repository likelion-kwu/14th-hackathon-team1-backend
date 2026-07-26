package com.hackathon.backend.health;

/**
 * /health 의 DB 확인 결과입니다.
 *
 * 문자열로 두면 of(zone, "up") 같은 오타가 컴파일을 통과하면서 전체 status 를
 * UP 으로 만들어버립니다.
 */
public enum DatabaseStatus {

	/** 커넥션을 얻어 유효성 확인까지 성공했습니다. */
	UP,

	/** 커넥션을 얻지 못했거나 유효하지 않습니다. /health 는 503 을 반환합니다. */
	DOWN,

	/** DataSource 빈이 없습니다. 슬라이스 테스트나 DB 를 붙이기 전 단계입니다. */
	NOT_CONFIGURED
}
