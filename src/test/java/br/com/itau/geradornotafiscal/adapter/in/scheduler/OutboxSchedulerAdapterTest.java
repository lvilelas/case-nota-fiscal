package br.com.itau.geradornotafiscal.adapter.in.scheduler;

import br.com.itau.geradornotafiscal.application.service.integration.PublicadorOutboxNotaFiscal;
import br.com.itau.geradornotafiscal.config.properties.OutboxProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OutboxSchedulerAdapterTest {
    @Test
    void deveAcionarPublicadorComConfiguracaoDoLote() {
        PublicadorOutboxNotaFiscal publicador = mock(PublicadorOutboxNotaFiscal.class);
        OutboxProperties properties = properties();

        new OutboxSchedulerAdapter(publicador, properties).publicarPendentes();

        verify(publicador).publicarPendentes(20, Duration.ofSeconds(30));
    }

    @Test
    void deveConterFalhaParaManterSchedulerAtivo() {
        PublicadorOutboxNotaFiscal publicador = mock(PublicadorOutboxNotaFiscal.class);
        OutboxProperties properties = properties();
        doThrow(new RuntimeException("banco indisponivel"))
                .when(publicador).publicarPendentes(20, Duration.ofSeconds(30));

        assertDoesNotThrow(() -> new OutboxSchedulerAdapter(publicador, properties).publicarPendentes());
    }

    private OutboxProperties properties() {
        return new OutboxProperties(
                true,
                20,
                Duration.ofSeconds(1),
                Duration.ofSeconds(30),
                Duration.ofSeconds(1),
                Duration.ofMinutes(5),
                10);
    }
}
