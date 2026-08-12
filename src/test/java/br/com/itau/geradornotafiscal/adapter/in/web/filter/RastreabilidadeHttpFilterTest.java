package br.com.itau.geradornotafiscal.adapter.in.web.filter;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RastreabilidadeHttpFilterTest {
    private final RastreabilidadeHttpFilter filter = new RastreabilidadeHttpFilter();

    @AfterEach
    void limparMdc() {
        MDC.clear();
    }

    @Test
    void deveGerarIdentificadoresQuandoCorrelationIdNaoForInformado() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> correlationIdDuranteFluxo = new AtomicReference<>();
        AtomicReference<String> flowIdDuranteFluxo = new AtomicReference<>();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
            correlationIdDuranteFluxo.set(MDC.get(RastreabilidadeHttpFilter.CORRELATION_ID_MDC));
            flowIdDuranteFluxo.set(MDC.get(RastreabilidadeHttpFilter.FLOW_ID_MDC));
        });

        String correlationId = response.getHeader(RastreabilidadeHttpFilter.CORRELATION_ID_HEADER);
        String flowId = response.getHeader(RastreabilidadeHttpFilter.FLOW_ID_HEADER);
        assertDoesNotThrow(() -> UUID.fromString(correlationId));
        assertDoesNotThrow(() -> UUID.fromString(flowId));
        assertEquals(correlationId, correlationIdDuranteFluxo.get());
        assertEquals(flowId, flowIdDuranteFluxo.get());
        assertNull(MDC.get(RastreabilidadeHttpFilter.CORRELATION_ID_MDC));
        assertNull(MDC.get(RastreabilidadeHttpFilter.FLOW_ID_MDC));
    }

    @Test
    void deveReutilizarCorrelationIdEFlowIdValidos() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(RastreabilidadeHttpFilter.CORRELATION_ID_HEADER, "sessao-entrevista-123");
        request.addHeader(RastreabilidadeHttpFilter.FLOW_ID_HEADER, "fluxo-pedido-456");

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
            assertEquals(
                    "sessao-entrevista-123",
                    MDC.get(RastreabilidadeHttpFilter.CORRELATION_ID_MDC));
            assertEquals("fluxo-pedido-456", MDC.get(RastreabilidadeHttpFilter.FLOW_ID_MDC));
        });

        assertEquals(
                "sessao-entrevista-123",
                response.getHeader(RastreabilidadeHttpFilter.CORRELATION_ID_HEADER));
        assertEquals("fluxo-pedido-456", response.getHeader(RastreabilidadeHttpFilter.FLOW_ID_HEADER));
    }

    @Test
    void deveSubstituirCorrelationIdInvalido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(RastreabilidadeHttpFilter.CORRELATION_ID_HEADER, "id com espacos");

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> { });

        String correlationId = response.getHeader(RastreabilidadeHttpFilter.CORRELATION_ID_HEADER);
        assertNotEquals("id com espacos", correlationId);
        assertDoesNotThrow(() -> UUID.fromString(correlationId));
    }

    @Test
    void deveSubstituirFlowIdInvalido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(RastreabilidadeHttpFilter.FLOW_ID_HEADER, "flow id invalido");

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> { });

        String flowId = response.getHeader(RastreabilidadeHttpFilter.FLOW_ID_HEADER);
        assertNotEquals("flow id invalido", flowId);
        assertDoesNotThrow(() -> UUID.fromString(flowId));
    }

    @Test
    void deveLimparMdcQuandoFluxoFalhar() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(
                ServletException.class,
                () -> filter.doFilter(
                        request,
                        response,
                        (ignoredRequest, ignoredResponse) -> {
                            throw new ServletException("falha simulada");
                        }));

        assertNull(MDC.get(RastreabilidadeHttpFilter.CORRELATION_ID_MDC));
        assertNull(MDC.get(RastreabilidadeHttpFilter.FLOW_ID_MDC));
    }
}
