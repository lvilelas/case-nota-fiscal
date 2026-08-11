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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        geradorNotaFiscalService = new GeradorNotaFiscalServiceImpl(
                new CalculadoraAliquotaProduto(),
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

    private Pedido pedido(
            TipoPessoa tipoPessoa,
            RegimeTributacaoPJ regimeTributacao,
            double valorTotalInformado,
            List<Item> itens) {
        Endereco endereco = Endereco.builder()
                .finalidade(Finalidade.ENTREGA)
                .regiao(Regiao.SUDESTE)
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
