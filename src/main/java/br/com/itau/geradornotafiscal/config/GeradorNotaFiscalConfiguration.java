package br.com.itau.geradornotafiscal.config;

import br.com.itau.geradornotafiscal.application.port.in.GerarNotaFiscalUseCase;
import br.com.itau.geradornotafiscal.application.port.out.AgendarEntregaPort;
import br.com.itau.geradornotafiscal.application.port.out.BaixarEstoquePort;
import br.com.itau.geradornotafiscal.application.port.out.EnviarNotaFiscalFinanceiroPort;
import br.com.itau.geradornotafiscal.application.port.out.RegistrarNotaFiscalPort;
import br.com.itau.geradornotafiscal.application.service.GerarNotaFiscalService;
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

        return new GerarNotaFiscalService(
                new CatalogoTributario(),
                calculadorTributacao,
                calculadorFrete,
                baixarEstoquePort,
                registrarNotaFiscalPort,
                agendarEntregaPort,
                enviarNotaFiscalFinanceiroPort);
    }
}
