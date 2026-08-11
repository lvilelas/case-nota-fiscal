package br.com.itau.geradornotafiscal.adapter.out.persistence.postgresql;

import br.com.itau.geradornotafiscal.application.event.NotaFiscalGeradaEvent;
import br.com.itau.geradornotafiscal.application.exception.PersistenciaNotaFiscalException;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostgresNotaFiscalAdapterFailureTest {
    private JdbcTemplate jdbcTemplate;
    private ObjectMapper objectMapper;
    private PostgresNotaFiscalAdapter adapter;

    @BeforeEach
    void configurar() {
        jdbcTemplate = mock(JdbcTemplate.class);
        objectMapper = mock(ObjectMapper.class);
        var transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        adapter = new PostgresNotaFiscalAdapter(jdbcTemplate, objectMapper, transactionManager);
    }

    @Test
    @SuppressWarnings("unchecked")
    void deveTraduzirFalhaAoBuscar() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenThrow(new RuntimeException("conexao encerrada"));

        assertThrows(PersistenciaNotaFiscalException.class, () -> adapter.buscarPorPedidoId(1));
    }

    @Test
    @SuppressWarnings("unchecked")
    void devePreservarExcecaoDeDesserializacaoAoReservar() throws Exception {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocacao -> {
                    RowMapper<?> mapper = invocacao.getArgument(1);
                    var resultSet = mock(java.sql.ResultSet.class);
                    when(resultSet.getString("payload")).thenReturn("json-invalido");
                    mapper.mapRow(resultSet, 0);
                    return List.of();
                });
        when(objectMapper.readValue(anyString(), any(Class.class)))
                .thenThrow(new RuntimeException("json invalido"));

        assertThrows(PersistenciaNotaFiscalException.class,
                () -> adapter.reservarPendentes(1, Duration.ofSeconds(30)));
    }

    @Test
    void devePreservarExcecaoDeSerializacaoAoSalvar() throws Exception {
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new RuntimeException("serializacao invalida"));

        assertThrows(PersistenciaNotaFiscalException.class,
                () -> adapter.salvarSeAusente(evento()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void deveTraduzirConflitoSemRegistroPersistido() {
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(0);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        assertThrows(PersistenciaNotaFiscalException.class,
                () -> adapter.salvarSeAusente(evento()));
    }

    @Test
    void deveTraduzirFalhaAoAtualizarOutbox() {
        when(jdbcTemplate.update(anyString(), any(Object[].class)))
                .thenThrow(new RuntimeException("banco indisponivel"));

        assertThrows(PersistenciaNotaFiscalException.class,
                () -> adapter.marcarPublicado("00000000-0000-0000-0000-000000000001"));
    }

    private NotaFiscalGeradaEvent evento() {
        return new NotaFiscalGeradaEvent(
                "00000000-0000-0000-0000-000000000001",
                "NotaFiscalGerada",
                1,
                OffsetDateTime.parse("2026-08-11T20:00:00Z"),
                "correlation-id",
                "flow-id",
                "nota-fiscal-gerada:pedido:1",
                1,
                NotaFiscal.builder()
                        .idNotaFiscal("00000000-0000-0000-0000-000000000002")
                        .itens(List.of())
                        .build());
    }
}
