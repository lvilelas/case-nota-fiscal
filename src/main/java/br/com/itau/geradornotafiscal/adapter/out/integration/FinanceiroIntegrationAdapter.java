package br.com.itau.geradornotafiscal.adapter.out.integration;

import br.com.itau.geradornotafiscal.application.port.out.EnviarNotaFiscalFinanceiroPort;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FinanceiroIntegrationAdapter implements EnviarNotaFiscalFinanceiroPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(FinanceiroIntegrationAdapter.class);

    @Override
    public void enviar(NotaFiscal notaFiscal) {
        long inicio = System.nanoTime();
        LOGGER.info(
                "Iniciando chamada ao serviço financeiro: notaFiscalId={}",
                notaFiscal.getIdNotaFiscal());
        try {
            Thread.sleep(250);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn(
                    "Chamada ao serviço financeiro interrompida: notaFiscalId={}",
                    notaFiscal.getIdNotaFiscal());
            throw new RuntimeException(exception);
        }
        LOGGER.info(
                "Chamada ao serviço financeiro concluída: notaFiscalId={}, duracaoMs={}",
                notaFiscal.getIdNotaFiscal(),
                (System.nanoTime() - inicio) / 1_000_000);
    }
}
