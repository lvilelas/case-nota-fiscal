package br.com.itau.consumidoressimulados.adapter.in.sqs;

import br.com.itau.consumidoressimulados.config.ConsumerProperties;
import br.com.itau.consumidoressimulados.domain.ConsumerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ConsumerLifecycle implements SmartLifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerLifecycle.class);

    private final Map<ConsumerType, NotaFiscalSqsListener> listeners;
    private final ConsumerProperties properties;
    private final AtomicBoolean running = new AtomicBoolean();
    private ExecutorService executor;

    public ConsumerLifecycle(List<NotaFiscalSqsListener> listeners, ConsumerProperties properties) {
        this.listeners = new EnumMap<>(ConsumerType.class);
        listeners.forEach(listener -> this.listeners.put(listener.type(), listener));
        this.properties = properties;
    }

    @Override
    public synchronized void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        executor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("sqs-listener-", 0).factory());
        properties.type().selectedConsumers().forEach(type -> {
            NotaFiscalSqsListener listener = listeners.get(type);
            if (listener == null) {
                throw new IllegalStateException("Listener nao implementado para " + type);
            }
            executor.submit(() -> listener.listen(running::get));
        });
        LOGGER.info("Consumidores iniciados: mode={}, selected={}",
                properties.type(), properties.type().selectedConsumers());
    }

    @Override
    public synchronized void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        executor.shutdownNow();
        LOGGER.info("Encerramento dos consumidores solicitado");
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }
}
