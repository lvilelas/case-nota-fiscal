package br.com.itau.geradornotafiscal.adapter.in.web;

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
        GeradorNotaFiscalController controller = new GeradorNotaFiscalController(useCase);
        Pedido pedido = Pedido.builder().idPedido(123).itens(List.of()).build();
        NotaFiscal notaFiscal = NotaFiscal.builder()
                .idNotaFiscal("nf-123")
                .itens(List.of())
                .build();
        when(useCase.gerarNotaFiscal(pedido)).thenReturn(notaFiscal);

        ResponseEntity<NotaFiscal> resposta = controller.gerarNotaFiscal(pedido);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertSame(notaFiscal, resposta.getBody());
        verify(useCase).gerarNotaFiscal(pedido);
    }

    @Test
    void devePropagarFalhaDoCasoDeUso() {
        GerarNotaFiscalUseCase useCase = mock(GerarNotaFiscalUseCase.class);
        GeradorNotaFiscalController controller = new GeradorNotaFiscalController(useCase);
        Pedido pedido = Pedido.builder().idPedido(123).itens(List.of()).build();
        RuntimeException falha = new RuntimeException("falha simulada");
        when(useCase.gerarNotaFiscal(pedido)).thenThrow(falha);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> controller.gerarNotaFiscal(pedido));

        assertSame(falha, exception);
        verify(useCase).gerarNotaFiscal(pedido);
    }
}
