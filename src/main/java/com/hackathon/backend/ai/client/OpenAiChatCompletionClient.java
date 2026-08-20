package com.hackathon.backend.ai.client;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import com.hackathon.backend.ai.config.OpenAiProperties;

@Component
public class OpenAiChatCompletionClient implements OpenAiChatClient {

	private static final Logger log = LoggerFactory.getLogger(OpenAiChatCompletionClient.class);
	private final RestClient restClient;
	private final String model;

	public OpenAiChatCompletionClient(OpenAiProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.connectTimeout());
		requestFactory.setReadTimeout(properties.readTimeout());
		this.model = properties.model();
		this.restClient = RestClient.builder().baseUrl(properties.baseUrl())
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
				.requestFactory(requestFactory)
				.build();
	}

	@Override
	public String complete(String systemPrompt, String userPrompt) {
		return request(List.of(new Message("system", systemPrompt), new Message("user", userPrompt)),
				Map.of("type", "json_object"));
	}

	@Override
	public String modelName() {
		return model;
	}

	@Override
	public String reply(String systemPrompt, List<ChatMessage> messages) {
		List<Message> requestMessages = new java.util.ArrayList<>();
		requestMessages.add(new Message("system", systemPrompt));
		messages.forEach(message -> requestMessages.add(new Message(message.role(), message.content())));
		return request(requestMessages, null);
	}

	private String request(List<Message> messages, Map<String, String> responseFormat) {
		String clientRequestId = UUID.randomUUID().toString();
		OpenAiResponse response;
		try {
			ResponseEntity<OpenAiResponse> responseEntity = restClient.post()
					.uri("/chat/completions")
					.contentType(MediaType.APPLICATION_JSON)
					.header("X-Client-Request-Id", clientRequestId)
					.body(new OpenAiRequest(model, messages, responseFormat))
					.retrieve()
					.toEntity(OpenAiResponse.class);
			response = responseEntity.getBody();
			log.debug("AI chat completion succeeded: clientRequestId={}, providerRequestId={}, model={}", clientRequestId,
					responseEntity.getHeaders().getFirst("x-request-id"), model);
		} catch (RestClientResponseException exception) {
			log.warn("AI chat completion failed: clientRequestId={}, providerRequestId={}, status={}", clientRequestId,
					exception.getResponseHeaders().getFirst("x-request-id"), exception.getStatusCode().value());
			throw exception;
		}

		if (response == null || response.choices() == null || response.choices().isEmpty()
				|| response.choices().get(0).message() == null || response.choices().get(0).message().content() == null) {
			throw new IllegalStateException("AI provider response does not contain a completion message");
		}
		return response.choices().get(0).message().content();
	}

	private record OpenAiRequest(String model, List<Message> messages, Map<String, String> response_format) {
	}

	private record Message(String role, String content) {
	}

	private record OpenAiResponse(List<Choice> choices) {
	}

	private record Choice(Message message) {
	}
}
