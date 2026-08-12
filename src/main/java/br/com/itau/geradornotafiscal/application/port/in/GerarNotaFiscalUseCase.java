package br.com.itau.geradornotafiscal.application.port.in;

import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import br.com.itau.geradornotafiscal.domain.model.Pedido;

public interface GerarNotaFiscalUseCase {
	NotaFiscal gerarNotaFiscal(Pedido pedido);
}
