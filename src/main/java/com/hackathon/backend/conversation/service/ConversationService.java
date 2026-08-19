package com.hackathon.backend.conversation.service;

import com.hackathon.backend.common.exception.NotFoundException;
import com.hackathon.backend.conversation.dto.ConversationCreateRequest;
import com.hackathon.backend.conversation.dto.ConversationMessageResponse;
import com.hackathon.backend.conversation.dto.ConversationResponse;
import com.hackathon.backend.conversation.dto.MessageSendRequest;
import com.hackathon.backend.conversation.dto.MessageSendResponse;
import com.hackathon.backend.conversation.entity.Conversation;
import com.hackathon.backend.conversation.entity.ConversationMessage;
import com.hackathon.backend.conversation.entity.ConversationMessage.MessageRole;
import com.hackathon.backend.conversation.repository.ConversationMessageRepository;
import com.hackathon.backend.conversation.repository.ConversationRepository;
import com.hackathon.backend.member.entity.Member;
import com.hackathon.backend.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * 대화 도메인 비즈니스 로직입니다.
 *
 * AI 연동 전에는 sendMessage 의 응답을 목 문자열로 처리합니다.
 * 실제 AI 서비스가 붙으면 estimateTokenCount 와 목 응답 부분만 교체하면 됩니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final MemberRepository memberRepository;

    /**
     * 회원의 대화 목록을 조회합니다.
     * date 를 주면 그 날짜만, 생략하면 전체를 최신순으로 반환합니다.
     */
    public List<ConversationResponse> findByMember(Long memberId, LocalDate date) {
        if (!memberRepository.existsById(memberId)) {
            throw new NotFoundException("해당 회원이 없습니다.");
        }
        List<Conversation> list;
        if (date != null) {
            list = conversationRepository.findByMemberIdAndSessionDate(memberId, date);
            list = list.stream()
                    .sorted(Comparator.comparing(Conversation::getCreatedAt).reversed())
                    .toList();
        } else {
            list = conversationRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
        }
        return list.stream().map(this::toResponse).toList();
    }

    public ConversationResponse findById(Long conversationId) {
        return toResponse(conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("해당 대화가 없습니다.")));
    }

    public List<ConversationMessageResponse> findMessages(Long conversationId) {
        if (!conversationRepository.existsById(conversationId)) {
            throw new NotFoundException("해당 대화가 없습니다.");
        }
        return conversationMessageRepository
                .findByConversationIdOrderBySequenceNoAsc(conversationId)
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    /**
     * 대화를 시작합니다.
     * 오늘 이미 IN_PROGRESS 인 대화가 있으면 새로 만들지 않고 그것을 반환합니다.
     * sessionDate 는 서버가 오늘(KST)로 정합니다.
     */
    @Transactional
    public ConversationResponse start(ConversationCreateRequest request) {
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new NotFoundException("해당 회원이 없습니다."));

        LocalDate today = LocalDate.now();

        return conversationRepository
                .findByMemberIdAndSessionDateAndStatus(
                        request.memberId(), today, Conversation.ConversationStatus.IN_PROGRESS)
                .map(this::toResponse)
                .orElseGet(() -> {
                    Conversation c = Conversation.builder()
                            .member(member)
                            .type(request.type())
                            .sessionDate(today)
                            .build();
                    c.start();
                    return toResponse(conversationRepository.save(c));
                });
    }

    /**
     * 사용자 발화를 저장하고 AI 응답을 함께 반환합니다.
     * AI 연동 전에는 목 응답을 씁니다.
     */
    @Transactional
    public MessageSendResponse sendMessage(Long conversationId, MessageSendRequest request) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("해당 대화가 없습니다."));

        if (conversation.getStatus() != Conversation.ConversationStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "진행 중인 대화가 아닙니다.");
        }

        long count = conversationMessageRepository.countByConversationId(conversationId);
        int nextSeq = (int) count + 1;

        ConversationMessage userMsg = conversationMessageRepository.save(
                ConversationMessage.builder()
                        .conversation(conversation)
                        .role(MessageRole.USER)
                        .content(request.content())
                        .sequenceNo(nextSeq)
                        .tokenCount(estimateTokenCount(request.content()))
                        .build());

        // TODO: AI 서비스 연동 시 이 부분을 교체합니다.
        String aiContent = "이야기해줘서 고마워요. 조금 더 자세히 말해줄 수 있을까요?";
        ConversationMessage aiMsg = conversationMessageRepository.save(
                ConversationMessage.builder()
                        .conversation(conversation)
                        .role(MessageRole.ASSISTANT)
                        .content(aiContent)
                        .sequenceNo(nextSeq + 1)
                        .tokenCount(estimateTokenCount(aiContent))
                        .build());

        return new MessageSendResponse(toMessageResponse(userMsg), toMessageResponse(aiMsg));
    }

    /**
     * 대화를 완료 상태로 전환합니다.
     * 이미 COMPLETED 라면 성공으로 처리합니다(idempotent).
     * SCHEDULED / MISSED 등 IN_PROGRESS 가 아닌 상태에서는 400 입니다.
     */
    @Transactional
    public ConversationResponse complete(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("해당 대화가 없습니다."));

        if (conversation.getStatus() == Conversation.ConversationStatus.COMPLETED) {
            // 화면 이탈 시 재시도가 오는 것이 정상 흐름이므로 그냥 성공 반환합니다.
            return toResponse(conversation);
        }
        if (conversation.getStatus() != Conversation.ConversationStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "시작되지 않은 대화는 완료할 수 없습니다.");
        }

        conversation.complete();
        return toResponse(conversation);
    }

    private ConversationResponse toResponse(Conversation c) {
        return new ConversationResponse(
                c.getId(),
                c.getMember().getId(),
                c.getType(),
                c.getStatus(),
                c.getSessionDate(),
                c.getStartedAt(),
                c.getEndedAt(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }

    private ConversationMessageResponse toMessageResponse(ConversationMessage m) {
        return new ConversationMessageResponse(
                m.getId(),
                m.getConversation().getId(),
                m.getRole(),
                m.getContent(),
                m.getSequenceNo(),
                m.getTokenCount(),
                m.getCreatedAt()
        );
    }

    /**
     * 한국어 기준 간이 토큰 추정입니다. 글자 수를 3.5 로 나눕니다.
     * AI 연동 후에는 실제 토큰 수로 교체합니다.
     */
    private int estimateTokenCount(String content) {
        return (int) Math.ceil(content.length() / 3.5);
    }
}
