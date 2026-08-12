package br.com.itau.consumidoressimulados.adapter.in.sqs;

import br.com.itau.consumidoressimulados.application.FinanceiroHandler;
import br.com.itau.consumidoressimulados.domain.ConsumerType;
import org.springframework.stereotype.Component;

import java.util.function.BooleanSupplier;

@Component
public class FinanceiroSqsListener implements NotaFiscalSqsListener {
    private final SqsMessageProcessor processor;
    private final FinanceiroHandler handler;

    public FinanceiroSqsListener(SqsMessageProcessor processor, FinanceiroHandler handler) {
        this.processor = processor;
        this.handler = handler;
    }

    @Override
    public ConsumerType type() {
        return ConsumerType.FINANCEIRO;
    }

    @Override
    public void listen(BooleanSupplier isRunning) {
        processor.listen(type(), handler, isRunning);
    }
}
