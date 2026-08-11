package br.com.itau.geradornotafiscal.application.service.calculo;

import br.com.itau.geradornotafiscal.domain.model.Destinatario;
import br.com.itau.geradornotafiscal.domain.model.Pedido;
import br.com.itau.geradornotafiscal.domain.service.tributacao.CalculadorTributacaoPorFaixa;
import br.com.itau.geradornotafiscal.domain.service.tributacao.CatalogoTributario;
import br.com.itau.geradornotafiscal.domain.service.tributacao.FaixaAliquota;
import br.com.itau.geradornotafiscal.domain.service.tributacao.ResultadoCalculoTributario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CalculadorTributosPedido {
    private static final Logger LOGGER = LoggerFactory.getLogger(CalculadorTributosPedido.class);

    private final CatalogoTributario catalogoTributario;
    private final CalculadorTributacaoPorFaixa calculadorTributacao;

    public CalculadorTributosPedido(
            CatalogoTributario catalogoTributario,
            CalculadorTributacaoPorFaixa calculadorTributacao) {
        this.catalogoTributario = catalogoTributario;
        this.calculadorTributacao = calculadorTributacao;
    }

    public ResultadoCalculoTributario calcular(Pedido pedido, double valorTotalItens) {
        Destinatario destinatario = pedido.getDestinatario();
        LOGGER.info(
                "Buscando faixas tributárias: pedidoId={}, tipoPessoa={}, regimeTributacao={}",
                pedido.getIdPedido(),
                destinatario.getTipoPessoa(),
                destinatario.getRegimeTributacao());
        List<FaixaAliquota> faixas = catalogoTributario.buscar(
                destinatario.getTipoPessoa(),
                destinatario.getRegimeTributacao());
        LOGGER.debug(
                "Faixas tributárias recuperadas: pedidoId={}, quantidadeFaixas={}, faixas={}",
                pedido.getIdPedido(),
                faixas.size(),
                faixas);

        LOGGER.info(
                "Iniciando cálculo dos tributos: pedidoId={}, valorTotalItens={}",
                pedido.getIdPedido(),
                valorTotalItens);
        ResultadoCalculoTributario resultado = calculadorTributacao.calcular(
                pedido.getItens(),
                valorTotalItens,
                faixas);
        FaixaAliquota faixaAplicada = resultado.faixaAplicada();
        LOGGER.info(
                "Cálculo dos tributos concluído: pedidoId={}, limiteFaixa={}, limiteInclusivo={}, aliquota={}, quantidadeItens={}",
                pedido.getIdPedido(),
                faixaAplicada.limiteSuperior(),
                faixaAplicada.inclusivo(),
                faixaAplicada.aliquota(),
                resultado.itens().size());
        return resultado;
    }
}
