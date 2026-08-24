package com.hackathon.backend.conversation.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
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
	private static final int MAX_AI_RETRY = 2;
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

	/**
	 * 메시지 전송.
	 *
	 * <p>동일 대화에 대한 동시 요청은 conversation row의 pessimistic lock으로 직렬화합니다.
	 * OpenAI 일시 오류는 최대 {@value MAX_AI_RETRY}회 재시도하며, 최종 실패 시 503을 반환합니다.
	 * 실패한 경우 트랜잭션이 롤백되므로 사용자 메시지도 저장되지 않습니다.
	 *
	 * <p>clientMessageId가 제공된 경우 이미 처리된 요청인지 확인하고,
	 * 존재하면 저장된 응답을 그대로 반환합니다(멱등성 보장).
	 */
	@Transactional
	public MessageSendResponse sendMessage(Long conversationId, MessageSendRequest request, String clientMessageId) {
		// 멱등성 키 확인: 이미 처리된 요청이면 저장된 응답을 그대로 반환
		if (clientMessageId != null) {
			return conversationMessageRepository.findByClientMessageId(clientMessageId)
					.map(userMsg -> {
						ConversationMessage assistantMsg = conversationMessageRepository
								.findByConversationIdAndSequenceNo(conversationId, userMsg.getSequenceNo() + 1)
								.orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
										"응답 메시지를 찾을 수 없습니다."));
						return new MessageSendResponse(toMessageResponse(userMsg), toMessageResponse(assistantMsg));
					})
					.orElseGet(() -> processNewMessage(conversationId, request, clientMessageId));
		}
		return processNewMessage(conversationId, request, null);
	}

	/**
	 * 실제 메시지 저장 및 AI 호출을 수행합니다.
	 */
	private MessageSendResponse processNewMessage(Long conversationId, MessageSendRequest request, String clientMessageId) {
		// pessimistic lock으로 동시 요청 직렬화 + sequenceNo 경쟁 조건 방지
		Conversation conversation = conversationRepository.findWithLockById(conversationId)
				.orElseThrow(() -> new NotFoundException("해당 대화가 없습니다."));
		if (conversation.getStatus() != Conversation.ConversationStatus.IN_PROGRESS) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "진행 중인 대화가 아닙니다.");
		}

		int nextSequence = Math.toIntExact(conversationMessageRepository.countByConversationId(conversationId)) + 1;
		ConversationMessage userMessage = conversationMessageRepository.save(ConversationMessage.builder()
				.conversation(conversation).role(MessageRole.USER).content(request.content()).sequenceNo(nextSequence)
				.tokenCount(estimateTokenCount(request.content())).clientMessageId(clientMessageId).build());

		List<OpenAiChatClient.ChatMessage> context = conversationMessageRepository
				.findByConversationIdOrderBySequenceNoAsc(conversationId).stream()
				.skip(Math.max(0, nextSequence - CONTEXT_MESSAGE_LIMIT)).map(message -> new OpenAiChatClient.ChatMessage(
						message.getRole() == MessageRole.USER ? "user" : "assistant", message.getContent())).toList();

		// 실패 시 예외가 던져지므로 @Transactional 롤백으로 userMessage도 함께 미저장
		String reply = callOpenAiWithRetry(context);

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

	/**
	 * OpenAI 호출을 최대 {@value MAX_AI_RETRY}회 재시도합니다.
	 * 일시 오류(네트워크, 408, 429, 5xx)에만 재시도하며 최종 실패 시 503을 던집니다.
	 */
	private String callOpenAiWithRetry(List<OpenAiChatClient.ChatMessage> context) {
		Exception lastException = null;
		for (int attempt = 0; attempt <= MAX_AI_RETRY; attempt++) {
			try {
				String reply = openAiChatClient.reply(CHAT_SYSTEM_PROMPT, context);
				if (reply == null || reply.isBlank()) {
					lastException = new IllegalStateException("OpenAI returned an empty reply");
					continue;
				}
				return reply;
			} catch (Exception e) {
				if (isRetryable(e)) {
					lastException = e;
				} else {
					throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
							"AI 응답 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.", e);
				}
			}
		}
		throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
				"AI 응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요.", lastException);
	}

	private boolean isRetryable(Exception e) {
		if (e instanceof ResourceAccessException) {
			return true;
		}
		if (e instanceof ResponseStatusException rse) {
			int status = rse.getStatusCode().value();
			return status == 408 || status == 429 || status >= 500;
		}
		return false;
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
