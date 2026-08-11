package br.com.itau.geradornotafiscal.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.aws")
public record AwsProperties(
        String region,
        String endpoint,
        String accessKey,
        String secretKey,
        Sns sns,
        Retry retry,
        Secrets secrets) {

    public record Sns(String topicArn) {
    }

    public record Retry(int maxAttempts, Duration baseDelay, Duration maxBackoff) {
    }

    public record Secrets(String applicationSecretName) {
    }
}
