package br.com.itau.geradornotafiscal.config;

import br.com.itau.geradornotafiscal.application.port.in.GerarNotaFiscalUseCase;
import br.com.itau.geradornotafiscal.application.port.out.AgendarEntregaPort;
import br.com.itau.geradornotafiscal.application.port.out.BaixarEstoquePort;
import br.com.itau.geradornotafiscal.application.port.out.EnviarNotaFiscalFinanceiroPort;
import br.com.itau.geradornotafiscal.application.port.out.RegistrarNotaFiscalPort;
import br.com.itau.geradornotafiscal.application.service.GerarNotaFiscalService;
import br.com.itau.geradornotafiscal.application.service.calculo.CalculadorFretePedido;
import br.com.itau.geradornotafiscal.application.service.calculo.CalculadorTotalPedido;
import br.com.itau.geradornotafiscal.application.service.calculo.CalculadorTributosPedido;
import br.com.itau.geradornotafiscal.application.service.factory.NotaFiscalFactory;
import br.com.itau.geradornotafiscal.application.service.integration.OrquestradorIntegracoesNotaFiscal;
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
            BaixarEstoquePort baixarEstoquePort,
            RegistrarNotaFiscalPort registrarNotaFiscalPort,
            AgendarEntregaPort agendarEntregaPort,
            EnviarNotaFiscalFinanceiroPort enviarNotaFiscalFinanceiroPort) {
        CalculadorTributacaoPorFaixa calculadorTributacao =
                new CalculadorTributacaoPorFaixa(new CalculadoraAliquotaProduto());
        CalculadorFrete calculadorFrete = new CalculadorFrete(new CatalogoFreteRegional());
        CalculadorTributosPedido calculadorTributosPedido = new CalculadorTributosPedido(
                new CatalogoTributario(),
                calculadorTributacao);
        OrquestradorIntegracoesNotaFiscal orquestradorIntegracoes =
                new OrquestradorIntegracoesNotaFiscal(
                        baixarEstoquePort,
                        registrarNotaFiscalPort,
                        agendarEntregaPort,
                        enviarNotaFiscalFinanceiroPort);

        return new GerarNotaFiscalService(
                new CalculadorTotalPedido(),
                calculadorTributosPedido,
                new CalculadorFretePedido(calculadorFrete),
                new NotaFiscalFactory(),
                orquestradorIntegracoes);
    }
}
