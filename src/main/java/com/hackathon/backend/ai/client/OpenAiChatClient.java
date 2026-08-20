package com.hackathon.backend.ai.client;

import java.util.List;

/** OpenAI API boundary for structured analysis and user-visible chat replies. */
public interface OpenAiChatClient {

	String complete(String systemPrompt, String userPrompt);

	default String reply(String systemPrompt, List<ChatMessage> messages) {
		throw new UnsupportedOperationException("Chat replies are not configured");
	}

	record ChatMessage(String role, String content) {
	}
}
