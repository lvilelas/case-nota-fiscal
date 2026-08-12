package br.com.itau.geradornotafiscal.adapter.out.aws.secrets;

import br.com.itau.geradornotafiscal.application.exception.AcessoSegredoException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AwsSecretsManagerAdapterTest {
    @Test
    void deveBuscarSegredoSemRegistrarSeuValor() {
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(GetSecretValueRequest.class)))
                .thenReturn(GetSecretValueResponse.builder().secretString("valor-sensivel").build());

        String segredo = new AwsSecretsManagerAdapter(client).buscar("case-nota-fiscal/dev");

        assertEquals("valor-sensivel", segredo);
        verify(client).getSecretValue(GetSecretValueRequest.builder()
                .secretId("case-nota-fiscal/dev")
                .build());
    }

    @Test
    void deveTraduzirFalhaDaAwsParaExcecaoDaAplicacao() {
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        RuntimeException causa = new RuntimeException("AWS indisponivel");
        when(client.getSecretValue(any(GetSecretValueRequest.class))).thenThrow(causa);

        AcessoSegredoException exception = assertThrows(
                AcessoSegredoException.class,
                () -> new AwsSecretsManagerAdapter(client).buscar("case-nota-fiscal/prod"));

        assertEquals("Falha ao buscar segredo no AWS Secrets Manager: case-nota-fiscal/prod", exception.getMessage());
        assertSame(causa, exception.getCause());
    }
}
