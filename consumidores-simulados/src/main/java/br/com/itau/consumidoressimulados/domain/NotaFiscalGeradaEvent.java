package br.com.itau.consumidoressimulados.domain;

import java.time.OffsetDateTime;

public record NotaFiscalGeradaEvent(
        String eventId,
        String eventType,
        int eventVersion,
        OffsetDateTime occurredAt,
        String correlationId,
        String flowId,
        String idempotencyKey,
        int pedidoId,
        NotaFiscalPayload notaFiscal) {
}
