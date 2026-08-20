package com.hackathon.backend.conversation.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

	@Operation(summary = "대화 목록 조회")
	@ApiNotFound("해당 회원이 없습니다.")
	@GetMapping
	public ApiResponse<List<ConversationResponse>> findByMember(
			@RequestParam @Positive Long memberId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return ApiResponse.success(conversationService.findByMember(memberId, date));
	}

	@Operation(summary = "대화 상세 조회")
	@ApiNotFound("해당 대화가 없습니다.")
	@GetMapping("/{conversationId}")
	public ApiResponse<ConversationResponse> findById(@PathVariable @Positive Long conversationId) {
		return ApiResponse.success(conversationService.findById(conversationId));
	}

	@Operation(summary = "대화 메시지 목록 조회")
	@ApiNotFound("해당 대화가 없습니다.")
	@GetMapping("/{conversationId}/messages")
	public ApiResponse<List<ConversationMessageResponse>> findMessages(@PathVariable @Positive Long conversationId) {
		return ApiResponse.success(conversationService.findMessages(conversationId));
	}

	@Operation(summary = "대화 시작")
	@ApiNotFound("해당 회원이 없습니다.")
	@PostMapping
	public ApiResponse<ConversationResponse> start(@Valid @RequestBody ConversationCreateRequest request) {
		return ApiResponse.success(conversationService.start(request));
	}

	@Operation(summary = "메시지 전송")
	@ApiNotFound("해당 대화가 없습니다.")
	@PostMapping("/{conversationId}/messages")
	public ApiResponse<MessageSendResponse> sendMessage(
			@Parameter(required = true) @PathVariable @Positive Long conversationId,
			@Valid @RequestBody MessageSendRequest request) {
		return ApiResponse.success(conversationService.sendMessage(conversationId, request));
	}

	@Operation(summary = "대화 종료")
	@ApiNotFound("해당 대화가 없습니다.")
	@PatchMapping("/{conversationId}/complete")
	public ApiResponse<ConversationResponse> complete(@PathVariable @Positive Long conversationId) {
		return ApiResponse.success(conversationService.complete(conversationId));
	}
}
