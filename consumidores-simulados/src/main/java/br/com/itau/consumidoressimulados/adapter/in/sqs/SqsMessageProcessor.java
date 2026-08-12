package br.com.itau.consumidoressimulados.adapter.in.sqs;

import br.com.itau.consumidoressimulados.application.NotaFiscalGeradaHandler;
import br.com.itau.consumidoressimulados.config.ConsumerProperties;
import br.com.itau.consumidoressimulados.domain.ConsumerType;
import br.com.itau.consumidoressimulados.domain.NotaFiscalGeradaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import tools.jackson.databind.ObjectMapper;

import java.util.function.BooleanSupplier;

@Component
public class SqsMessageProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SqsMessageProcessor.class);
    private static final String EVENT_TYPE = "NotaFiscalGerada";
    private static final int EVENT_VERSION = 1;

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final ConsumerProperties properties;

    public SqsMessageProcessor(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            ConsumerProperties properties) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public void listen(
            ConsumerType type,
            NotaFiscalGeradaHandler handler,
            BooleanSupplier isRunning) {
        String queueName = properties.queuePrefix() + type.queueSuffix();
        String queueUrl = null;

        while (isRunning.getAsBoolean() && !Thread.currentThread().isInterrupted()) {
            try {
                if (queueUrl == null) {
                    queueUrl = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                                    .queueName(queueName)
                                    .build())
                            .queueUrl();
                    LOGGER.info("Listener SQS iniciado: consumerType={}, queueName={}", type, queueName);
                }
                String currentQueueUrl = queueUrl;
                receive(currentQueueUrl).messages()
                        .forEach(message -> processMessage(currentQueueUrl, type, handler, message));
            } catch (RuntimeException exception) {
                if (isRunning.getAsBoolean()) {
                    LOGGER.error("Falha ao consultar fila SQS: consumerType={}, queueName={}",
                            type, queueName, exception);
                    queueUrl = null;
                    pauseAfterError();
                }
            }
        }
        LOGGER.info("Listener SQS encerrado: consumerType={}, queueName={}", type, queueName);
    }

    void processMessage(
            String queueUrl,
            ConsumerType type,
            NotaFiscalGeradaHandler handler,
            Message message) {
        long start = System.nanoTime();
        try {
            NotaFiscalGeradaEvent event = deserializeAndValidate(message.body());
            try (MDC.MDCCloseable ignoredCorrelation = MDC.putCloseable("correlationId", event.correlationId());
                 MDC.MDCCloseable ignoredFlow = MDC.putCloseable("flowId", event.flowId());
                 MDC.MDCCloseable ignoredEvent = MDC.putCloseable("eventId", event.eventId())) {
                LOGGER.info("Mensagem recebida: consumerType={}, messageId={}, pedidoId={}",
                        type, message.messageId(), event.pedidoId());
                handler.handle(event);
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build());
                LOGGER.info("Mensagem processada e removida: consumerType={}, messageId={}, duracaoMs={}",
                        type, message.messageId(), elapsedMilliseconds(start));
            }
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Falha ao processar mensagem; ela permanecera na fila para retry/DLQ: consumerType={}, messageId={}",
                    type, message.messageId(), exception);
        }
    }

    private software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse receive(String queueUrl) {
        return sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .waitTimeSeconds(properties.waitTimeSeconds())
                .visibilityTimeout(properties.visibilityTimeoutSeconds())
                .maxNumberOfMessages(properties.maxMessages())
                .build());
    }

    private NotaFiscalGeradaEvent deserializeAndValidate(String body) {
        NotaFiscalGeradaEvent event = objectMapper.readValue(body, NotaFiscalGeradaEvent.class);
        if (!EVENT_TYPE.equals(event.eventType()) || event.eventVersion() != EVENT_VERSION) {
            throw new UnsupportedEventException(
                    "Contrato nao suportado: type=" + event.eventType() + ", version=" + event.eventVersion());
        }
        if (event.eventId() == null || event.notaFiscal() == null) {
            throw new UnsupportedEventException("Evento sem event_id ou nota_fiscal");
        }
        return event;
    }

    private void pauseAfterError() {
        try {
            Thread.sleep(properties.errorBackoff().toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private long elapsedMilliseconds(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }
}
