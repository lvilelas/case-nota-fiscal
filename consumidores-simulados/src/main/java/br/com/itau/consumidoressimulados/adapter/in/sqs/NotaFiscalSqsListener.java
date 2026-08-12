package br.com.itau.consumidoressimulados.adapter.in.sqs;

import br.com.itau.consumidoressimulados.domain.ConsumerType;

import java.util.function.BooleanSupplier;

public interface NotaFiscalSqsListener {

    ConsumerType type();

    void listen(BooleanSupplier isRunning);
}
