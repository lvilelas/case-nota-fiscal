package br.com.itau.geradornotafiscal.adapter.out.persistence.postgresql;

import br.com.itau.geradornotafiscal.application.event.NotaFiscalGeradaEvent;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class PostgresNotaFiscalAdapterTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:16-alpine"));

    private JdbcTemplate jdbcTemplate;
    private PostgresNotaFiscalAdapter adapter;

    @BeforeEach
    void prepararBanco() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("TRUNCATE TABLE outbox_evento, nota_fiscal_processada");

        var objectMapper = JsonMapper.builder()
                .findAndAddModules()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();
        adapter = new PostgresNotaFiscalAdapter(
                jdbcTemplate,
                objectMapper,
                new DataSourceTransactionManager(dataSource));
    }

    @Test
    void devePersistirNotaEEventoAtomicamenteEReutilizarResposta() {
        var eventoOriginal = evento(10, "00000000-0000-0000-0000-000000000001", "00000000-0000-0000-0000-000000000011");
        var eventoDuplicado = evento(10, "00000000-0000-0000-0000-000000000002", "00000000-0000-0000-0000-000000000012");

        NotaFiscal primeiraResposta = adapter.salvarSeAusente(eventoOriginal);
        NotaFiscal segundaResposta = adapter.salvarSeAusente(eventoDuplicado);

        assertEquals(eventoOriginal.notaFiscal().getIdNotaFiscal(), primeiraResposta.getIdNotaFiscal());
        assertEquals(eventoOriginal.notaFiscal().getIdNotaFiscal(), segundaResposta.getIdNotaFiscal());
        assertEquals(eventoOriginal.notaFiscal().getIdNotaFiscal(),
                adapter.buscarPorPedidoId(10).orElseThrow().getIdNotaFiscal());
        assertEquals(1, contar("nota_fiscal_processada"));
        assertEquals(1, contar("outbox_evento"));
    }

    @Test
    void deveGarantirIdempotenciaEmRequisicoesConcorrentes() throws Exception {
        var eventoA = evento(20, "00000000-0000-0000-0000-000000000021", "00000000-0000-0000-0000-000000000031");
        var eventoB = evento(20, "00000000-0000-0000-0000-000000000022", "00000000-0000-0000-0000-000000000032");

        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Callable<NotaFiscal>> tarefas = List.of(
                    () -> adapter.salvarSeAusente(eventoA),
                    () -> adapter.salvarSeAusente(eventoB));
            var respostas = executor.invokeAll(tarefas).stream()
                    .map(resultado -> {
                        try {
                            return resultado.get().getIdNotaFiscal();
                        } catch (Exception exception) {
                            throw new AssertionError(exception);
                        }
                    })
                    .distinct()
                    .toList();

            assertEquals(1, respostas.size());
        }
        assertEquals(1, contar("nota_fiscal_processada"));
        assertEquals(1, contar("outbox_evento"));
    }

    @Test
    void deveReservarPublicarEImpedirReservaDuplicadaDuranteLock() {
        var evento = evento(30, "00000000-0000-0000-0000-000000000041", "00000000-0000-0000-0000-000000000051");
        adapter.salvarSeAusente(evento);

        var reservados = adapter.reservarPendentes(10, Duration.ofSeconds(30));

        assertEquals(1, reservados.size());
        assertEquals(evento.eventId(), reservados.getFirst().evento().eventId());
        assertTrue(adapter.reservarPendentes(10, Duration.ofSeconds(30)).isEmpty());

        adapter.marcarPublicado(evento.eventId());

        assertEquals("PUBLICADO", buscarStatus(evento.eventId()));
        assertTrue(adapter.reservarPendentes(10, Duration.ofSeconds(30)).isEmpty());
    }

    @Test
    void deveReagendarEventoERecuperarLockExpirado() {
        var evento = evento(40, "00000000-0000-0000-0000-000000000061", "00000000-0000-0000-0000-000000000071");
        adapter.salvarSeAusente(evento);
        adapter.reservarPendentes(1, Duration.ofSeconds(30));

        adapter.reagendar(evento.eventId(), OffsetDateTime.now().minusSeconds(1), "SNS indisponivel");
        var reagendado = adapter.reservarPendentes(1, Duration.ofMillis(1));
        assertEquals(1, reagendado.size());
        assertEquals(1, reagendado.getFirst().tentativas());

        jdbcTemplate.update(
                "UPDATE outbox_evento SET bloqueado_ate = CURRENT_TIMESTAMP - INTERVAL '1 second' WHERE event_id = ?",
                UUID.fromString(evento.eventId()));
        assertEquals(1, adapter.reservarPendentes(1, Duration.ofSeconds(30)).size());
    }

    @Test
    void deveMarcarFalhaDefinitivaELimitarMotivo() {
        var evento = evento(50, "00000000-0000-0000-0000-000000000081", "00000000-0000-0000-0000-000000000091");
        adapter.salvarSeAusente(evento);
        String motivoLongo = "x".repeat(1100);

        adapter.marcarFalhaDefinitiva(evento.eventId(), motivoLongo);

        assertEquals("FALHA", buscarStatus(evento.eventId()));
        assertEquals(1000, jdbcTemplate.queryForObject(
                "SELECT length(ultimo_erro) FROM outbox_evento WHERE event_id = ?",
                Integer.class,
                UUID.fromString(evento.eventId())));
        assertTrue(adapter.buscarPorPedidoId(999).isEmpty());
    }

    private int contar(String tabela) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + tabela, Integer.class);
    }

    private String buscarStatus(String eventId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM outbox_evento WHERE event_id = ?",
                String.class,
                UUID.fromString(eventId));
    }

    private NotaFiscalGeradaEvent evento(int pedidoId, String eventId, String notaFiscalId) {
        var notaFiscal = NotaFiscal.builder()
                .idNotaFiscal(notaFiscalId)
                .valorTotalItens(100)
                .valorFrete(10)
                .itens(List.of())
                .build();
        return new NotaFiscalGeradaEvent(
                eventId,
                "NotaFiscalGerada",
                1,
                OffsetDateTime.parse("2026-08-11T20:00:00Z"),
                "correlation-id",
                "flow-id",
                "nota-fiscal-gerada:pedido:" + pedidoId,
                pedidoId,
                notaFiscal);
    }
}
