package com.hackathon.backend.item;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * RDS 연결과 프론트 호출 경로가 실제로 뚫렸다는 것을 증명하기 위한 샘플 엔티티입니다.
 * 본 기획이 정해지면 이 엔티티는 지웁니다.
 *
 * createdAt 을 LocalDateTime 이 아니라 OffsetDateTime 으로 둡니다.
 * LocalDateTime 은 타임존 정보가 없어서 JVM 기본 타임존이 UTC 인 서버에서는
 * 그 값이 무엇을 가리키는지 알 수 없고, 응답에서도 Jackson 이 보정할 근거가
 * 없어 9시간 어긋난 값이 프론트로 나갑니다.
 *
 * 단, MySQL 은 타임존을 담는 컬럼 타입이 없어서(datetime(6)) 오프셋이 컬럼에
 * 보존되지는 않습니다. Hibernate 가 UTC 로 정규화해 저장하고 읽을 때 되돌립니다
 * (공통 설정의 hibernate.timezone.default_storage=NORMALIZE_UTC). 즉 DB 를 직접
 * 조회하면 KST 가 아니라 UTC 값이 보입니다. API 응답은 Jackson 이 KST 로
 * 렌더하므로 정상입니다. DB_URL 의 connectionTimeZone 은 이 컬럼에 영향을
 * 주지 않습니다.
 *
 * name 에 유니크 제약을 둡니다. 중복 요청이 조용히 두 건으로 쌓이지 않게 하고,
 * 제약 위반이 409 로 변환되는 경로를 실제로 쓰기 위해서입니다.
 *
 * 제약 이름을 직접 지정합니다. 지정하지 않으면 Hibernate 가 UKmnhl79u3... 같은
 * 해시 이름을 만들고, 나중에 ddl-auto 를 validate 로 바꾸고 마이그레이션 SQL 을
 * 손으로 쓸 때 그 이름을 맞출 수 없습니다.
 */
@Entity
@Table(name = "items",
		uniqueConstraints = @UniqueConstraint(name = "uk_items_name", columnNames = "name"))
public class Item {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String name;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	/**
	 * JPA 가 요구하는 기본 생성자입니다. 애플리케이션 코드에서는 쓰지 않습니다.
	 */
	protected Item() {
	}

	public Item(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
