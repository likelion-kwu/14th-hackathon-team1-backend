package com.hackathon.backend.item;

import java.time.OffsetDateTime;

/**
 * 항목 응답입니다.
 *
 * 엔티티를 그대로 직렬화하지 않습니다. 엔티티에 필드를 추가하면 의도치 않게
 * API 응답이 바뀌고, 연관관계가 생기면 지연 로딩이 직렬화 시점에 터집니다
 * (open-in-view 를 끈 상태라 그때는 500 이 됩니다).
 *
 * @param id        항목 식별자입니다
 * @param name      항목 이름입니다
 * @param createdAt 생성 시각입니다. 오프셋을 포함합니다
 */
public record ItemResponse(Long id, String name, OffsetDateTime createdAt) {

	static ItemResponse from(Item item) {
		return new ItemResponse(item.getId(), item.getName(), item.getCreatedAt());
	}
}
