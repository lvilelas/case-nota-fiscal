package br.com.itau.geradornotafiscal.adapter.in.web;

import br.com.itau.geradornotafiscal.application.port.in.GerarNotaFiscalUseCase;
import br.com.itau.geradornotafiscal.adapter.in.web.dto.PedidoRequest;
import br.com.itau.geradornotafiscal.adapter.in.web.mapper.PedidoWebMapper;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import br.com.itau.geradornotafiscal.domain.model.Pedido;
import jakarta.validation.Valid;
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
    private final PedidoWebMapper pedidoWebMapper;

    public GeradorNotaFiscalController(
            GerarNotaFiscalUseCase gerarNotaFiscalUseCase,
            PedidoWebMapper pedidoWebMapper) {
        this.gerarNotaFiscalUseCase = gerarNotaFiscalUseCase;
        this.pedidoWebMapper = pedidoWebMapper;
    }

    @PostMapping("/gerarNotaFiscal")
    public ResponseEntity<NotaFiscal> gerarNotaFiscal(@Valid @RequestBody PedidoRequest request) {
        Pedido pedido = pedidoWebMapper.paraDominio(request);
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
