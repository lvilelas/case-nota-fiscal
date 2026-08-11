package br.com.itau.geradornotafiscal.config;

import br.com.itau.geradornotafiscal.application.port.out.OutboxNotaFiscalPort;
import br.com.itau.geradornotafiscal.application.port.out.PublicarNotaFiscalGeradaPort;
import br.com.itau.geradornotafiscal.application.service.integration.PublicadorOutboxNotaFiscal;
import br.com.itau.geradornotafiscal.config.properties.OutboxProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxConfiguration {
    @Bean
    PublicadorOutboxNotaFiscal publicadorOutboxNotaFiscal(
            OutboxNotaFiscalPort outboxPort,
            PublicarNotaFiscalGeradaPort publicadorPort,
            OutboxProperties properties) {
        return new PublicadorOutboxNotaFiscal(
                outboxPort,
                publicadorPort,
                properties.maxAttempts(),
                properties.baseDelay(),
                properties.maxBackoff());
    }
}
