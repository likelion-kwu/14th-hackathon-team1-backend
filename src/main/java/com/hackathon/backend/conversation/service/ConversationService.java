package com.hackathon.backend.conversation.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.hackathon.backend.ai.client.OpenAiChatClient;
import com.hackathon.backend.common.exception.NotFoundException;
import com.hackathon.backend.conversation.dto.ConversationCreateRequest;
import com.hackathon.backend.conversation.dto.ConversationMessageResponse;
import com.hackathon.backend.conversation.dto.ConversationResponse;
import com.hackathon.backend.conversation.dto.MessageSendRequest;
import com.hackathon.backend.conversation.dto.MessageSendResponse;
import com.hackathon.backend.conversation.entity.Conversation;
import com.hackathon.backend.conversation.entity.ConversationMessage;
import com.hackathon.backend.conversation.entity.ConversationMessage.MessageRole;
import com.hackathon.backend.conversation.event.ConversationCompletedEvent;
import com.hackathon.backend.conversation.repository.ConversationMessageRepository;
import com.hackathon.backend.conversation.repository.ConversationRepository;
import com.hackathon.backend.engagement.service.StreakService;
import com.hackathon.backend.member.entity.Member;
import com.hackathon.backend.member.repository.MemberRepository;

@Service
@Transactional(readOnly = true)
public class ConversationService {

	private static final String CHAT_SYSTEM_PROMPT = """
			You are \"Gobi\", a warm Korean health conversation companion for a short daily check-in.
			Always respond in natural Korean, as if speaking to one person on a phone call.

			For every reply:
			- First acknowledge or answer what the user actually said. Be specific when they mention a time, amount, feeling, or habit.
			- Then ask exactly one short, easy follow-up question that naturally helps understand their sleep, mood, meals, activity, water intake, medication, or symptom.
			- Keep the reply to two or three short sentences. Do not use a checklist, headings, markdown, or several questions at once.
			- Use the conversation context and do not ask for information the user has already shared.
			- Do not diagnose, prescribe, or claim medical certainty. For severe or urgent symptoms, clearly recommend contacting emergency services or a medical professional.
			""";
	private static final String OPENING_MESSAGE_TEMPLATE = "%s님, 안녕하세요. 오늘 몸과 마음은 어떠신가요? 가장 먼저 떠오르는 것부터 편하게 말씀해 주세요.";
	private static final int CONTEXT_MESSAGE_LIMIT = 20;
	private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

	private final ConversationRepository conversationRepository;
	private final ConversationMessageRepository conversationMessageRepository;
	private final MemberRepository memberRepository;
	private final OpenAiChatClient openAiChatClient;
	private final StreakService streakService;
	private final ApplicationEventPublisher eventPublisher;

	public ConversationService(ConversationRepository conversationRepository,
			ConversationMessageRepository conversationMessageRepository, MemberRepository memberRepository,
			OpenAiChatClient openAiChatClient, StreakService streakService, ApplicationEventPublisher eventPublisher) {
		this.conversationRepository = conversationRepository;
		this.conversationMessageRepository = conversationMessageRepository;
		this.memberRepository = memberRepository;
		this.openAiChatClient = openAiChatClient;
		this.streakService = streakService;
		this.eventPublisher = eventPublisher;
	}

	public List<ConversationResponse> findByMember(Long memberId, LocalDate date) {
		if (!memberRepository.existsById(memberId)) {
			throw new NotFoundException("해당 회원이 없습니다.");
		}
		List<Conversation> conversations = date == null
				? conversationRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
				: conversationRepository.findByMemberIdAndSessionDate(memberId, date).stream()
						.sorted(Comparator.comparing(Conversation::getCreatedAt).reversed()).toList();
		return conversations.stream().map(this::toResponse).toList();
	}

	public ConversationResponse findById(Long conversationId) {
		return toResponse(findConversation(conversationId));
	}

	public List<ConversationMessageResponse> findMessages(Long conversationId) {
		findConversation(conversationId);
		return conversationMessageRepository.findByConversationIdOrderBySequenceNoAsc(conversationId).stream()
				.map(this::toMessageResponse).toList();
	}

	@Transactional
	public ConversationResponse start(ConversationCreateRequest request) {
		Member member = memberRepository.findById(request.memberId())
				.orElseThrow(() -> new NotFoundException("해당 회원이 없습니다."));
		LocalDate today = LocalDate.now(KOREA_ZONE);
		return conversationRepository.findByMemberIdAndSessionDateAndStatus(request.memberId(), today,
				Conversation.ConversationStatus.IN_PROGRESS).map(this::toResponse).orElseGet(() -> {
			Conversation conversation = Conversation.builder().member(member).type(request.type()).sessionDate(today).build();
			conversation.start();
			Conversation savedConversation = conversationRepository.save(conversation);
			String openingMessage = OPENING_MESSAGE_TEMPLATE.formatted(member.getNickname());
			conversationMessageRepository.save(ConversationMessage.builder()
					.conversation(savedConversation).role(MessageRole.ASSISTANT)
					.content(openingMessage).sequenceNo(1).tokenCount(estimateTokenCount(openingMessage)).build());
			return toResponse(savedConversation);
		});
	}

	@Transactional
	public MessageSendResponse sendMessage(Long conversationId, MessageSendRequest request) {
		Conversation conversation = findConversation(conversationId);
		if (conversation.getStatus() != Conversation.ConversationStatus.IN_PROGRESS) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "진행 중인 대화가 아닙니다.");
		}

		int nextSequence = Math.toIntExact(conversationMessageRepository.countByConversationId(conversationId)) + 1;
		ConversationMessage userMessage = conversationMessageRepository.save(ConversationMessage.builder()
				.conversation(conversation).role(MessageRole.USER).content(request.content()).sequenceNo(nextSequence)
				.tokenCount(estimateTokenCount(request.content())).build());

		List<OpenAiChatClient.ChatMessage> context = conversationMessageRepository
				.findByConversationIdOrderBySequenceNoAsc(conversationId).stream()
				.skip(Math.max(0, nextSequence - CONTEXT_MESSAGE_LIMIT)).map(message -> new OpenAiChatClient.ChatMessage(
						message.getRole() == MessageRole.USER ? "user" : "assistant", message.getContent())).toList();
		String reply = openAiChatClient.reply(CHAT_SYSTEM_PROMPT, context);
		if (reply == null || reply.isBlank()) {
			throw new IllegalStateException("OpenAI returned an empty chat reply");
		}

		ConversationMessage assistantMessage = conversationMessageRepository.save(ConversationMessage.builder()
				.conversation(conversation).role(MessageRole.ASSISTANT).content(reply.trim()).sequenceNo(nextSequence + 1)
				.tokenCount(estimateTokenCount(reply)).build());
		return new MessageSendResponse(toMessageResponse(userMessage), toMessageResponse(assistantMessage));
	}

	@Transactional
	public ConversationResponse complete(Long conversationId) {
		Conversation conversation = conversationRepository.findWithLockById(conversationId)
				.orElseThrow(() -> new NotFoundException("해당 대화가 없습니다."));
		if (conversation.getStatus() == Conversation.ConversationStatus.COMPLETED) {
			return toResponse(conversation);
		}
		if (conversation.getStatus() != Conversation.ConversationStatus.IN_PROGRESS) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "시작되지 않은 대화는 완료할 수 없습니다.");
		}
		conversation.complete();
		streakService.recordActivity(conversation.getMember(), LocalDate.now(KOREA_ZONE));
		eventPublisher.publishEvent(new ConversationCompletedEvent(conversationId));
		return toResponse(conversation);
	}

	private Conversation findConversation(Long conversationId) {
		return conversationRepository.findById(conversationId)
				.orElseThrow(() -> new NotFoundException("해당 대화가 없습니다."));
	}

	private ConversationResponse toResponse(Conversation conversation) {
		return new ConversationResponse(conversation.getId(), conversation.getMember().getId(), conversation.getType(),
				conversation.getStatus(), conversation.getSessionDate(), conversation.getStartedAt(), conversation.getEndedAt(),
				conversation.getCreatedAt(), conversation.getUpdatedAt());
	}

	private ConversationMessageResponse toMessageResponse(ConversationMessage message) {
		return new ConversationMessageResponse(message.getId(), message.getConversation().getId(), message.getRole(),
				message.getContent(), message.getSequenceNo(), message.getTokenCount(), message.getCreatedAt());
	}

	private int estimateTokenCount(String content) {
		return (int) Math.ceil(content.length() / 3.5);
	}
}
