package br.com.itau.geradornotafiscal.application.service.factory;

import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import br.com.itau.geradornotafiscal.domain.model.Pedido;
import br.com.itau.geradornotafiscal.domain.service.frete.ResultadoCalculoFrete;
import br.com.itau.geradornotafiscal.domain.service.tributacao.ResultadoCalculoTributario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.UUID;

public class NotaFiscalFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotaFiscalFactory.class);

    public NotaFiscal criar(
            Pedido pedido,
            double valorTotalItens,
            ResultadoCalculoTributario resultadoTributario,
            ResultadoCalculoFrete resultadoFrete) {
        NotaFiscal notaFiscal = NotaFiscal.builder()
                .idNotaFiscal(UUID.randomUUID().toString())
                .data(LocalDateTime.now())
                .valorTotalItens(valorTotalItens)
                .valorFrete(resultadoFrete.valorCalculado())
                .itens(resultadoTributario.itens())
                .destinatario(pedido.getDestinatario())
                .build();

        LOGGER.info(
                "Nota fiscal montada: pedidoId={}, notaFiscalId={}, valorTotalItens={}, valorFrete={}, quantidadeItens={}",
                pedido.getIdPedido(),
                notaFiscal.getIdNotaFiscal(),
                notaFiscal.getValorTotalItens(),
                notaFiscal.getValorFrete(),
                notaFiscal.getItens().size());
        return notaFiscal;
    }
}
