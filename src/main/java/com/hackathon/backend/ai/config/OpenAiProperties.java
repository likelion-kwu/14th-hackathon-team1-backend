package com.hackathon.backend.ai.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties("app.ai.openai")
public record OpenAiProperties(
        @NotBlank String apiKey,
        @NotBlank String model,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout) {

    public OpenAiProperties {
        if (connectTimeout.isZero() || connectTimeout.isNegative()) {
            throw new IllegalArgumentException("connectTimeout must be positive");
        }
        if (readTimeout.isZero() || readTimeout.isNegative()) {
            throw new IllegalArgumentException("readTimeout must be positive");
        }
    }
}
