package br.com.itau.geradornotafiscal.adapter.in.scheduler;

import br.com.itau.geradornotafiscal.application.service.integration.PublicadorOutboxNotaFiscal;
import br.com.itau.geradornotafiscal.config.properties.OutboxProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxSchedulerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxSchedulerAdapter.class);

    private final PublicadorOutboxNotaFiscal publicador;
    private final OutboxProperties properties;

    public OutboxSchedulerAdapter(PublicadorOutboxNotaFiscal publicador, OutboxProperties properties) {
        this.publicador = publicador;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval:1s}")
    public void publicarPendentes() {
        try {
            publicador.publicarPendentes(properties.batchSize(), properties.lockDuration());
        } catch (RuntimeException exception) {
            LOGGER.error("Falha inesperada ao consultar a outbox", exception);
        }
    }
}
