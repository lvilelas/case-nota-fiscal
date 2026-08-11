package br.com.itau.geradornotafiscal.adapter.out.integration;

import br.com.itau.geradornotafiscal.application.port.out.AgendarEntregaPort;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EntregaIntegrationAdapter implements AgendarEntregaPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntregaIntegrationAdapter.class);

    @Override
    public void agendar(NotaFiscal notaFiscal) {
        long inicio = System.nanoTime();
        int quantidadeItens = notaFiscal.getItens().size();
        LOGGER.info(
                "Iniciando chamada ao serviço de entrega: notaFiscalId={}, quantidadeItens={}",
                notaFiscal.getIdNotaFiscal(),
                quantidadeItens);
        try {
            Thread.sleep(150);
            if (quantidadeItens > 5) {
                LOGGER.warn(
                        "Serviço de entrega executará fluxo de alta latência: notaFiscalId={}, quantidadeItens={}",
                        notaFiscal.getIdNotaFiscal(),
                        quantidadeItens);
                Thread.sleep(5000);
            }
            Thread.sleep(200);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn(
                    "Chamada ao serviço de entrega interrompida: notaFiscalId={}, quantidadeItens={}",
                    notaFiscal.getIdNotaFiscal(),
                    quantidadeItens);
            throw new RuntimeException(exception);
        }
        LOGGER.info(
                "Chamada ao serviço de entrega concluída: notaFiscalId={}, quantidadeItens={}, duracaoMs={}",
                notaFiscal.getIdNotaFiscal(),
                quantidadeItens,
                (System.nanoTime() - inicio) / 1_000_000);
    }
}
