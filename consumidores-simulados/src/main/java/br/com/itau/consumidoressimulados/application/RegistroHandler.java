package br.com.itau.consumidoressimulados.application;

import br.com.itau.consumidoressimulados.domain.ConsumerType;
import br.com.itau.consumidoressimulados.domain.NotaFiscalGeradaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RegistroHandler implements NotaFiscalGeradaHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistroHandler.class);
    private static final long LATENCY_MILLISECONDS = 500;

    private final LatencySimulator latencySimulator;

    public RegistroHandler(LatencySimulator latencySimulator) {
        this.latencySimulator = latencySimulator;
    }

    @Override
    public ConsumerType type() {
        return ConsumerType.REGISTRO;
    }

    @Override
    public void handle(NotaFiscalGeradaEvent event) {
        LOGGER.info("Registrando nota fiscal: notaFiscalId={}, pedidoId={}",
                event.notaFiscal().idNotaFiscal(), event.pedidoId());
        latencySimulator.waitFor(LATENCY_MILLISECONDS);
    }
}
