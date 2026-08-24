package com.hackathon.backend.conversation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import com.hackathon.backend.ai.client.OpenAiChatClient;
import com.hackathon.backend.conversation.dto.ConversationCreateRequest;
import com.hackathon.backend.conversation.dto.MessageSendRequest;
import com.hackathon.backend.conversation.dto.MessageSendResponse;
import com.hackathon.backend.conversation.entity.Conversation;
import com.hackathon.backend.conversation.entity.ConversationMessage;
import com.hackathon.backend.conversation.repository.ConversationMessageRepository;
import com.hackathon.backend.conversation.repository.ConversationRepository;
import com.hackathon.backend.engagement.service.StreakService;
import com.hackathon.backend.member.entity.Member;
import com.hackathon.backend.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

	@Mock
	private ConversationRepository conversationRepository;
	@Mock
	private ConversationMessageRepository conversationMessageRepository;
	@Mock
	private MemberRepository memberRepository;
	@Mock
	private OpenAiChatClient openAiChatClient;
	@Mock
	private StreakService streakService;
	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private ConversationService conversationService;

	// ── 기존 테스트 ──────────────────────────────────────────────────────────────

	@Test
	void startsNewConversationWithAnImmediateAssistantGreeting() {
		Member member = Member.builder().nickname("민지").phone("010-1234-5678").build();
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(conversationRepository.findByMemberIdAndSessionDateAndStatus(eq(1L), any(),
				any(Conversation.ConversationStatus.class))).willReturn(Optional.empty());
		given(conversationRepository.save(any(Conversation.class))).willAnswer(invocation -> invocation.getArgument(0));

		conversationService.start(new ConversationCreateRequest(1L, Conversation.ConversationType.CHAT));

		ArgumentCaptor<ConversationMessage> messageCaptor = ArgumentCaptor.forClass(ConversationMessage.class);
		verify(conversationMessageRepository).save(messageCaptor.capture());
		ConversationMessage greeting = messageCaptor.getValue();
		assertThat(greeting.getRole()).isEqualTo(ConversationMessage.MessageRole.ASSISTANT);
		assertThat(greeting.getSequenceNo()).isEqualTo(1);
		assertThat(greeting.getContent()).contains("민지님").contains("어떠신가요");
	}

	@Test
	void doesNotAddAnotherGreetingWhenReturningAnExistingConversation() {
		Member member = Member.builder().nickname("민지").phone("010-1234-5678").build();
		Conversation existing = Conversation.builder().member(member).type(Conversation.ConversationType.CHAT).build();
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(conversationRepository.findByMemberIdAndSessionDateAndStatus(eq(1L), any(),
				any(Conversation.ConversationStatus.class))).willReturn(Optional.of(existing));

		conversationService.start(new ConversationCreateRequest(1L, Conversation.ConversationType.CHAT));

		verify(conversationMessageRepository, never()).save(any());
	}

	// ── 메시지 전송 안정화 테스트 ─────────────────────────────────────────────────

	@Test
	void sendMessage_정상_응답() {
		Conversation conversation = buildInProgressConversation();
		given(conversationRepository.findWithLockById(1L)).willReturn(Optional.of(conversation));
		given(conversationMessageRepository.countByConversationId(1L)).willReturn(1L);
		given(conversationMessageRepository.findByConversationIdOrderBySequenceNoAsc(1L)).willReturn(List.of());
		given(openAiChatClient.reply(any(), any())).willReturn("AI 응답입니다.");
		given(conversationMessageRepository.save(any(ConversationMessage.class)))
				.willAnswer(inv -> inv.getArgument(0));

		MessageSendResponse response = conversationService.sendMessage(1L, new MessageSendRequest("안녕하세요"), null);

		assertThat(response.userMessage().content()).isEqualTo("안녕하세요");
		assertThat(response.userMessage().role()).isEqualTo(ConversationMessage.MessageRole.USER);
		assertThat(response.assistantMessage().content()).isEqualTo("AI 응답입니다.");
		assertThat(response.assistantMessage().role()).isEqualTo(ConversationMessage.MessageRole.ASSISTANT);
		verify(openAiChatClient, times(1)).reply(any(), any());
	}

	@Test
	void sendMessage_첫_요청_실패_후_재시도_성공() {
		Conversation conversation = buildInProgressConversation();
		given(conversationRepository.findWithLockById(1L)).willReturn(Optional.of(conversation));
		given(conversationMessageRepository.countByConversationId(1L)).willReturn(1L);
		given(conversationMessageRepository.findByConversationIdOrderBySequenceNoAsc(1L)).willReturn(List.of());
		given(openAiChatClient.reply(any(), any()))
				.willThrow(new ResourceAccessException("connection timeout"))
				.willReturn("재시도 후 AI 응답");
		given(conversationMessageRepository.save(any(ConversationMessage.class)))
				.willAnswer(inv -> inv.getArgument(0));

		MessageSendResponse response = conversationService.sendMessage(1L, new MessageSendRequest("안녕하세요"), null);

		assertThat(response.assistantMessage().content()).isEqualTo("재시도 후 AI 응답");
		verify(openAiChatClient, times(2)).reply(any(), any());
	}

	@Test
	void sendMessage_최종_실패_시_503_반환() {
		Conversation conversation = buildInProgressConversation();
		given(conversationRepository.findWithLockById(1L)).willReturn(Optional.of(conversation));
		given(conversationMessageRepository.countByConversationId(1L)).willReturn(1L);
		given(conversationMessageRepository.findByConversationIdOrderBySequenceNoAsc(1L)).willReturn(List.of());
		given(openAiChatClient.reply(any(), any()))
				.willThrow(new ResourceAccessException("connection timeout"));

		assertThatThrownBy(() -> conversationService.sendMessage(1L, new MessageSendRequest("안녕하세요"), null))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(503));

		// MAX_AI_RETRY(2) + 최초 1회 = 총 3회 시도
		verify(openAiChatClient, times(3)).reply(any(), any());
	}

	@Test
	void sendMessage_완료된_대화에는_전송_불가() {
		Conversation conversation = buildInProgressConversation();
		conversation.complete();
		given(conversationRepository.findWithLockById(1L)).willReturn(Optional.of(conversation));

		assertThatThrownBy(() -> conversationService.sendMessage(1L, new MessageSendRequest("안녕하세요"), null))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(400));

		verify(openAiChatClient, never()).reply(any(), any());
	}

	@Test
	void sendMessage_동일_idempotencyKey_재요청_시_캐시된_응답_반환() {
		Conversation conversation = buildInProgressConversation();

		ConversationMessage cachedUserMsg = ConversationMessage.builder()
				.conversation(conversation)
				.role(ConversationMessage.MessageRole.USER)
				.content("안녕하세요")
				.sequenceNo(2)
				.tokenCount(3)
				.clientMessageId("test-uuid-1")
				.build();
		ConversationMessage cachedAssistantMsg = ConversationMessage.builder()
				.conversation(conversation)
				.role(ConversationMessage.MessageRole.ASSISTANT)
				.content("AI 캐시 응답입니다.")
				.sequenceNo(3)
				.tokenCount(10)
				.build();

		given(conversationMessageRepository.findByClientMessageId("test-uuid-1"))
				.willReturn(Optional.of(cachedUserMsg));
		given(conversationMessageRepository.findByConversationIdAndSequenceNo(1L, 3))
				.willReturn(Optional.of(cachedAssistantMsg));

		MessageSendResponse response = conversationService.sendMessage(1L, new MessageSendRequest("안녕하세요"), "test-uuid-1");

		assertThat(response.userMessage().content()).isEqualTo("안녕하세요");
		assertThat(response.assistantMessage().content()).isEqualTo("AI 캐시 응답입니다.");
		// OpenAI를 다시 호출하지 않아야 함
		verify(openAiChatClient, never()).reply(any(), any());
		// 새 메시지를 저장하지 않아야 함
		verify(conversationMessageRepository, never()).save(any());
	}

	// ── 헬퍼 ─────────────────────────────────────────────────────────────────────

	private Conversation buildInProgressConversation() {
		Conversation conversation = Conversation.builder()
				.type(Conversation.ConversationType.CHAT)
				.sessionDate(LocalDate.now())
				.build();
		conversation.start();
		return conversation;
	}
}
