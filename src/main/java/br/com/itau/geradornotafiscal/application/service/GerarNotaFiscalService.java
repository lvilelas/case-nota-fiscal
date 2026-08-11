package br.com.itau.geradornotafiscal.application.service;

import br.com.itau.geradornotafiscal.application.port.in.GerarNotaFiscalUseCase;
import br.com.itau.geradornotafiscal.application.service.calculo.CalculadorFretePedido;
import br.com.itau.geradornotafiscal.application.service.calculo.CalculadorTotalPedido;
import br.com.itau.geradornotafiscal.application.service.calculo.CalculadorTributosPedido;
import br.com.itau.geradornotafiscal.application.service.factory.NotaFiscalFactory;
import br.com.itau.geradornotafiscal.application.service.integration.OrquestradorIntegracoesNotaFiscal;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import br.com.itau.geradornotafiscal.domain.model.Pedido;
import br.com.itau.geradornotafiscal.domain.service.frete.ResultadoCalculoFrete;
import br.com.itau.geradornotafiscal.domain.service.tributacao.ResultadoCalculoTributario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GerarNotaFiscalService implements GerarNotaFiscalUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(GerarNotaFiscalService.class);

    private final CalculadorTotalPedido calculadorTotalPedido;
    private final CalculadorTributosPedido calculadorTributosPedido;
    private final CalculadorFretePedido calculadorFretePedido;
    private final NotaFiscalFactory notaFiscalFactory;
    private final OrquestradorIntegracoesNotaFiscal orquestradorIntegracoes;

    public GerarNotaFiscalService(
            CalculadorTotalPedido calculadorTotalPedido,
            CalculadorTributosPedido calculadorTributosPedido,
            CalculadorFretePedido calculadorFretePedido,
            NotaFiscalFactory notaFiscalFactory,
            OrquestradorIntegracoesNotaFiscal orquestradorIntegracoes) {
        this.calculadorTotalPedido = calculadorTotalPedido;
        this.calculadorTributosPedido = calculadorTributosPedido;
        this.calculadorFretePedido = calculadorFretePedido;
        this.notaFiscalFactory = notaFiscalFactory;
        this.orquestradorIntegracoes = orquestradorIntegracoes;
    }

    @Override
    public NotaFiscal gerarNotaFiscal(Pedido pedido) {
        long inicioProcessamento = System.nanoTime();
        LOGGER.info(
                "Iniciando geração da nota fiscal: pedidoId={}, quantidadeItens={}, tipoPessoa={}, regimeTributacao={}",
                pedido.getIdPedido(),
                pedido.getItens().size(),
                pedido.getDestinatario().getTipoPessoa(),
                pedido.getDestinatario().getRegimeTributacao());

        try {
            NotaFiscal notaFiscal = processar(pedido);
            LOGGER.info(
                    "Geração da nota fiscal concluída: pedidoId={}, notaFiscalId={}, duracaoMs={}",
                    pedido.getIdPedido(),
                    notaFiscal.getIdNotaFiscal(),
                    duracaoEmMilissegundos(inicioProcessamento));
            return notaFiscal;
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Falha na geração da nota fiscal: pedidoId={}, duracaoMs={}, tipoErro={}",
                    pedido.getIdPedido(),
                    duracaoEmMilissegundos(inicioProcessamento),
                    exception.getClass().getSimpleName(),
                    exception);
            throw exception;
        }
    }

    private NotaFiscal processar(Pedido pedido) {
        double valorTotalItens = calculadorTotalPedido.calcular(pedido);
        ResultadoCalculoTributario resultadoTributario =
                calculadorTributosPedido.calcular(pedido, valorTotalItens);
        ResultadoCalculoFrete resultadoFrete = calculadorFretePedido.calcular(pedido);
        NotaFiscal notaFiscal = notaFiscalFactory.criar(
                pedido,
                valorTotalItens,
                resultadoTributario,
                resultadoFrete);
        orquestradorIntegracoes.executar(pedido.getIdPedido(), notaFiscal);
        return notaFiscal;
    }

    private long duracaoEmMilissegundos(long inicioProcessamento) {
        return (System.nanoTime() - inicioProcessamento) / 1_000_000;
    }
}
