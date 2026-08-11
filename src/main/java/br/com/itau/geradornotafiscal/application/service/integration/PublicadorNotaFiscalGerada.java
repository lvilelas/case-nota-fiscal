package br.com.itau.geradornotafiscal.application.service.integration;

import br.com.itau.geradornotafiscal.application.port.out.PublicarNotaFiscalGeradaPort;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublicadorNotaFiscalGerada {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublicadorNotaFiscalGerada.class);

    private final PublicarNotaFiscalGeradaPort publicarNotaFiscalGeradaPort;

    public PublicadorNotaFiscalGerada(PublicarNotaFiscalGeradaPort publicarNotaFiscalGeradaPort) {
        this.publicarNotaFiscalGeradaPort = publicarNotaFiscalGeradaPort;
    }

    public void publicar(int pedidoId, NotaFiscal notaFiscal) {
        LOGGER.info(
                "Iniciando publicação do evento NotaFiscalGerada: pedidoId={}, notaFiscalId={}",
                pedidoId,
                notaFiscal.getIdNotaFiscal());
        publicarNotaFiscalGeradaPort.publicar(pedidoId, notaFiscal);
        LOGGER.info(
                "Publicação do evento NotaFiscalGerada concluída: pedidoId={}, notaFiscalId={}",
                pedidoId,
                notaFiscal.getIdNotaFiscal());
    }
}
