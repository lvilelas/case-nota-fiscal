package br.com.itau.geradornotafiscal.config;

import br.com.itau.geradornotafiscal.adapter.out.aws.secrets.AwsSecretsManagerAdapter;
import br.com.itau.geradornotafiscal.adapter.out.aws.sns.SnsNotaFiscalGeradaAdapter;
import br.com.itau.geradornotafiscal.application.port.out.BuscarSegredoPort;
import br.com.itau.geradornotafiscal.application.port.out.PublicarNotaFiscalGeradaPort;
import br.com.itau.geradornotafiscal.config.properties.AwsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.FullJitterBackoffStrategy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.sns.SnsClient;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(AwsProperties.class)
public class AwsClientConfiguration {
    @Bean
    SnsClient snsClient(AwsProperties properties) {
        var builder = SnsClient.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(credentialsProvider(properties))
                .overrideConfiguration(overrideConfiguration(properties));
        if (StringUtils.hasText(properties.endpoint())) {
            builder.endpointOverride(URI.create(properties.endpoint()));
        }
        return builder.build();
    }

    @Bean
    SecretsManagerClient secretsManagerClient(AwsProperties properties) {
        var builder = SecretsManagerClient.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(credentialsProvider(properties))
                .overrideConfiguration(overrideConfiguration(properties));
        if (StringUtils.hasText(properties.endpoint())) {
            builder.endpointOverride(URI.create(properties.endpoint()));
        }
        return builder.build();
    }

    @Bean
    PublicarNotaFiscalGeradaPort publicarNotaFiscalGeradaPort(
            SnsClient snsClient,
            ObjectMapper objectMapper,
            AwsProperties properties) {
        return new SnsNotaFiscalGeradaAdapter(snsClient, objectMapper, properties);
    }

    @Bean
    BuscarSegredoPort buscarSegredoPort(SecretsManagerClient secretsManagerClient) {
        return new AwsSecretsManagerAdapter(secretsManagerClient);
    }

    private AwsCredentialsProvider credentialsProvider(AwsProperties properties) {
        if (StringUtils.hasText(properties.accessKey())
                && StringUtils.hasText(properties.secretKey())) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(
                    properties.accessKey(),
                    properties.secretKey()));
        }
        return DefaultCredentialsProvider.create();
    }

    private ClientOverrideConfiguration overrideConfiguration(AwsProperties properties) {
        var backoff = FullJitterBackoffStrategy.builder()
                .baseDelay(properties.retry().baseDelay())
                .maxBackoffTime(properties.retry().maxBackoff())
                .build();
        return ClientOverrideConfiguration.builder()
                .retryPolicy(RetryPolicy.builder()
                        .numRetries(properties.retry().maxAttempts() - 1)
                        .backoffStrategy(backoff)
                        .throttlingBackoffStrategy(backoff)
                        .build())
                .build();
    }
}
