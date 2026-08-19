package com.hackathon.backend.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties("app.ai.openai")
public record OpenAiProperties(@NotBlank String apiKey) {
}
