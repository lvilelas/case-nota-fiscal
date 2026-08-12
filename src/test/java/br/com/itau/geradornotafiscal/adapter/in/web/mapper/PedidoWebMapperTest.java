package br.com.itau.geradornotafiscal.adapter.in.web.mapper;

import br.com.itau.geradornotafiscal.adapter.in.web.dto.DestinatarioRequest;
import br.com.itau.geradornotafiscal.adapter.in.web.dto.DocumentoRequest;
import br.com.itau.geradornotafiscal.adapter.in.web.dto.EnderecoRequest;
import br.com.itau.geradornotafiscal.adapter.in.web.dto.ItemRequest;
import br.com.itau.geradornotafiscal.adapter.in.web.dto.PedidoRequest;
import br.com.itau.geradornotafiscal.domain.model.Finalidade;
import br.com.itau.geradornotafiscal.domain.model.Regiao;
import br.com.itau.geradornotafiscal.domain.model.RegimeTributacaoPJ;
import br.com.itau.geradornotafiscal.domain.model.TipoDocumento;
import br.com.itau.geradornotafiscal.domain.model.TipoPessoa;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PedidoWebMapperTest {
    @Test
    void deveConverterContratoHttpParaDominio() {
        PedidoRequest request = new PedidoRequest(
                42,
                LocalDate.of(2026, 8, 11),
                100,
                10,
                List.of(new ItemRequest("item-1", "Produto", 50, 2)),
                new DestinatarioRequest(
                        "Empresa",
                        TipoPessoa.JURIDICA,
                        RegimeTributacaoPJ.SIMPLES_NACIONAL,
                        List.of(new DocumentoRequest("123", TipoDocumento.CNPJ)),
                        List.of(new EnderecoRequest(
                                "01001000",
                                "Praca da Se",
                                "1",
                                "SP",
                                "lado impar",
                                Finalidade.ENTREGA,
                                Regiao.SUDESTE))));

        var pedido = new PedidoWebMapper().paraDominio(request);

        assertEquals(42, pedido.getIdPedido());
        assertEquals(LocalDate.of(2026, 8, 11), pedido.getData());
        assertEquals(100, pedido.getValorTotalItens());
        assertEquals(10, pedido.getValorFrete());
        assertEquals("item-1", pedido.getItens().getFirst().getIdItem());
        assertEquals("Produto", pedido.getItens().getFirst().getDescricao());
        assertEquals(50, pedido.getItens().getFirst().getValorUnitario());
        assertEquals(2, pedido.getItens().getFirst().getQuantidade());
        assertEquals("Empresa", pedido.getDestinatario().getNome());
        assertEquals(TipoPessoa.JURIDICA, pedido.getDestinatario().getTipoPessoa());
        assertEquals(RegimeTributacaoPJ.SIMPLES_NACIONAL, pedido.getDestinatario().getRegimeTributacao());
        assertEquals("123", pedido.getDestinatario().getDocumentos().getFirst().getNumero());
        assertEquals(TipoDocumento.CNPJ, pedido.getDestinatario().getDocumentos().getFirst().getTipo());
        assertEquals("01001000", pedido.getDestinatario().getEnderecos().getFirst().getCep());
        assertEquals("Praca da Se", pedido.getDestinatario().getEnderecos().getFirst().getLogradouro());
        assertEquals("1", pedido.getDestinatario().getEnderecos().getFirst().getNumero());
        assertEquals("SP", pedido.getDestinatario().getEnderecos().getFirst().getEstado());
        assertEquals("lado impar", pedido.getDestinatario().getEnderecos().getFirst().getComplemento());
        assertEquals(Finalidade.ENTREGA, pedido.getDestinatario().getEnderecos().getFirst().getFinalidade());
        assertEquals(Regiao.SUDESTE, pedido.getDestinatario().getEnderecos().getFirst().getRegiao());
    }
}
