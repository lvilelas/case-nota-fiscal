package br.com.itau.consumidoressimulados.adapter.in.sqs;

import br.com.itau.consumidoressimulados.application.NotaFiscalGeradaHandler;
import br.com.itau.consumidoressimulados.config.ConsumerProperties;
import br.com.itau.consumidoressimulados.domain.ConsumerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

class SqsMessageProcessorTest {
    private SqsClient sqsClient;
    private NotaFiscalGeradaHandler handler;
    private SqsMessageProcessor processor;

    @BeforeEach
    void setUp() {
        sqsClient = mock(SqsClient.class);
        handler = mock(NotaFiscalGeradaHandler.class);
        var objectMapper = JsonMapper.builder()
                .findAndAddModules()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();
        var properties = new ConsumerProperties(
                ConsumerType.ALL, "nota-fiscal-", 20, 30, 10, Duration.ZERO);
        processor = new SqsMessageProcessor(sqsClient, objectMapper, properties);
    }

    @Test
    void shouldHandleAndDeleteAValidMessage() {
        Message message = message(validBody());

        processor.processMessage("queue-url", ConsumerType.ESTOQUE, handler, message);

        verify(handler).handle(any());
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
        assertThat(MDC.get("eventId")).isNull();
        assertThat(MDC.get("correlationId")).isNull();
        assertThat(MDC.get("flowId")).isNull();
    }

    @Test
    void shouldKeepMessageWhenHandlerFails() {
        doThrow(new IllegalStateException("external failure")).when(handler).handle(any());

        processor.processMessage("queue-url", ConsumerType.ESTOQUE, handler, message(validBody()));

        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void shouldRejectMalformedAndUnsupportedContracts() {
        processor.processMessage("queue-url", ConsumerType.ESTOQUE, handler, message("not-json"));
        processor.processMessage("queue-url", ConsumerType.ESTOQUE, handler,
                message(validBody().replace("NotaFiscalGerada", "OutroEvento")));
        processor.processMessage("queue-url", ConsumerType.ESTOQUE, handler,
                message(validBody().replace("\"event_version\":1", "\"event_version\":2")));
        processor.processMessage("queue-url", ConsumerType.ESTOQUE, handler,
                message(validBody().replace("\"event_id\":\"event-1\"", "\"event_id\":null")));
        processor.processMessage("queue-url", ConsumerType.ESTOQUE, handler,
                message(validBody().replace("\"nota_fiscal\":{", "\"nota_fiscal_original\":{")));

        verify(handler, never()).handle(any());
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void shouldLongPollUntilStopped() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("queue-url").build());
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().build());
        AtomicInteger checks = new AtomicInteger();
        BooleanSupplier running = () -> checks.getAndIncrement() < 2;

        processor.listen(ConsumerType.ESTOQUE, handler, running);

        verify(sqsClient, times(2)).receiveMessage(any(ReceiveMessageRequest.class));
    }

    @Test
    void shouldRecoverFromPollingFailureWhileRunning() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("queue-url").build());
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenThrow(new IllegalStateException("SQS unavailable"));
        AtomicInteger checks = new AtomicInteger();
        BooleanSupplier running = () -> checks.getAndIncrement() < 2;

        processor.listen(ConsumerType.ESTOQUE, handler, running);

        verify(sqsClient).receiveMessage(any(ReceiveMessageRequest.class));
    }

    @Test
    void shouldStopWithoutBackoffWhenFailureHappensDuringShutdown() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("queue-url").build());
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenThrow(new IllegalStateException("SQS unavailable"));
        AtomicInteger checks = new AtomicInteger();
        BooleanSupplier running = () -> checks.getAndIncrement() == 0;

        processor.listen(ConsumerType.ESTOQUE, handler, running);

        verify(sqsClient).receiveMessage(any(ReceiveMessageRequest.class));
    }

    @Test
    void shouldRespectAnInterruptedListenerThread() {
        Thread.currentThread().interrupt();
        try {
            processor.listen(ConsumerType.ESTOQUE, handler, () -> true);
            verify(sqsClient, never()).getQueueUrl(any(GetQueueUrlRequest.class));
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void shouldPreserveInterruptFlagDuringErrorBackoff() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl("queue-url").build());
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenAnswer(invocation -> {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("SQS unavailable");
        });
        AtomicInteger checks = new AtomicInteger();
        BooleanSupplier running = () -> checks.getAndIncrement() < 2;

        try {
            processor.listen(ConsumerType.ESTOQUE, handler, running);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    private Message message(String body) {
        return Message.builder()
                .messageId("message-1")
                .receiptHandle("receipt-1")
                .body(body)
                .build();
    }

    private String validBody() {
        return """
                {
                  "event_id":"event-1",
                  "event_type":"NotaFiscalGerada",
                  "event_version":1,
                  "occurred_at":"2026-08-11T12:00:00Z",
                  "correlation_id":"correlation-1",
                  "flow_id":"flow-1",
                  "idempotency_key":"pedido:42",
                  "pedido_id":42,
                  "nota_fiscal":{"id_nota_fiscal":"nf-1","itens":[{"id_item":"item-1"}]}
                }
                """;
    }
}
