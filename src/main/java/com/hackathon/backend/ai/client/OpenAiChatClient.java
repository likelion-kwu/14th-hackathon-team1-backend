package com.hackathon.backend.ai.client;

/** OpenAI Chat Completions API boundary. */
public interface OpenAiChatClient {

	String complete(String systemPrompt, String userPrompt);
}
