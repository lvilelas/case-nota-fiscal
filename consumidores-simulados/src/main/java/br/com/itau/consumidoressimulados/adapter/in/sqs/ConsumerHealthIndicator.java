package br.com.itau.consumidoressimulados.adapter.in.sqs;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class ConsumerHealthIndicator implements HealthIndicator {
    private final ConsumerLifecycle lifecycle;

    public ConsumerHealthIndicator(ConsumerLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public Health health() {
        return lifecycle.isRunning()
                ? Health.up().withDetail("listeners", "running").build()
                : Health.down().withDetail("listeners", "stopped").build();
    }
}
