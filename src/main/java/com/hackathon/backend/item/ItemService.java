package com.hackathon.backend.item;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hackathon.backend.common.exception.NotFoundException;

/**
 * 항목 조회와 변경을 담당합니다.
 *
 * 컨트롤러에 트랜잭션과 도메인 규칙을 두지 않기 위해 서비스를 둡니다.
 * 조회 메서드에는 readOnly 를 붙입니다. 쓰기 트랜잭션으로 조회하면 Hibernate 가
 * 불필요한 더티 체킹과 플러시를 수행합니다.
 *
 * 없는 항목에 대해 NotFoundException 을 던집니다. ResponseStatusException 을
 * 쓰지 않는 이유는 웹 계층의 관심사가 서비스로 새어 들어오기 때문입니다.
 */
@Service
@Transactional(readOnly = true)
public class ItemService {

	private final ItemRepository itemRepository;

	public ItemService(ItemRepository itemRepository) {
		this.itemRepository = itemRepository;
	}

	@Transactional
	public ItemResponse create(ItemRequest request) {
		Item saved = itemRepository.save(new Item(request.name()));
		return ItemResponse.from(saved);
	}

	/**
	 * 목록은 최신순으로 반환합니다. 정렬을 지정하지 않으면 DB 가 반환하는 순서에
	 * 의존하게 되어, 프론트에서 순서가 들쭉날쭉한 것처럼 보입니다.
	 */
	public List<ItemResponse> findAll() {
		return itemRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
				.map(ItemResponse::from)
				.toList();
	}

	public ItemResponse findById(Long id) {
		return itemRepository.findById(id)
				.map(ItemResponse::from)
				.orElseThrow(() -> new NotFoundException("항목을 찾을 수 없습니다."));
	}

	/**
	 * 없는 항목을 지우려 하면 404 로 응답합니다.
	 * deleteById 는 대상이 없어도 조용히 성공하므로 그대로 쓰면 프론트가 삭제
	 * 성공과 대상 부재를 구분할 수 없습니다.
	 *
	 * existsById 로 먼저 확인하지 않는 이유가 있습니다. deleteById 자신이 내부에서
	 * findById 를 수행하므로 존재 확인을 따로 하면 왕복이 3회가 되고, 확인과 삭제
	 * 사이에 다른 트랜잭션이 지울 수 있는 창이 생깁니다. 조회한 엔티티를 그대로
	 * 넘기면 왕복이 2회로 줄고 그 창도 사라집니다.
	 */
	@Transactional
	public void delete(Long id) {
		Item item = itemRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("항목을 찾을 수 없습니다."));
		itemRepository.delete(item);
	}
}
