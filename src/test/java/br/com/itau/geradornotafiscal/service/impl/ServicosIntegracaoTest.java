package br.com.itau.geradornotafiscal.service.impl;

import br.com.itau.geradornotafiscal.model.NotaFiscal;
import br.com.itau.geradornotafiscal.port.out.EntregaIntegrationPort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ServicosIntegracaoTest {

    private final NotaFiscal notaFiscal = new NotaFiscal();

    @Test
    void estoqueDeveConcluirEnvio() {
        EstoqueService service = new EstoqueService();

        assertDoesNotThrow(() -> service.enviarNotaFiscalParaBaixaEstoque(notaFiscal));
    }

    @Test
    void estoqueDevePropagarInterrupcao() {
        EstoqueService service = new EstoqueService();

        assertInterrupted(() -> service.enviarNotaFiscalParaBaixaEstoque(notaFiscal));
    }

    @Test
    void registroDeveConcluirEnvio() {
        RegistroService service = new RegistroService();

        assertDoesNotThrow(() -> service.registrarNotaFiscal(notaFiscal));
    }

    @Test
    void registroDevePropagarInterrupcao() {
        RegistroService service = new RegistroService();

        assertInterrupted(() -> service.registrarNotaFiscal(notaFiscal));
    }

    @Test
    void financeiroDeveConcluirEnvio() {
        FinanceiroService service = new FinanceiroService();

        assertDoesNotThrow(() -> service.enviarNotaFiscalParaContasReceber(notaFiscal));
    }

    @Test
    void financeiroDevePropagarInterrupcao() {
        FinanceiroService service = new FinanceiroService();

        assertInterrupted(() -> service.enviarNotaFiscalParaContasReceber(notaFiscal));
    }

    @Test
    void entregaDeveAguardarEDelegarAgendamento() {
        EntregaIntegrationPort integrationPort = mock(EntregaIntegrationPort.class);
        EntregaService service = new EntregaService(integrationPort);

        assertDoesNotThrow(() -> service.agendarEntrega(notaFiscal));

        verify(integrationPort).criarAgendamentoEntrega(notaFiscal);
    }

    @Test
    void entregaDevePropagarInterrupcaoSemDelegarAgendamento() {
        EntregaIntegrationPort integrationPort = mock(EntregaIntegrationPort.class);
        EntregaService service = new EntregaService(integrationPort);

        assertInterrupted(() -> service.agendarEntrega(notaFiscal));
    }

    private void assertInterrupted(Runnable operacao) {
        Thread.currentThread().interrupt();

        RuntimeException exception = assertThrows(RuntimeException.class, operacao::run);

        assertInstanceOf(InterruptedException.class, exception.getCause());
    }
}
