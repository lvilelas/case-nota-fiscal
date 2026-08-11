package br.com.itau.geradornotafiscal.application.port.out;

import br.com.itau.geradornotafiscal.application.event.NotaFiscalGeradaEvent;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;

import java.util.Optional;

public interface NotaFiscalIdempotenciaPort {
    Optional<NotaFiscal> buscarPorPedidoId(int pedidoId);

    NotaFiscal salvarSeAusente(NotaFiscalGeradaEvent evento);
}
