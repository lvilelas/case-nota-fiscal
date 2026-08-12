package br.com.itau.geradornotafiscal.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.outbox")
@Validated
public record OutboxProperties(
        boolean enabled,
        @Min(1) int batchSize,
        @NotNull Duration pollInterval,
        @NotNull Duration lockDuration,
        @NotNull Duration baseDelay,
        @NotNull Duration maxBackoff,
        @Min(1) int maxAttempts) {
}
