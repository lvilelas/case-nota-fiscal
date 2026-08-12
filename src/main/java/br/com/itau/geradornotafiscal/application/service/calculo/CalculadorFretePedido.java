package br.com.itau.geradornotafiscal.application.service.calculo;

import br.com.itau.geradornotafiscal.domain.model.Endereco;
import br.com.itau.geradornotafiscal.domain.model.Finalidade;
import br.com.itau.geradornotafiscal.domain.model.Pedido;
import br.com.itau.geradornotafiscal.domain.model.Regiao;
import br.com.itau.geradornotafiscal.domain.service.frete.CalculadorFrete;
import br.com.itau.geradornotafiscal.domain.service.frete.ResultadoCalculoFrete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CalculadorFretePedido {
    private static final Logger LOGGER = LoggerFactory.getLogger(CalculadorFretePedido.class);

    private final CalculadorFrete calculadorFrete;

    public CalculadorFretePedido(CalculadorFrete calculadorFrete) {
        this.calculadorFrete = calculadorFrete;
    }

    public ResultadoCalculoFrete calcular(Pedido pedido) {
        Regiao regiao = pedido.getDestinatario().getEnderecos().stream()
                .filter(endereco -> endereco.getFinalidade() == Finalidade.ENTREGA
                        || endereco.getFinalidade() == Finalidade.COBRANCA_ENTREGA)
                .map(Endereco::getRegiao)
                .findFirst()
                .orElse(null);

        LOGGER.info(
                "Iniciando cálculo do frete: pedidoId={}, regiao={}, valorFreteOriginal={}",
                pedido.getIdPedido(),
                regiao,
                pedido.getValorFrete());
        ResultadoCalculoFrete resultado = calculadorFrete.calcular(pedido.getValorFrete(), regiao);
        LOGGER.info(
                "Cálculo do frete concluído: pedidoId={}, regiao={}, multiplicador={}, valorFreteCalculado={}",
                pedido.getIdPedido(),
                resultado.regiao(),
                resultado.multiplicador(),
                resultado.valorCalculado());
        return resultado;
    }
}
