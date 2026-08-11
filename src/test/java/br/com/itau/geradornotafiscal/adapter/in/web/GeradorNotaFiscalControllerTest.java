package br.com.itau.geradornotafiscal.adapter.in.web;

import br.com.itau.geradornotafiscal.application.port.in.GerarNotaFiscalUseCase;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import br.com.itau.geradornotafiscal.domain.model.Pedido;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GeradorNotaFiscalControllerTest {
    @Test
    void deveRetornarNotaFiscalGeradaComStatusOk() {
        GerarNotaFiscalUseCase useCase = mock(GerarNotaFiscalUseCase.class);
        GeradorNotaFiscalController controller = new GeradorNotaFiscalController(useCase);
        Pedido pedido = new Pedido();
        NotaFiscal notaFiscal = new NotaFiscal();
        when(useCase.gerarNotaFiscal(pedido)).thenReturn(notaFiscal);

        ResponseEntity<NotaFiscal> resposta = controller.gerarNotaFiscal(pedido);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertSame(notaFiscal, resposta.getBody());
        verify(useCase).gerarNotaFiscal(pedido);
    }
}
