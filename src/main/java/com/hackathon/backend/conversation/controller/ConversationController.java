package com.hackathon.backend.conversation.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.backend.common.response.ApiNotFound;
import com.hackathon.backend.common.response.ApiResponse;
import com.hackathon.backend.conversation.dto.ConversationCreateRequest;
import com.hackathon.backend.conversation.dto.ConversationMessageResponse;
import com.hackathon.backend.conversation.dto.ConversationResponse;
import com.hackathon.backend.conversation.dto.MessageSendRequest;
import com.hackathon.backend.conversation.dto.MessageSendResponse;
import com.hackathon.backend.conversation.service.ConversationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Tag(name = "conversation", description = "대화 조회·진행 API 입니다.")
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

	private final ConversationService conversationService;

	public ConversationController(ConversationService conversationService) {
		this.conversationService = conversationService;
	}

	@Operation(summary = "대화 목록 조회",
			description = "회원의 대화를 생성 시각 내림차순으로 조회합니다. date를 지정하면 해당 KST 날짜의 대화만 반환하며, "
					+ "생략하면 전체 대화를 반환합니다. 메시지 본문은 포함하지 않습니다.")
	@ApiNotFound("해당 회원이 없습니다.")
	@GetMapping
	public ApiResponse<List<ConversationResponse>> findByMember(
			@RequestParam @Positive Long memberId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return ApiResponse.success(conversationService.findByMember(memberId, date));
	}

	@Operation(summary = "대화 상세 조회", description = "대화 한 건의 상태와 시각을 조회합니다. 메시지는 포함하지 않습니다.")
	@ApiNotFound("해당 대화가 없습니다.")
	@GetMapping("/{conversationId}")
	public ApiResponse<ConversationResponse> findById(@PathVariable @Positive Long conversationId) {
		return ApiResponse.success(conversationService.findById(conversationId));
	}

	@Operation(summary = "대화 메시지 목록 조회", description = "대화의 모든 메시지를 sequenceNo 오름차순으로 조회합니다. 페이징은 제공하지 않습니다.")
	@ApiNotFound("해당 대화가 없습니다.")
	@GetMapping("/{conversationId}/messages")
	public ApiResponse<List<ConversationMessageResponse>> findMessages(@PathVariable @Positive Long conversationId) {
		return ApiResponse.success(conversationService.findMessages(conversationId));
	}

	@Operation(summary = "대화 시작",
			description = "서버가 현재 KST 날짜로 대화를 시작합니다. 같은 회원에게 그날 진행 중인 대화가 있으면 새로 만들지 않고 "
					+ "기존 대화를 반환하므로 응답은 항상 200입니다.")
	@ApiNotFound("해당 회원이 없습니다.")
	@PostMapping
	public ApiResponse<ConversationResponse> start(@Valid @RequestBody ConversationCreateRequest request) {
		return ApiResponse.success(conversationService.start(request));
	}

	@Operation(summary = "메시지 전송",
			description = "진행 중(IN_PROGRESS) 대화에만 메시지를 보낼 수 있습니다. 사용자 메시지를 저장한 뒤 OpenAI 응답도 저장하고 "
					+ "두 메시지를 함께 반환합니다. 진행 중이 아닌 대화는 400(BAD_REQUEST)입니다. "
					+ "Idempotency-Key 헤더를 제공하면 동일 키의 재요청 시 저장된 응답을 그대로 반환합니다.")
	@ApiNotFound("해당 대화가 없습니다.")
	@PostMapping("/{conversationId}/messages")
	public ApiResponse<MessageSendResponse> sendMessage(
			@Parameter(required = true) @PathVariable @Positive Long conversationId,
			@Valid @RequestBody MessageSendRequest request,
			@Parameter(description = "재요청 중복 방지용 UUID. 동일 키로 재전송 시 저장된 응답을 반환합니다.")
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
		return ApiResponse.success(conversationService.sendMessage(conversationId, request, idempotencyKey));
	}

	@Operation(summary = "대화 종료",
			description = "진행 중(IN_PROGRESS) 대화만 완료할 수 있습니다. 이미 완료된 대화를 다시 요청하면 상태를 바꾸지 않고 "
					+ "성공 응답을 반환합니다. 최초 완료 뒤에는 스트릭을 갱신하고, 커밋 후 비동기로 HEALTH_EXTRACTION 분석을 시작합니다. "
					+ "분석 상태는 GET /api/ai-analyses?conversationId={conversationId}&taskType=HEALTH_EXTRACTION으로 조회합니다.")
	@ApiNotFound("해당 대화가 없습니다.")
	@PatchMapping("/{conversationId}/complete")
	public ApiResponse<ConversationResponse> complete(@PathVariable @Positive Long conversationId) {
		return ApiResponse.success(conversationService.complete(conversationId));
	}
}
