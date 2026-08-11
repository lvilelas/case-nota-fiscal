package br.com.itau.geradornotafiscal.adapter.in.web.error;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String codigo,
        String mensagem,
        String path,
        String correlationId,
        String flowId,
        List<CampoInvalido> camposInvalidos) {
}
