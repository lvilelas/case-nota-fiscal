package br.com.itau.consumidoressimulados.application;

import br.com.itau.consumidoressimulados.domain.ConsumerType;
import br.com.itau.consumidoressimulados.domain.NotaFiscalGeradaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EntregaHandler implements NotaFiscalGeradaHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntregaHandler.class);
    private static final long INITIAL_LATENCY_MILLISECONDS = 150;
    private static final long FINAL_LATENCY_MILLISECONDS = 200;
    private static final long HIGH_VOLUME_LATENCY_MILLISECONDS = 5_000;
    private static final int HIGH_VOLUME_MINIMUM_ITEMS = 6;

    private final LatencySimulator latencySimulator;

    public EntregaHandler(LatencySimulator latencySimulator) {
        this.latencySimulator = latencySimulator;
    }

    @Override
    public ConsumerType type() {
        return ConsumerType.ENTREGA;
    }

    @Override
    public void handle(NotaFiscalGeradaEvent event) {
        int itemCount = event.notaFiscal().quantidadeItens();
        LOGGER.info("Agendando entrega: notaFiscalId={}, pedidoId={}, quantidadeItens={}",
                event.notaFiscal().idNotaFiscal(), event.pedidoId(), itemCount);

        latencySimulator.waitFor(INITIAL_LATENCY_MILLISECONDS);
        if (itemCount >= HIGH_VOLUME_MINIMUM_ITEMS) {
            LOGGER.warn("Fluxo de alta latencia da entrega: notaFiscalId={}, quantidadeItens={}",
                    event.notaFiscal().idNotaFiscal(), itemCount);
            latencySimulator.waitFor(HIGH_VOLUME_LATENCY_MILLISECONDS);
        }
        latencySimulator.waitFor(FINAL_LATENCY_MILLISECONDS);
    }
}
