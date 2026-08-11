package br.com.itau.geradornotafiscal.port.out;

import br.com.itau.geradornotafiscal.model.ItemNotaFiscal;
import br.com.itau.geradornotafiscal.model.NotaFiscal;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EntregaIntegrationPortTest {

    private final EntregaIntegrationPort integrationPort = new EntregaIntegrationPort();

    @Test
    void deveAgendarEntregaParaNotaComAteCincoItens() {
        NotaFiscal notaFiscal = notaFiscalComItens(5);

        assertDoesNotThrow(() -> integrationPort.criarAgendamentoEntrega(notaFiscal));
    }

    @Test
    void deveAgendarEntregaParaNotaComMaisDeCincoItens() {
        NotaFiscal notaFiscal = notaFiscalComItens(6);

        assertDoesNotThrow(() -> integrationPort.criarAgendamentoEntrega(notaFiscal));
    }

    @Test
    void devePropagarFalhaQuandoAgendamentoForInterrompidoParaMaisDeCincoItens() {
        NotaFiscal notaFiscal = notaFiscalComItens(6);
        Thread.currentThread().interrupt();

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> integrationPort.criarAgendamentoEntrega(notaFiscal));

        assertInstanceOf(InterruptedException.class, exception.getCause());
    }

    private NotaFiscal notaFiscalComItens(int quantidade) {
        List<ItemNotaFiscal> itens = Collections.nCopies(quantidade, new ItemNotaFiscal());
        return NotaFiscal.builder().itens(itens).build();
    }
}
