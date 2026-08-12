package br.com.itau.geradornotafiscal.application.port.out;

import br.com.itau.geradornotafiscal.application.event.NotaFiscalGeradaEvent;

public interface PublicarNotaFiscalGeradaPort {
    void publicar(NotaFiscalGeradaEvent evento);
}
