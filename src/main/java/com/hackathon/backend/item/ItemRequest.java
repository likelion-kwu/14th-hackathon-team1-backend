package com.hackathon.backend.item;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 항목 생성 요청입니다.
 *
 * 엔티티를 요청 타입으로 그대로 쓰지 않습니다. 클라이언트가 id 나 createdAt 을
 * 넘겨 서버가 정해야 할 값을 덮는 것을 막기 위해서입니다.
 *
 * @param name 항목 이름입니다. 엔티티 컬럼 길이(100)와 맞춥니다
 */
public record ItemRequest(
		@NotBlank(message = "name 은 필수입니다")
		@Size(max = 100, message = "name 은 100자를 넘을 수 없습니다")
		String name) {
}
