package br.com.itau.geradornotafiscal.application.port.out;

import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;

public interface PublicarNotaFiscalGeradaPort {
    void publicar(int pedidoId, NotaFiscal notaFiscal);
}
