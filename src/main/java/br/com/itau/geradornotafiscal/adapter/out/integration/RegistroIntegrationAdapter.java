package br.com.itau.geradornotafiscal.adapter.out.integration;

import br.com.itau.geradornotafiscal.application.port.out.RegistrarNotaFiscalPort;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RegistroIntegrationAdapter implements RegistrarNotaFiscalPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistroIntegrationAdapter.class);

    @Override
    public void registrar(NotaFiscal notaFiscal) {
        long inicio = System.nanoTime();
        LOGGER.info(
                "Iniciando chamada ao serviço de registro: notaFiscalId={}",
                notaFiscal.getIdNotaFiscal());
        try {
            Thread.sleep(500);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn(
                    "Chamada ao serviço de registro interrompida: notaFiscalId={}",
                    notaFiscal.getIdNotaFiscal());
            throw new RuntimeException(exception);
        }
        LOGGER.info(
                "Chamada ao serviço de registro concluída: notaFiscalId={}, duracaoMs={}",
                notaFiscal.getIdNotaFiscal(),
                (System.nanoTime() - inicio) / 1_000_000);
    }
}
