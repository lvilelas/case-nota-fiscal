package br.com.itau.geradornotafiscal.adapter.out.persistence.postgresql;

import br.com.itau.geradornotafiscal.application.event.NotaFiscalGeradaEvent;
import br.com.itau.geradornotafiscal.application.event.RegistroOutbox;
import br.com.itau.geradornotafiscal.application.exception.PersistenciaNotaFiscalException;
import br.com.itau.geradornotafiscal.application.port.out.NotaFiscalIdempotenciaPort;
import br.com.itau.geradornotafiscal.application.port.out.OutboxNotaFiscalPort;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class PostgresNotaFiscalAdapter implements NotaFiscalIdempotenciaPort, OutboxNotaFiscalPort {
    private static final int TAMANHO_MAXIMO_ERRO = 1000;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public PostgresNotaFiscalAdapter(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public Optional<NotaFiscal> buscarPorPedidoId(int pedidoId) {
        try {
            return buscarSemTratamento(pedidoId);
        } catch (RuntimeException exception) {
            throw traduzir("Falha ao buscar nota fiscal idempotente", exception);
        }
    }

    @Override
    public NotaFiscal salvarSeAusente(NotaFiscalGeradaEvent evento) {
        try {
            return Objects.requireNonNull(transactionTemplate.execute(status -> salvarNaTransacao(evento)));
        } catch (RuntimeException exception) {
            throw traduzir("Falha ao persistir nota fiscal e evento de outbox", exception);
        }
    }

    @Override
    public List<RegistroOutbox> reservarPendentes(int limite, Duration duracaoBloqueio) {
        try {
            return Objects.requireNonNull(transactionTemplate.execute(status -> jdbcTemplate.query("""
                    WITH candidatos AS (
                        SELECT event_id
                        FROM outbox_evento
                        WHERE (status = 'PENDENTE' AND proxima_tentativa_em <= CURRENT_TIMESTAMP)
                           OR (status = 'PROCESSANDO' AND bloqueado_ate < CURRENT_TIMESTAMP)
                        ORDER BY criado_em
                        FOR UPDATE SKIP LOCKED
                        LIMIT ?
                    )
                    UPDATE outbox_evento AS outbox
                    SET status = 'PROCESSANDO',
                        bloqueado_ate = CURRENT_TIMESTAMP + (? * INTERVAL '1 millisecond')
                    FROM candidatos
                    WHERE outbox.event_id = candidatos.event_id
                    RETURNING outbox.payload, outbox.tentativas
                    """, (resultSet, rowNum) -> new RegistroOutbox(
                    desserializar(resultSet.getString("payload"), NotaFiscalGeradaEvent.class),
                    resultSet.getInt("tentativas")),
                    limite,
                    duracaoBloqueio.toMillis())));
        } catch (RuntimeException exception) {
            throw traduzir("Falha ao reservar eventos da outbox", exception);
        }
    }

    @Override
    public void marcarPublicado(String eventId) {
        executarAtualizacao(
                """
                UPDATE outbox_evento
                SET status = 'PUBLICADO', publicado_em = CURRENT_TIMESTAMP, bloqueado_ate = NULL, ultimo_erro = NULL
                WHERE event_id = ?
                """,
                "Falha ao confirmar publicação da outbox",
                UUID.fromString(eventId));
    }

    @Override
    public void reagendar(String eventId, OffsetDateTime proximaTentativa, String motivo) {
        executarAtualizacao(
                """
                UPDATE outbox_evento
                SET status = 'PENDENTE', tentativas = tentativas + 1, proxima_tentativa_em = ?,
                    bloqueado_ate = NULL, ultimo_erro = ?
                WHERE event_id = ?
                """,
                "Falha ao reagendar evento da outbox",
                proximaTentativa,
                limitar(motivo),
                UUID.fromString(eventId));
    }

    @Override
    public void marcarFalhaDefinitiva(String eventId, String motivo) {
        executarAtualizacao(
                """
                UPDATE outbox_evento
                SET status = 'FALHA', tentativas = tentativas + 1, bloqueado_ate = NULL, ultimo_erro = ?
                WHERE event_id = ?
                """,
                "Falha ao marcar erro definitivo da outbox",
                limitar(motivo),
                UUID.fromString(eventId));
    }

    private NotaFiscal salvarNaTransacao(NotaFiscalGeradaEvent evento) {
        int inseridos = jdbcTemplate.update("""
                INSERT INTO nota_fiscal_processada (pedido_id, nota_fiscal_id, resposta)
                VALUES (?, ?, ?::jsonb)
                ON CONFLICT (pedido_id) DO NOTHING
                """,
                evento.pedidoId(),
                UUID.fromString(evento.notaFiscal().getIdNotaFiscal()),
                serializar(evento.notaFiscal()));

        if (inseridos == 0) {
            return buscarSemTratamento(evento.pedidoId())
                    .orElseThrow(() -> new IllegalStateException("Nota concorrente não encontrada após conflito"));
        }

        jdbcTemplate.update("""
                INSERT INTO outbox_evento (
                    event_id, aggregate_id, event_type, event_version, idempotency_key, payload)
                VALUES (?, ?, ?, ?, ?, ?::jsonb)
                """,
                UUID.fromString(evento.eventId()),
                String.valueOf(evento.pedidoId()),
                evento.eventType(),
                evento.eventVersion(),
                evento.idempotencyKey(),
                serializar(evento));
        return evento.notaFiscal();
    }

    private Optional<NotaFiscal> buscarSemTratamento(int pedidoId) {
        return jdbcTemplate.query(
                        "SELECT resposta FROM nota_fiscal_processada WHERE pedido_id = ?",
                        (resultSet, rowNum) -> desserializar(resultSet.getString("resposta"), NotaFiscal.class),
                        pedidoId)
                .stream()
                .findFirst();
    }

    private void executarAtualizacao(String sql, String mensagem, Object... parametros) {
        try {
            jdbcTemplate.update(sql, parametros);
        } catch (RuntimeException exception) {
            throw traduzir(mensagem, exception);
        }
    }

    private String serializar(Object objeto) {
        try {
            return objectMapper.writeValueAsString(objeto);
        } catch (Exception exception) {
            throw new PersistenciaNotaFiscalException("Falha ao serializar dado persistente", exception);
        }
    }

    private <T> T desserializar(String json, Class<T> tipo) {
        try {
            return objectMapper.readValue(json, tipo);
        } catch (Exception exception) {
            throw new PersistenciaNotaFiscalException("Falha ao desserializar dado persistente", exception);
        }
    }

    private String limitar(String motivo) {
        return motivo.length() <= TAMANHO_MAXIMO_ERRO
                ? motivo
                : motivo.substring(0, TAMANHO_MAXIMO_ERRO);
    }

    private PersistenciaNotaFiscalException traduzir(String mensagem, RuntimeException exception) {
        if (exception instanceof PersistenciaNotaFiscalException persistenciaException) {
            return persistenciaException;
        }
        return new PersistenciaNotaFiscalException(mensagem, exception);
    }
}
