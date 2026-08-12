package br.com.itau.consumidoressimulados.config;

import br.com.itau.consumidoressimulados.domain.ConsumerType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.consumer")
public record ConsumerProperties(
        ConsumerType type,
        String queuePrefix,
        int waitTimeSeconds,
        int visibilityTimeoutSeconds,
        int maxMessages,
        Duration errorBackoff) {
}
