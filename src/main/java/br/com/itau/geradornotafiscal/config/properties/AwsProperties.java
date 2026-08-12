package br.com.itau.geradornotafiscal.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.aws")
@Validated
public record AwsProperties(
        @NotBlank String region,
        String endpoint,
        String accessKey,
        String secretKey,
        @Valid @NotNull Sns sns,
        @Valid @NotNull Retry retry,
        @Valid @NotNull Secrets secrets) {

    public record Sns(@NotBlank String topicArn) {
    }

    public record Retry(
            @Min(1) int maxAttempts,
            @NotNull Duration baseDelay,
            @NotNull Duration maxBackoff) {
    }

    public record Secrets(@NotBlank String applicationSecretName) {
    }
}
