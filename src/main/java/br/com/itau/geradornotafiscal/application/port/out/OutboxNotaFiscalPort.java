package br.com.itau.geradornotafiscal.application.port.out;

import br.com.itau.geradornotafiscal.application.event.RegistroOutbox;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

public interface OutboxNotaFiscalPort {
    List<RegistroOutbox> reservarPendentes(int limite, Duration duracaoBloqueio);

    void marcarPublicado(String eventId);

    void reagendar(String eventId, OffsetDateTime proximaTentativa, String motivo);

    void marcarFalhaDefinitiva(String eventId, String motivo);
}
