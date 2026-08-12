package br.com.itau.consumidoressimulados.config;

import br.com.itau.consumidoressimulados.domain.ConsumerType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties("app.consumer")
@Validated
public record ConsumerProperties(
        @NotNull ConsumerType type,
        @NotBlank String queuePrefix,
        @Min(0) @Max(20) int waitTimeSeconds,
        @Min(0) @Max(43_200) int visibilityTimeoutSeconds,
        @Min(1) @Max(10) int maxMessages,
        @NotNull Duration errorBackoff) {
}
