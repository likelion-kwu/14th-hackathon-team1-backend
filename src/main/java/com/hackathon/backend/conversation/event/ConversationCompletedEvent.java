package com.hackathon.backend.conversation.event;

/** Published inside completion's transaction and consumed only after it commits. */
public record ConversationCompletedEvent(Long conversationId) {
}
