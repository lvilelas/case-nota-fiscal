package br.com.itau.geradornotafiscal.application.port.out;

import br.com.itau.geradornotafiscal.application.event.RegistroOutbox;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

public interface OutboxNotaFiscalPort {
    List<RegistroOutbox> reservarPendentes(int limite, Duration duracaoBloqueio);

    boolean renovarReserva(String eventId, String tokenReserva, Duration duracaoBloqueio);

    boolean marcarPublicado(String eventId, String tokenReserva);

    boolean reagendar(String eventId, String tokenReserva, OffsetDateTime proximaTentativa, String motivo);

    boolean marcarFalhaDefinitiva(String eventId, String tokenReserva, String motivo);
}
