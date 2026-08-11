package br.com.itau.geradornotafiscal.adapter.out.integration;

import br.com.itau.geradornotafiscal.application.port.out.BaixarEstoquePort;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EstoqueIntegrationAdapter implements BaixarEstoquePort {
    private static final Logger LOGGER = LoggerFactory.getLogger(EstoqueIntegrationAdapter.class);

    @Override
    public void baixarEstoque(NotaFiscal notaFiscal) {
        long inicio = System.nanoTime();
        LOGGER.info(
                "Iniciando chamada ao serviço de estoque: notaFiscalId={}",
                notaFiscal.getIdNotaFiscal());
        try {
            Thread.sleep(380);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn(
                    "Chamada ao serviço de estoque interrompida: notaFiscalId={}",
                    notaFiscal.getIdNotaFiscal());
            throw new RuntimeException(exception);
        }
        LOGGER.info(
                "Chamada ao serviço de estoque concluída: notaFiscalId={}, duracaoMs={}",
                notaFiscal.getIdNotaFiscal(),
                (System.nanoTime() - inicio) / 1_000_000);
    }
}
