package br.com.itau.geradornotafiscal.adapter.out.integration;

import br.com.itau.geradornotafiscal.domain.model.ItemNotaFiscal;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegrationAdaptersTest {
    private final NotaFiscal notaFiscal = new NotaFiscal();

    @Test
    void estoqueDeveConcluirEnvio() {
        assertDoesNotThrow(() -> new EstoqueIntegrationAdapter().baixarEstoque(notaFiscal));
    }

    @Test
    void estoqueDevePropagarInterrupcao() {
        assertInterrupted(() -> new EstoqueIntegrationAdapter().baixarEstoque(notaFiscal));
    }

    @Test
    void registroDeveConcluirEnvio() {
        assertDoesNotThrow(() -> new RegistroIntegrationAdapter().registrar(notaFiscal));
    }

    @Test
    void registroDevePropagarInterrupcao() {
        assertInterrupted(() -> new RegistroIntegrationAdapter().registrar(notaFiscal));
    }

    @Test
    void financeiroDeveConcluirEnvio() {
        assertDoesNotThrow(() -> new FinanceiroIntegrationAdapter().enviar(notaFiscal));
    }

    @Test
    void financeiroDevePropagarInterrupcao() {
        assertInterrupted(() -> new FinanceiroIntegrationAdapter().enviar(notaFiscal));
    }

    @Test
    void entregaDeveAgendarNotaComAteCincoItens() {
        assertDoesNotThrow(() -> new EntregaIntegrationAdapter().agendar(notaFiscalComItens(5)));
    }

    @Test
    void entregaDeveAgendarNotaComMaisDeCincoItens() {
        assertDoesNotThrow(() -> new EntregaIntegrationAdapter().agendar(notaFiscalComItens(6)));
    }

    @Test
    void entregaDevePropagarInterrupcao() {
        assertInterrupted(() -> new EntregaIntegrationAdapter().agendar(notaFiscalComItens(6)));
    }

    private NotaFiscal notaFiscalComItens(int quantidade) {
        return NotaFiscal.builder()
                .itens(Collections.nCopies(quantidade, new ItemNotaFiscal()))
                .build();
    }

    private void assertInterrupted(Runnable operacao) {
        try {
            Thread.currentThread().interrupt();

            RuntimeException exception = assertThrows(RuntimeException.class, operacao::run);

            assertInstanceOf(InterruptedException.class, exception.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
