package br.com.itau.consumidoressimulados.application;

import br.com.itau.consumidoressimulados.domain.ConsumerType;
import br.com.itau.consumidoressimulados.domain.NotaFiscalGeradaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FinanceiroHandler implements NotaFiscalGeradaHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FinanceiroHandler.class);
    private static final long LATENCY_MILLISECONDS = 250;

    private final LatencySimulator latencySimulator;

    public FinanceiroHandler(LatencySimulator latencySimulator) {
        this.latencySimulator = latencySimulator;
    }

    @Override
    public ConsumerType type() {
        return ConsumerType.FINANCEIRO;
    }

    @Override
    public void handle(NotaFiscalGeradaEvent event) {
        LOGGER.info("Enviando nota fiscal ao financeiro: notaFiscalId={}, pedidoId={}",
                event.notaFiscal().idNotaFiscal(), event.pedidoId());
        latencySimulator.waitFor(LATENCY_MILLISECONDS);
    }
}
