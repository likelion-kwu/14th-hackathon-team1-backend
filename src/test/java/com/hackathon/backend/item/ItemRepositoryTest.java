package com.hackathon.backend.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * 엔티티 매핑과 제약이 실제 DB 에서 동작하는지 확인합니다.
 *
 * 저장 후에는 반드시 flush + clear 를 합니다. @DataJpaTest 는 트랜잭션 안에서
 * 돌기 때문에 clear 없이 findById 를 하면 SELECT 가 나가지 않고 1차 캐시의
 * 같은 인스턴스가 돌아옵니다. 그러면 방금 만든 자바 객체를 자기 자신과 비교하는
 * 셈이어서 컬럼 타입이나 인코딩을 전혀 검증하지 못합니다.
 *
 * 주의: 테스트는 H2 로 돌고 운영은 MySQL 입니다. 시간 컬럼은 공통 설정의
 * hibernate.timezone.default_storage=NORMALIZE_UTC 로 양쪽 모두 UTC 정규화
 * 저장이 되도록 맞췄지만, 방언 차이가 남는 부분이 있으므로 스키마 최종 확인은
 * 배포 후 실제 RDS 에 ddl-auto=validate 로 한 번 올려서 해야 합니다.
 */
@DataJpaTest
class ItemRepositoryTest {

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("저장하면 id 와 createdAt 이 채워집니다")
	void savePopulatesGeneratedFields() {
		Item saved = itemRepository.save(new Item("첫 항목"));
		Long id = saved.getId();
		entityManager.flush();
		entityManager.clear();

		Item reloaded = itemRepository.findById(id).orElseThrow();

		assertThat(id).isNotNull();
		assertThat(reloaded.getCreatedAt()).isNotNull();
		assertThat(reloaded.getCreatedAt()).isBeforeOrEqualTo(OffsetDateTime.now());
		// 왕복 후에도 같은 시각을 가리켜야 합니다. 오프셋 자체는 저장 전략에 따라
		// 달라질 수 있으므로 오프셋 값이 아니라 instant 가 같은지를 봅니다.
		assertThat(reloaded.getCreatedAt().toInstant())
				.isEqualTo(saved.getCreatedAt().toInstant());
	}

	@Test
	@DisplayName("이름이 중복되면 저장을 거부합니다")
	void rejectsDuplicateName() {
		itemRepository.saveAndFlush(new Item("중복"));

		// 이 예외가 409 로 변환되는지는 ItemApiIntegrationTest 에서 확인합니다.
		assertThatThrownBy(() -> itemRepository.saveAndFlush(new Item("중복")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@DisplayName("이름이 없으면 저장을 거부합니다")
	void rejectsNullName() {
		// 이 위반은 @NotBlank 로 이미 400 에서 걸리므로 여기까지 오면 검증 누락입니다.
		// 그래서 예외 핸들러는 이 경우를 409 가 아니라 500 으로 처리합니다.
		assertThatThrownBy(() -> itemRepository.saveAndFlush(new Item(null)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@DisplayName("한글이 깨지지 않고 저장됩니다")
	void storesKoreanText() {
		Item saved = itemRepository.save(new Item("한글 항목 테스트"));
		Long id = saved.getId();
		entityManager.flush();
		entityManager.clear();

		// clear 를 하지 않으면 DB 를 거치지 않아 컬럼 charset 문제를 잡지 못합니다.
		assertThat(itemRepository.findById(id))
				.get()
				.extracting(Item::getName)
				.isEqualTo("한글 항목 테스트");
	}

	@Test
	@DisplayName("이름 길이 100자는 저장되고 101자는 거부됩니다")
	void enforcesNameLength() {
		// @Size(max = 100) 과 @Column(length = 100) 이 어긋나면, 400 이어야 할 요청이
		// DB 까지 내려가 500 으로 나갑니다. 두 숫자가 같다는 것을 여기서 고정합니다.
		String maxLength = "가".repeat(100);
		Item saved = itemRepository.saveAndFlush(new Item(maxLength));
		assertThat(saved.getId()).isNotNull();

		assertThatThrownBy(() -> itemRepository.saveAndFlush(new Item("나".repeat(101))))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
