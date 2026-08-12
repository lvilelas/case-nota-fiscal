package br.com.itau.consumidoressimulados.adapter.in.sqs;

import br.com.itau.consumidoressimulados.application.RegistroHandler;
import br.com.itau.consumidoressimulados.domain.ConsumerType;
import org.springframework.stereotype.Component;

import java.util.function.BooleanSupplier;

@Component
public class RegistroSqsListener implements NotaFiscalSqsListener {
    private final SqsMessageProcessor processor;
    private final RegistroHandler handler;

    public RegistroSqsListener(SqsMessageProcessor processor, RegistroHandler handler) {
        this.processor = processor;
        this.handler = handler;
    }

    @Override
    public ConsumerType type() {
        return ConsumerType.REGISTRO;
    }

    @Override
    public void listen(BooleanSupplier isRunning) {
        processor.listen(type(), handler, isRunning);
    }
}
