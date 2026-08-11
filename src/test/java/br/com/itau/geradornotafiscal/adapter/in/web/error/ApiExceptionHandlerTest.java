package br.com.itau.geradornotafiscal.adapter.in.web.error;

import br.com.itau.geradornotafiscal.application.exception.PublicacaoEventoException;
import br.com.itau.geradornotafiscal.domain.exception.RegimeTributacaoNaoSuportadoException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();
    private HttpServletRequest request;

    @BeforeEach
    void configurarContexto() {
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/gerar-nota-fiscal");
        MDC.put("correlationId", "correlation-test");
        MDC.put("flowId", "flow-test");
    }

    @AfterEach
    void limparContexto() {
        MDC.clear();
    }

    @Test
    void deveResponderBadRequestComCamposInvalidos() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "pedidoRequest");
        bindingResult.addError(new FieldError("pedidoRequest", "idPedido", "deve ser maior que zero"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        var response = handler.tratarValidacao(exception, request);

        assertError(response.getBody(), HttpStatus.BAD_REQUEST, "PAYLOAD_INVALIDO");
        assertEquals("id_pedido", response.getBody().camposInvalidos().getFirst().campo());
        assertEquals("deve ser maior que zero", response.getBody().camposInvalidos().getFirst().mensagem());
    }

    @Test
    void deveResponderBadRequestParaJsonIlegivel() {
        var response = handler.tratarMensagemIlegivel(
                new HttpMessageNotReadableException("json invalido", new MockHttpInputMessage(new byte[0])),
                request);

        assertError(response.getBody(), HttpStatus.BAD_REQUEST, "PAYLOAD_ILEGIVEL");
        assertTrue(response.getBody().camposInvalidos().isEmpty());
    }

    @Test
    void deveResponderUnprocessableEntityParaRegraNaoSuportada() {
        var exception = new RegimeTributacaoNaoSuportadoException(null);

        var response = handler.tratarRegraNegocio(exception, request);

        assertError(response.getBody(), HttpStatus.UNPROCESSABLE_ENTITY, "REGRA_NEGOCIO_INVALIDA");
        assertEquals(exception.getMessage(), response.getBody().mensagem());
    }

    @Test
    void deveResponderServiceUnavailableParaFalhaNaPublicacao() {
        var exception = new PublicacaoEventoException("falha", new RuntimeException());

        var response = handler.tratarIndisponibilidadeMensageria(exception, request);

        assertError(response.getBody(), HttpStatus.SERVICE_UNAVAILABLE, "MENSAGERIA_INDISPONIVEL");
    }

    @Test
    void deveResponderInternalServerErrorSemExporDetalhes() {
        var response = handler.tratarErroInesperado(new RuntimeException("detalhe sensivel"), request);

        assertError(response.getBody(), HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO");
        assertEquals("Ocorreu um erro inesperado", response.getBody().mensagem());
    }

    private void assertError(ApiError error, HttpStatus status, String codigo) {
        assertNotNull(error);
        assertNotNull(error.timestamp());
        assertEquals(status.value(), error.status());
        assertEquals(codigo, error.codigo());
        assertEquals("/api/gerar-nota-fiscal", error.path());
        assertEquals("correlation-test", error.correlationId());
        assertEquals("flow-test", error.flowId());
    }
}
