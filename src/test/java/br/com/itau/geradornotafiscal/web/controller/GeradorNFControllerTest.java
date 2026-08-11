package br.com.itau.geradornotafiscal.web.controller;

import br.com.itau.geradornotafiscal.model.NotaFiscal;
import br.com.itau.geradornotafiscal.model.Pedido;
import br.com.itau.geradornotafiscal.service.GeradorNotaFiscalService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GeradorNFControllerTest {

    @Test
    void deveRetornarNotaFiscalGeradaComStatusOk() {
        GeradorNotaFiscalService service = mock(GeradorNotaFiscalService.class);
        GeradorNFController controller = new GeradorNFController();
        ReflectionTestUtils.setField(controller, "notaFiscalService", service);
        Pedido pedido = new Pedido();
        pedido.setIdPedido(123);
        NotaFiscal notaFiscal = new NotaFiscal();
        when(service.gerarNotaFiscal(pedido)).thenReturn(notaFiscal);

        ResponseEntity<NotaFiscal> resposta = controller.gerarNotaFiscal(pedido);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertSame(notaFiscal, resposta.getBody());
        verify(service).gerarNotaFiscal(pedido);
    }
}
