package br.com.itau.consumidoressimulados.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.aws")
public record AwsProperties(
        String region,
        String endpoint,
        String accessKey,
        String secretKey) {
}
