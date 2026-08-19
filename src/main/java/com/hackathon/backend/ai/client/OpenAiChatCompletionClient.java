package com.hackathon.backend.ai.client;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.hackathon.backend.ai.config.OpenAiProperties;

@Component
public class OpenAiChatCompletionClient implements OpenAiChatClient {

	private static final String MODEL = "gpt-4o-mini";
	private final RestClient restClient;

	public OpenAiChatCompletionClient(OpenAiProperties properties) {
		this.restClient = RestClient.builder().baseUrl("https://api.openai.com/v1")
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
				.build();
	}

	@Override
	public String complete(String systemPrompt, String userPrompt) {
		OpenAiResponse response = restClient.post()
				.uri("/chat/completions")
				.contentType(MediaType.APPLICATION_JSON)
				.body(new OpenAiRequest(MODEL, List.of(
						new Message("system", systemPrompt),
						new Message("user", userPrompt)),
						Map.of("type", "json_object")))
				.retrieve()
				.body(OpenAiResponse.class);

		if (response == null || response.choices() == null || response.choices().isEmpty()
				|| response.choices().get(0).message() == null || response.choices().get(0).message().content() == null) {
			throw new IllegalStateException("OpenAI response does not contain a completion message");
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
