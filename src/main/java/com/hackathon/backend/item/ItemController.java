package com.hackathon.backend.item;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.backend.common.response.ApiResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 샘플 CRUD 입니다. 목적은 기능이 아니라 RDS 연결과 프론트 호출 경로가 실제로
 * 뚫렸다는 증명입니다. 본 기획이 정해지면 이 컨트롤러는 지웁니다.
 *
 * 응답을 ApiResponse 로 직접 감쌉니다. 전역 자동 래핑을 쓰면 /health 와
 * /v3/api-docs 까지 감싸져 배포 파이프라인과 Swagger 가 깨집니다.
 */
@Tag(name = "item", description = "샘플 CRUD 입니다. 본 기획 확정 후 삭제합니다.")
@RestController
@RequestMapping("/api/items")
public class ItemController {

	private final ItemService itemService;

	public ItemController(ItemService itemService) {
		this.itemService = itemService;
	}

	/**
	 * 생성 성공 시 201 과 Location 헤더를 반환합니다.
	 * 200 으로 응답하면 프론트가 생성과 조회를 구분할 수 없습니다.
	 */
	@PostMapping
	public ResponseEntity<ApiResponse<ItemResponse>> create(@Valid @RequestBody ItemRequest request) {
		ItemResponse created = itemService.create(request);
		return ResponseEntity.created(URI.create("/api/items/" + created.id()))
				.body(ApiResponse.success(created));
	}

	@GetMapping
	public ApiResponse<List<ItemResponse>> findAll() {
		return ApiResponse.success(itemService.findAll());
	}

	@GetMapping("/{id}")
	public ApiResponse<ItemResponse> findById(@PathVariable Long id) {
		return ApiResponse.success(itemService.findById(id));
	}

	/**
	 * 삭제는 본문이 없지만 204 대신 200 + 공통 래퍼를 씁니다.
	 * 프론트가 모든 /api 응답을 같은 방식으로 파싱할 수 있게 하기 위해서입니다.
	 */
	@DeleteMapping("/{id}")
	public ApiResponse<Void> delete(@PathVariable Long id) {
		itemService.delete(id);
		return ApiResponse.success(null);
	}
}
