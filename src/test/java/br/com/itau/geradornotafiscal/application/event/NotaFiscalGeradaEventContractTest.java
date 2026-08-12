package br.com.itau.geradornotafiscal.application.event;

import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotaFiscalGeradaEventContractTest {
    @Test
    void deveManterContratoV1DoEventoEmSnakeCase() throws Exception {
        var objectMapper = JsonMapper.builder()
                .findAndAddModules()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();
        var evento = new NotaFiscalGeradaEvent(
                "00000000-0000-0000-0000-000000000001",
                "NotaFiscalGerada",
                1,
                OffsetDateTime.parse("2026-08-11T20:00:00Z"),
                "correlation-1",
                "flow-1",
                "nota-fiscal-gerada:pedido:42",
                42,
                NotaFiscal.builder()
                        .idNotaFiscal("00000000-0000-0000-0000-000000000002")
                        .valorTotalItens(100)
                        .valorFrete(10)
                        .itens(List.of())
                        .build());

        var json = objectMapper.readTree(objectMapper.writeValueAsString(evento));

        assertEquals(9, json.size());
        assertEquals("NotaFiscalGerada", json.get("event_type").asText());
        assertEquals(1, json.get("event_version").asInt());
        assertEquals(42, json.get("pedido_id").asInt());
        assertEquals("nota-fiscal-gerada:pedido:42", json.get("idempotency_key").asText());
        assertTrue(json.has("event_id"));
        assertTrue(json.has("occurred_at"));
        assertTrue(json.has("correlation_id"));
        assertTrue(json.has("flow_id"));
        assertTrue(json.has("nota_fiscal"));
        assertTrue(json.get("nota_fiscal").has("id_nota_fiscal"));
        assertFalse(json.has("eventType"));
        assertFalse(json.get("nota_fiscal").has("idNotaFiscal"));
    }
}
