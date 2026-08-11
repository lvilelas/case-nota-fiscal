package br.com.itau.geradornotafiscal.service.impl;

import br.com.itau.geradornotafiscal.model.Destinatario;
import br.com.itau.geradornotafiscal.model.Endereco;
import br.com.itau.geradornotafiscal.model.Finalidade;
import br.com.itau.geradornotafiscal.model.Item;
import br.com.itau.geradornotafiscal.model.NotaFiscal;
import br.com.itau.geradornotafiscal.model.Pedido;
import br.com.itau.geradornotafiscal.model.Regiao;
import br.com.itau.geradornotafiscal.model.RegimeTributacaoPJ;
import br.com.itau.geradornotafiscal.model.TipoPessoa;
import br.com.itau.geradornotafiscal.service.CalculadoraAliquotaProduto;
import br.com.itau.geradornotafiscal.service.frete.CalculadorFrete;
import br.com.itau.geradornotafiscal.service.frete.tabela.CatalogoFreteRegional;
import br.com.itau.geradornotafiscal.service.tributacao.exception.RegimeTributacaoNaoSuportadoException;
import br.com.itau.geradornotafiscal.service.tributacao.exception.TipoPessoaNaoSuportadoException;
import br.com.itau.geradornotafiscal.service.tributacao.faixa.CalculadorTributacaoPorFaixa;
import br.com.itau.geradornotafiscal.service.tributacao.tabela.CatalogoTributario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GeradorNotaFiscalServiceImplTest {

    @Mock
    private EstoqueService estoqueService;
    @Mock
    private RegistroService registroService;
    @Mock
    private EntregaService entregaService;
    @Mock
    private FinanceiroService financeiroService;

    private GeradorNotaFiscalServiceImpl geradorNotaFiscalService;

    @BeforeEach
    void setup() {
        CalculadoraAliquotaProduto calculadoraAliquotaProduto = new CalculadoraAliquotaProduto();
        CalculadorTributacaoPorFaixa calculadorTributacao =
                new CalculadorTributacaoPorFaixa(calculadoraAliquotaProduto);

        geradorNotaFiscalService = new GeradorNotaFiscalServiceImpl(
                new CatalogoTributario(),
                calculadorTributacao,
                new CalculadorFrete(new CatalogoFreteRegional()),
                estoqueService,
                registroService,
                entregaService,
                financeiroService);
    }

    @Test
    void deveGerarNotaFiscalParaPessoaFisicaComTotalMenorQueQuinhentos() {
        Pedido pedido = pedido(
                TipoPessoa.FISICA,
                null,
                400,
                List.of(item("item-1", 100, 4)));

        NotaFiscal notaFiscal = geradorNotaFiscalService.gerarNotaFiscal(pedido);

        assertEquals(400, notaFiscal.getValorTotalItens());
        assertEquals(1, notaFiscal.getItens().size());
        assertEquals(4, notaFiscal.getItens().get(0).getQuantidade());
        assertEquals(0, notaFiscal.getItens().get(0).getValorTributoItem());
    }

    @Test
    void deveGerarNotaFiscalParaLucroPresumidoComTotalMaiorQueCincoMil() {
        Pedido pedido = pedido(
                TipoPessoa.JURIDICA,
                RegimeTributacaoPJ.LUCRO_PRESUMIDO,
                6000,
                List.of(item("item-1", 1000, 6)));

        NotaFiscal notaFiscal = geradorNotaFiscalService.gerarNotaFiscal(pedido);

        assertEquals(6000, notaFiscal.getValorTotalItens());
        assertEquals(1, notaFiscal.getItens().size());
        assertEquals(6, notaFiscal.getItens().get(0).getQuantidade());
        assertEquals(200, notaFiscal.getItens().get(0).getValorTributoItem(), 0.001);
    }

    @Test
    void deveRecalcularValorTotalAPartirDosItens() {
        Pedido pedido = pedido(
                TipoPessoa.FISICA,
                null,
                9999,
                List.of(
                        item("item-1", 25, 2),
                        item("item-2", 10, 3)));

        NotaFiscal notaFiscal = geradorNotaFiscalService.gerarNotaFiscal(pedido);

        assertEquals(80, notaFiscal.getValorTotalItens());
        assertEquals(2, notaFiscal.getItens().size());
        assertEquals(2, notaFiscal.getItens().get(0).getQuantidade());
        assertEquals(3, notaFiscal.getItens().get(1).getQuantidade());
    }

    @Test
    void naoDeveMisturarItensDeExecucoesDiferentes() {
        NotaFiscal primeiraNota = geradorNotaFiscalService.gerarNotaFiscal(pedido(
                TipoPessoa.FISICA,
                null,
                100,
                List.of(item("primeiro", 100, 1))));

        NotaFiscal segundaNota = geradorNotaFiscalService.gerarNotaFiscal(pedido(
                TipoPessoa.FISICA,
                null,
                200,
                List.of(item("segundo", 200, 1))));

        assertEquals(1, primeiraNota.getItens().size());
        assertEquals("primeiro", primeiraNota.getItens().get(0).getIdItem());
        assertEquals(1, segundaNota.getItens().size());
        assertEquals("segundo", segundaNota.getItens().get(0).getIdItem());
    }

    @ParameterizedTest(name = "PF com total {0} deve aplicar aliquota {1}")
    @CsvSource({
            "499.99, 0.00",
            "500.00, 0.12",
            "2000.00, 0.12",
            "2000.01, 0.15",
            "3500.00, 0.15",
            "3500.01, 0.17"
    })
    void deveAplicarAliquotaCorretaParaPessoaFisica(double total, double aliquota) {
        NotaFiscal notaFiscal = geradorNotaFiscalService.gerarNotaFiscal(pedido(
                TipoPessoa.FISICA,
                null,
                0,
                List.of(item("item", total, 1))));

        assertEquals(total, notaFiscal.getValorTotalItens(), 0.001);
        assertEquals(total * aliquota, notaFiscal.getItens().get(0).getValorTributoItem(), 0.001);
    }

    @ParameterizedTest(name = "{0} com total {1} deve aplicar aliquota {2}")
    @CsvSource({
            "SIMPLES_NACIONAL, 999.99, 0.03",
            "SIMPLES_NACIONAL, 1000.00, 0.07",
            "SIMPLES_NACIONAL, 2000.00, 0.07",
            "SIMPLES_NACIONAL, 2000.01, 0.13",
            "SIMPLES_NACIONAL, 5000.00, 0.13",
            "SIMPLES_NACIONAL, 5000.01, 0.19",
            "LUCRO_REAL, 999.99, 0.03",
            "LUCRO_REAL, 1000.00, 0.09",
            "LUCRO_REAL, 2000.00, 0.09",
            "LUCRO_REAL, 2000.01, 0.15",
            "LUCRO_REAL, 5000.00, 0.15",
            "LUCRO_REAL, 5000.01, 0.20",
            "LUCRO_PRESUMIDO, 999.99, 0.03",
            "LUCRO_PRESUMIDO, 1000.00, 0.09",
            "LUCRO_PRESUMIDO, 2000.00, 0.09",
            "LUCRO_PRESUMIDO, 2000.01, 0.16",
            "LUCRO_PRESUMIDO, 5000.00, 0.16",
            "LUCRO_PRESUMIDO, 5000.01, 0.20"
    })
    void deveAplicarAliquotaCorretaParaPessoaJuridica(
            RegimeTributacaoPJ regimeTributacao,
            double total,
            double aliquota) {
        NotaFiscal notaFiscal = geradorNotaFiscalService.gerarNotaFiscal(pedido(
                TipoPessoa.JURIDICA,
                regimeTributacao,
                0,
                List.of(item("item", total, 1))));

        assertEquals(total, notaFiscal.getValorTotalItens(), 0.001);
        assertEquals(total * aliquota, notaFiscal.getItens().get(0).getValorTributoItem(), 0.001);
    }

    @ParameterizedTest(name = "Frete para {0} deve usar multiplicador {1}")
    @CsvSource({
            "NORTE, 1.08",
            "NORDESTE, 1.085",
            "CENTRO_OESTE, 1.07",
            "SUDESTE, 1.048",
            "SUL, 1.06"
    })
    void deveCalcularFreteConformeRegiao(Regiao regiao, double multiplicador) {
        Pedido pedido = pedido(
                TipoPessoa.FISICA,
                null,
                0,
                List.of(item("item", 100, 1)),
                Finalidade.COBRANCA_ENTREGA,
                regiao);

        NotaFiscal notaFiscal = geradorNotaFiscalService.gerarNotaFiscal(pedido);

        assertEquals(100 * multiplicador, notaFiscal.getValorFrete(), 0.001);
    }

    @Test
    void deveManterFreteZeradoQuandoNaoHaEnderecoDeEntrega() {
        Pedido pedido = pedido(
                TipoPessoa.FISICA,
                null,
                0,
                List.of(item("item", 100, 1)),
                Finalidade.COBRANCA,
                Regiao.NORTE);

        NotaFiscal notaFiscal = geradorNotaFiscalService.gerarNotaFiscal(pedido);

        assertEquals(0, notaFiscal.getValorFrete());
    }

    @Test
    void deveLancarExcecaoParaRegimeTributarioNaoMapeado() {
        Pedido pedido = pedido(
                TipoPessoa.JURIDICA,
                RegimeTributacaoPJ.OUTROS,
                0,
                List.of(item("item", 100, 1)));

        RegimeTributacaoNaoSuportadoException exception = assertThrows(
                RegimeTributacaoNaoSuportadoException.class,
                () -> geradorNotaFiscalService.gerarNotaFiscal(pedido));

        assertEquals("Regime tributario nao suportado: OUTROS", exception.getMessage());
        verifyNoInteractions(estoqueService, registroService, entregaService, financeiroService);
    }

    @Test
    void deveLancarExcecaoParaRegimeTributarioNaoInformado() {
        Pedido pedido = pedido(
                TipoPessoa.JURIDICA,
                null,
                0,
                List.of(item("item", 100, 1)));

        RegimeTributacaoNaoSuportadoException exception = assertThrows(
                RegimeTributacaoNaoSuportadoException.class,
                () -> geradorNotaFiscalService.gerarNotaFiscal(pedido));

        assertEquals("Regime tributario nao suportado: null", exception.getMessage());
        verifyNoInteractions(estoqueService, registroService, entregaService, financeiroService);
    }

    @Test
    void deveLancarExcecaoParaTipoPessoaNaoInformado() {
        Pedido pedido = pedido(
                null,
                null,
                0,
                List.of(item("item", 100, 1)));

        TipoPessoaNaoSuportadoException exception = assertThrows(
                TipoPessoaNaoSuportadoException.class,
                () -> geradorNotaFiscalService.gerarNotaFiscal(pedido));

        assertEquals("Tipo de pessoa nao suportado: null", exception.getMessage());
        verifyNoInteractions(estoqueService, registroService, entregaService, financeiroService);
    }

    private Pedido pedido(
            TipoPessoa tipoPessoa,
            RegimeTributacaoPJ regimeTributacao,
            double valorTotalInformado,
            List<Item> itens) {
        return pedido(
                tipoPessoa,
                regimeTributacao,
                valorTotalInformado,
                itens,
                Finalidade.ENTREGA,
                Regiao.SUDESTE);
    }

    private Pedido pedido(
            TipoPessoa tipoPessoa,
            RegimeTributacaoPJ regimeTributacao,
            double valorTotalInformado,
            List<Item> itens,
            Finalidade finalidade,
            Regiao regiao) {
        Endereco endereco = Endereco.builder()
                .finalidade(finalidade)
                .regiao(regiao)
                .build();

        Destinatario destinatario = Destinatario.builder()
                .tipoPessoa(tipoPessoa)
                .regimeTributacao(regimeTributacao)
                .enderecos(List.of(endereco))
                .build();

        return Pedido.builder()
                .valorTotalItens(valorTotalInformado)
                .valorFrete(100)
                .itens(itens)
                .destinatario(destinatario)
                .build();
    }

    private Item item(String id, double valorUnitario, int quantidade) {
        Item item = new Item();
        item.setIdItem(id);
        item.setDescricao(id);
        item.setValorUnitario(valorUnitario);
        item.setQuantidade(quantidade);
        return item;
    }
}
