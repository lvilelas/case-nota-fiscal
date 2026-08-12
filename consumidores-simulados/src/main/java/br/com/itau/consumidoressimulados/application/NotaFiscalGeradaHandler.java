package br.com.itau.consumidoressimulados.application;

import br.com.itau.consumidoressimulados.domain.ConsumerType;
import br.com.itau.consumidoressimulados.domain.NotaFiscalGeradaEvent;

public interface NotaFiscalGeradaHandler {

    ConsumerType type();

    void handle(NotaFiscalGeradaEvent event);
}
