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
import lombok.RequiredArgsConstructor;

@Tag(name = "conversation", description = "대화 조회·진행 API 입니다.")
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @Operation(summary = "대화 목록 조회",
            description = "date 를 주면 그 날짜의 대화를, 생략하면 전체 대화를 최신순으로 조회합니다. 메시지는 포함되지 않습니다.")
    @ApiNotFound("해당 회원이 없습니다.")
    @GetMapping
    public ApiResponse<List<ConversationResponse>> findByMember(
            @Parameter(description = "회원 식별자입니다", required = true)
            @RequestParam @Positive Long memberId,

            @Parameter(description = "조회할 날짜입니다 (KST). 생략하면 전체를 최신순으로 반환합니다.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(conversationService.findByMember(memberId, date));
    }

    @Operation(summary = "대화 상세 조회", description = "대화 한 건의 상태와 시각을 조회합니다. 메시지는 포함되지 않습니다.")
    @ApiNotFound("해당 대화가 없습니다.")
    @GetMapping("/{conversationId}")
    public ApiResponse<ConversationResponse> findById(
            @Parameter(description = "대화 식별자입니다", required = true)
            @PathVariable @Positive Long conversationId) {
        return ApiResponse.success(conversationService.findById(conversationId));
    }

    @Operation(summary = "대화 메시지 목록 조회",
            description = "대화의 모든 메시지를 sequenceNo 오름차순으로 조회합니다. 페이징은 없습니다.")
    @ApiNotFound("해당 대화가 없습니다.")
    @GetMapping("/{conversationId}/messages")
    public ApiResponse<List<ConversationMessageResponse>> findMessages(
            @Parameter(description = "대화 식별자입니다", required = true)
            @PathVariable @Positive Long conversationId) {
        return ApiResponse.success(conversationService.findMessages(conversationId));
    }

    /**
     * 201 이 아니라 200 입니다. 오늘 진행 중인 대화가 있으면 새로 만들지 않고
     * 그것을 돌려주므로, 항상 "생성됨" 이라고 답하면 사실과 다릅니다.
     */
    @Operation(summary = "대화 시작",
            description = "대화를 시작합니다. 오늘(KST) 이미 진행 중인 대화가 있으면 새로 만들지 않고 그 대화를 반환합니다. "
                    + "그래서 201 이 아니라 200 입니다.")
    @ApiNotFound("해당 회원이 없습니다.")
    @PostMapping
    public ApiResponse<ConversationResponse> start(@Valid @RequestBody ConversationCreateRequest request) {
        return ApiResponse.success(conversationService.start(request));
    }

    @Operation(summary = "메시지 전송",
            description = "사용자 발화를 저장하고 AI 응답을 생성해 함께 저장합니다. 저장된 두 메시지를 모두 반환합니다. "
                    + "진행 중이 아닌 대화에 보내면 400(BAD_REQUEST) 입니다.")
    @ApiNotFound("해당 대화가 없습니다.")
    @PostMapping("/{conversationId}/messages")
    public ApiResponse<MessageSendResponse> sendMessage(
            @Parameter(description = "대화 식별자입니다", required = true)
            @PathVariable @Positive Long conversationId,
            @Valid @RequestBody MessageSendRequest request) {
        return ApiResponse.success(conversationService.sendMessage(conversationId, request));
    }

    @Operation(summary = "대화 종료",
            description = "대화를 정상 종료합니다. 이미 종료된 대화에 다시 보내도 성공합니다. 화면을 벗어날 때 재시도로 "
                    + "두 번 오는 것이 정상적인 흐름이기 때문입니다. 시작한 적 없는 대화를 종료하면 400 입니다.")
    @ApiNotFound("해당 대화가 없습니다.")
    @PatchMapping("/{conversationId}/complete")
    public ApiResponse<ConversationResponse> complete(
            @Parameter(description = "대화 식별자입니다", required = true)
            @PathVariable @Positive Long conversationId) {
        return ApiResponse.success(conversationService.complete(conversationId));
    }
}
