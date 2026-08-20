package com.hackathon.backend.conversation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.hackathon.backend.ai.client.OpenAiChatClient;
import com.hackathon.backend.conversation.dto.ConversationCreateRequest;
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
}
