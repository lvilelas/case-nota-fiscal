package br.com.itau.consumidoressimulados.adapter.in.sqs;

import br.com.itau.consumidoressimulados.application.EntregaHandler;
import br.com.itau.consumidoressimulados.domain.ConsumerType;
import org.springframework.stereotype.Component;

import java.util.function.BooleanSupplier;

@Component
public class EntregaSqsListener implements NotaFiscalSqsListener {
    private final SqsMessageProcessor processor;
    private final EntregaHandler handler;

    public EntregaSqsListener(SqsMessageProcessor processor, EntregaHandler handler) {
        this.processor = processor;
        this.handler = handler;
    }

    @Override
    public ConsumerType type() {
        return ConsumerType.ENTREGA;
    }

    @Override
    public void listen(BooleanSupplier isRunning) {
        processor.listen(type(), handler, isRunning);
    }
}
