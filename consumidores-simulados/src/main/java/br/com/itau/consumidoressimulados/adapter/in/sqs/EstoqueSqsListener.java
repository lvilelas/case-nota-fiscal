package br.com.itau.consumidoressimulados.adapter.in.sqs;

import br.com.itau.consumidoressimulados.application.EstoqueHandler;
import br.com.itau.consumidoressimulados.domain.ConsumerType;
import org.springframework.stereotype.Component;

import java.util.function.BooleanSupplier;

@Component
public class EstoqueSqsListener implements NotaFiscalSqsListener {
    private final SqsMessageProcessor processor;
    private final EstoqueHandler handler;

    public EstoqueSqsListener(SqsMessageProcessor processor, EstoqueHandler handler) {
        this.processor = processor;
        this.handler = handler;
    }

    @Override
    public ConsumerType type() {
        return ConsumerType.ESTOQUE;
    }

    @Override
    public void listen(BooleanSupplier isRunning) {
        processor.listen(type(), handler, isRunning);
    }
}
