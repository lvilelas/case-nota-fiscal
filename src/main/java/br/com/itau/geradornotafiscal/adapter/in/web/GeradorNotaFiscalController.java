package br.com.itau.geradornotafiscal.adapter.in.web;

import br.com.itau.geradornotafiscal.application.port.in.GerarNotaFiscalUseCase;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import br.com.itau.geradornotafiscal.domain.model.Pedido;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedido")
public class GeradorNotaFiscalController {
    private final GerarNotaFiscalUseCase gerarNotaFiscalUseCase;

    public GeradorNotaFiscalController(GerarNotaFiscalUseCase gerarNotaFiscalUseCase) {
        this.gerarNotaFiscalUseCase = gerarNotaFiscalUseCase;
    }

    @PostMapping("/gerarNotaFiscal")
    public ResponseEntity<NotaFiscal> gerarNotaFiscal(@RequestBody Pedido pedido) {
        return ResponseEntity.ok(gerarNotaFiscalUseCase.gerarNotaFiscal(pedido));
    }
}
