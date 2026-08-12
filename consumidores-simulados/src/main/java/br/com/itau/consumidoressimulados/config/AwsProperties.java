package br.com.itau.consumidoressimulados.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("app.aws")
@Validated
public record AwsProperties(
        @NotBlank String region,
        String endpoint,
        String accessKey,
        String secretKey) {
}
