package br.com.itau.geradornotafiscal.adapter.in.web;

import br.com.itau.geradornotafiscal.application.port.in.GerarNotaFiscalUseCase;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import br.com.itau.geradornotafiscal.domain.model.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedido")
public class GeradorNotaFiscalController {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeradorNotaFiscalController.class);

    private final GerarNotaFiscalUseCase gerarNotaFiscalUseCase;

    public GeradorNotaFiscalController(GerarNotaFiscalUseCase gerarNotaFiscalUseCase) {
        this.gerarNotaFiscalUseCase = gerarNotaFiscalUseCase;
    }

    @PostMapping("/gerarNotaFiscal")
    public ResponseEntity<NotaFiscal> gerarNotaFiscal(@RequestBody Pedido pedido) {
        LOGGER.info(
                "Pedido recebido pela API: pedidoId={}, quantidadeItens={}, valorTotalInformado={}, valorFrete={}",
                pedido.getIdPedido(),
                pedido.getItens().size(),
                pedido.getValorTotalItens(),
                pedido.getValorFrete());

        try {
            NotaFiscal notaFiscal = gerarNotaFiscalUseCase.gerarNotaFiscal(pedido);
            LOGGER.info(
                    "Resposta da API preparada: pedidoId={}, notaFiscalId={}, quantidadeItens={}, valorTotalItens={}, valorFrete={}",
                    pedido.getIdPedido(),
                    notaFiscal.getIdNotaFiscal(),
                    notaFiscal.getItens().size(),
                    notaFiscal.getValorTotalItens(),
                    notaFiscal.getValorFrete());
            return ResponseEntity.ok(notaFiscal);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Falha ao processar pedido recebido pela API: pedidoId={}, tipoErro={}",
                    pedido.getIdPedido(),
                    exception.getClass().getSimpleName());
            throw exception;
        }
    }
}
