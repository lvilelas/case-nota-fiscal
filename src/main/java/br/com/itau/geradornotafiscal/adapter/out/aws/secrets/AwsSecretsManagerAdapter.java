package br.com.itau.geradornotafiscal.adapter.out.aws.secrets;

import br.com.itau.geradornotafiscal.application.exception.AcessoSegredoException;
import br.com.itau.geradornotafiscal.application.port.out.BuscarSegredoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

public class AwsSecretsManagerAdapter implements BuscarSegredoPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSecretsManagerAdapter.class);

    private final SecretsManagerClient secretsManagerClient;

    public AwsSecretsManagerAdapter(SecretsManagerClient secretsManagerClient) {
        this.secretsManagerClient = secretsManagerClient;
    }

    @Override
    public String buscar(String secretId) {
        try {
            LOGGER.info("Buscando segredo no AWS Secrets Manager: secretId={}", secretId);
            String secretString = secretsManagerClient.getSecretValue(GetSecretValueRequest.builder()
                            .secretId(secretId)
                            .build())
                    .secretString();
            LOGGER.info("Segredo recuperado no AWS Secrets Manager: secretId={}", secretId);
            return secretString;
        } catch (RuntimeException exception) {
            throw new AcessoSegredoException(
                    "Falha ao buscar segredo no AWS Secrets Manager: " + secretId,
                    exception);
        }
    }
}
