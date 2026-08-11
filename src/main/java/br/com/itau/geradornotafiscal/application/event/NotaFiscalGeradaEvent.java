package br.com.itau.geradornotafiscal.application.event;

import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;

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
        NotaFiscal notaFiscal) {
}
