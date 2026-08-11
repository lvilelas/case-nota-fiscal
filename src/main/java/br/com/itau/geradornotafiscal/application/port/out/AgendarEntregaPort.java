package br.com.itau.geradornotafiscal.application.port.out;

import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;

public interface AgendarEntregaPort {
    void agendar(NotaFiscal notaFiscal);
}
