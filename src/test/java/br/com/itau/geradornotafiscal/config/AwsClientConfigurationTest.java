package br.com.itau.geradornotafiscal.config;

import br.com.itau.geradornotafiscal.adapter.out.aws.secrets.AwsSecretsManagerAdapter;
import br.com.itau.geradornotafiscal.adapter.out.aws.sns.SnsNotaFiscalGeradaAdapter;
import br.com.itau.geradornotafiscal.config.properties.AwsProperties;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.sns.SnsClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class AwsClientConfigurationTest {
    private final AwsClientConfiguration configuration = new AwsClientConfiguration();

    @Test
    void deveCriarClientesLocaisComEndpointECredenciaisEstaticas() {
        AwsProperties properties = properties("http://localhost:4566", "test", "test");

        try (SnsClient snsClient = configuration.snsClient(properties);
             SecretsManagerClient secretsClient = configuration.secretsManagerClient(properties)) {
            assertInstanceOf(
                    SnsNotaFiscalGeradaAdapter.class,
                    configuration.publicarNotaFiscalGeradaPort(snsClient, mock(ObjectMapper.class), properties));
            assertInstanceOf(
                    AwsSecretsManagerAdapter.class,
                    configuration.buscarSegredoPort(secretsClient));
        }
    }

    @Test
    void deveCriarClientesAwsSemEndpointECredenciaisFixas() {
        AwsProperties properties = properties("", "", "");

        try (SnsClient snsClient = configuration.snsClient(properties);
             SecretsManagerClient secretsClient = configuration.secretsManagerClient(properties)) {
            assertInstanceOf(SnsClient.class, snsClient);
            assertInstanceOf(SecretsManagerClient.class, secretsClient);
        }
    }

    @Test
    void deveUsarProviderPadraoSeSomenteAccessKeyForInformada() {
        AwsProperties properties = properties(null, "access-key", "");

        try (SnsClient snsClient = configuration.snsClient(properties)) {
            assertInstanceOf(SnsClient.class, snsClient);
        }
    }

    private AwsProperties properties(String endpoint, String accessKey, String secretKey) {
        return new AwsProperties(
                "us-east-1",
                endpoint,
                accessKey,
                secretKey,
                new AwsProperties.Sns("arn:aws:sns:us-east-1:123456789012:nota-fiscal-gerada"),
                new AwsProperties.Retry(4, Duration.ofMillis(200), Duration.ofSeconds(5)),
                new AwsProperties.Secrets("case-nota-fiscal/test"));
    }
}
