package br.com.itau.geradornotafiscal.adapter.in.web;

import br.com.itau.geradornotafiscal.adapter.in.web.dto.PedidoRequest;
import br.com.itau.geradornotafiscal.adapter.in.web.mapper.PedidoWebMapper;
import br.com.itau.geradornotafiscal.application.port.in.GerarNotaFiscalUseCase;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import br.com.itau.geradornotafiscal.domain.model.Pedido;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GeradorNotaFiscalControllerTest {
    @Test
    void deveRetornarNotaFiscalGeradaComStatusOk() {
        GerarNotaFiscalUseCase useCase = mock(GerarNotaFiscalUseCase.class);
        PedidoWebMapper mapper = mock(PedidoWebMapper.class);
        GeradorNotaFiscalController controller = new GeradorNotaFiscalController(useCase, mapper);
        Pedido pedido = Pedido.builder().idPedido(123).itens(List.of()).build();
        PedidoRequest request = new PedidoRequest(123, null, 0, 0, List.of(), null);
        NotaFiscal notaFiscal = NotaFiscal.builder()
                .idNotaFiscal("nf-123")
                .itens(List.of())
                .build();
        when(useCase.gerarNotaFiscal(pedido)).thenReturn(notaFiscal);
        when(mapper.paraDominio(request)).thenReturn(pedido);

        ResponseEntity<NotaFiscal> resposta = controller.gerarNotaFiscal(request);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertSame(notaFiscal, resposta.getBody());
        verify(useCase).gerarNotaFiscal(pedido);
        verify(mapper).paraDominio(request);
    }

    @Test
    void devePropagarFalhaDoCasoDeUso() {
        GerarNotaFiscalUseCase useCase = mock(GerarNotaFiscalUseCase.class);
        PedidoWebMapper mapper = mock(PedidoWebMapper.class);
        GeradorNotaFiscalController controller = new GeradorNotaFiscalController(useCase, mapper);
        Pedido pedido = Pedido.builder().idPedido(123).itens(List.of()).build();
        PedidoRequest request = new PedidoRequest(123, null, 0, 0, List.of(), null);
        RuntimeException falha = new RuntimeException("falha simulada");
        when(useCase.gerarNotaFiscal(pedido)).thenThrow(falha);
        when(mapper.paraDominio(request)).thenReturn(pedido);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> controller.gerarNotaFiscal(request));

        assertSame(falha, exception);
        verify(useCase).gerarNotaFiscal(pedido);
    }
}
