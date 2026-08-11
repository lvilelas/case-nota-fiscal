package br.com.itau.geradornotafiscal.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.outbox")
public record OutboxProperties(
        boolean enabled,
        int batchSize,
        Duration pollInterval,
        Duration lockDuration,
        Duration baseDelay,
        Duration maxBackoff,
        int maxAttempts) {
}
