package br.com.itau.geradornotafiscal.config;

import br.com.itau.geradornotafiscal.application.port.in.GerarNotaFiscalUseCase;
import br.com.itau.geradornotafiscal.application.port.out.PublicarNotaFiscalGeradaPort;
import br.com.itau.geradornotafiscal.application.service.GerarNotaFiscalService;
import br.com.itau.geradornotafiscal.application.service.calculo.CalculadorFretePedido;
import br.com.itau.geradornotafiscal.application.service.calculo.CalculadorTotalPedido;
import br.com.itau.geradornotafiscal.application.service.calculo.CalculadorTributosPedido;
import br.com.itau.geradornotafiscal.application.service.factory.NotaFiscalFactory;
import br.com.itau.geradornotafiscal.application.service.integration.PublicadorNotaFiscalGerada;
import br.com.itau.geradornotafiscal.domain.service.frete.CalculadorFrete;
import br.com.itau.geradornotafiscal.domain.service.frete.CatalogoFreteRegional;
import br.com.itau.geradornotafiscal.domain.service.tributacao.CalculadoraAliquotaProduto;
import br.com.itau.geradornotafiscal.domain.service.tributacao.CalculadorTributacaoPorFaixa;
import br.com.itau.geradornotafiscal.domain.service.tributacao.CatalogoTributario;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeradorNotaFiscalConfiguration {
    @Bean
    GerarNotaFiscalUseCase gerarNotaFiscalUseCase(
            PublicarNotaFiscalGeradaPort publicarNotaFiscalGeradaPort) {
        CalculadorTributacaoPorFaixa calculadorTributacao =
                new CalculadorTributacaoPorFaixa(new CalculadoraAliquotaProduto());
        CalculadorFrete calculadorFrete = new CalculadorFrete(new CatalogoFreteRegional());
        CalculadorTributosPedido calculadorTributosPedido = new CalculadorTributosPedido(
                new CatalogoTributario(),
                calculadorTributacao);
        return new GerarNotaFiscalService(
                new CalculadorTotalPedido(),
                calculadorTributosPedido,
                new CalculadorFretePedido(calculadorFrete),
                new NotaFiscalFactory(),
                new PublicadorNotaFiscalGerada(publicarNotaFiscalGeradaPort));
    }
}
