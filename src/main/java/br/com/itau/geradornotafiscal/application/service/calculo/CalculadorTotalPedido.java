package br.com.itau.geradornotafiscal.application.service.calculo;

import br.com.itau.geradornotafiscal.domain.model.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class CalculadorTotalPedido {
    private static final Logger LOGGER = LoggerFactory.getLogger(CalculadorTotalPedido.class);

    public double calcular(Pedido pedido) {
        LOGGER.info(
                "Iniciando cálculo do valor total dos itens: pedidoId={}, quantidadeItens={}",
                pedido.getIdPedido(),
                pedido.getItens().size());

        double valorCalculado = pedido.getItens().stream()
                .map(item -> BigDecimal.valueOf(item.getValorUnitario())
                        .multiply(BigDecimal.valueOf(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .doubleValue();

        LOGGER.info(
                "Valor total dos itens calculado: pedidoId={}, valorTotalInformado={}, valorTotalCalculado={}",
                pedido.getIdPedido(),
                pedido.getValorTotalItens(),
                valorCalculado);
        return valorCalculado;
    }
}
