package br.com.itau.geradornotafiscal.application.service.integration;

import br.com.itau.geradornotafiscal.application.port.out.AgendarEntregaPort;
import br.com.itau.geradornotafiscal.application.port.out.BaixarEstoquePort;
import br.com.itau.geradornotafiscal.application.port.out.EnviarNotaFiscalFinanceiroPort;
import br.com.itau.geradornotafiscal.application.port.out.RegistrarNotaFiscalPort;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrquestradorIntegracoesNotaFiscal {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrquestradorIntegracoesNotaFiscal.class);

    private final BaixarEstoquePort baixarEstoquePort;
    private final RegistrarNotaFiscalPort registrarNotaFiscalPort;
    private final AgendarEntregaPort agendarEntregaPort;
    private final EnviarNotaFiscalFinanceiroPort enviarNotaFiscalFinanceiroPort;

    public OrquestradorIntegracoesNotaFiscal(
            BaixarEstoquePort baixarEstoquePort,
            RegistrarNotaFiscalPort registrarNotaFiscalPort,
            AgendarEntregaPort agendarEntregaPort,
            EnviarNotaFiscalFinanceiroPort enviarNotaFiscalFinanceiroPort) {
        this.baixarEstoquePort = baixarEstoquePort;
        this.registrarNotaFiscalPort = registrarNotaFiscalPort;
        this.agendarEntregaPort = agendarEntregaPort;
        this.enviarNotaFiscalFinanceiroPort = enviarNotaFiscalFinanceiroPort;
    }

    public void executar(int pedidoId, NotaFiscal notaFiscal) {
        LOGGER.info(
                "Iniciando chamadas aos serviços externos: pedidoId={}, notaFiscalId={}",
                pedidoId,
                notaFiscal.getIdNotaFiscal());
        baixarEstoquePort.baixarEstoque(notaFiscal);
        registrarNotaFiscalPort.registrar(notaFiscal);
        agendarEntregaPort.agendar(notaFiscal);
        enviarNotaFiscalFinanceiroPort.enviar(notaFiscal);
        LOGGER.info(
                "Chamadas aos serviços externos concluídas: pedidoId={}, notaFiscalId={}",
                pedidoId,
                notaFiscal.getIdNotaFiscal());
    }
}
