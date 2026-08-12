package br.com.itau.consumidoressimulados.application;

import br.com.itau.consumidoressimulados.domain.ConsumerType;
import br.com.itau.consumidoressimulados.domain.NotaFiscalGeradaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EstoqueHandler implements NotaFiscalGeradaHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(EstoqueHandler.class);
    private static final long LATENCY_MILLISECONDS = 380;

    private final LatencySimulator latencySimulator;

    public EstoqueHandler(LatencySimulator latencySimulator) {
        this.latencySimulator = latencySimulator;
    }

    @Override
    public ConsumerType type() {
        return ConsumerType.ESTOQUE;
    }

    @Override
    public void handle(NotaFiscalGeradaEvent event) {
        LOGGER.info("Baixando estoque: notaFiscalId={}, pedidoId={}",
                event.notaFiscal().idNotaFiscal(), event.pedidoId());
        latencySimulator.waitFor(LATENCY_MILLISECONDS);
    }
}
