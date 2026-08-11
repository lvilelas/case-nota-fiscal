package br.com.itau.geradornotafiscal.adapter.out.aws.sns;

import br.com.itau.geradornotafiscal.application.event.NotaFiscalGeradaEvent;
import br.com.itau.geradornotafiscal.application.exception.PublicacaoEventoException;
import br.com.itau.geradornotafiscal.config.properties.AwsProperties;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.time.Duration;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SnsNotaFiscalGeradaAdapterTest {
    private static final String TOPIC_ARN = "arn:aws:sns:us-east-1:123456789012:nota-fiscal-gerada";

    @Test
    void devePublicarEventoVersionadoComAtributosDeRastreabilidadeEIdempotencia() throws Exception {
        SnsClient snsClient = mock(SnsClient.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        ArgumentCaptor<NotaFiscalGeradaEvent> eventCaptor = ArgumentCaptor.forClass(NotaFiscalGeradaEvent.class);
        ArgumentCaptor<PublishRequest> requestCaptor = ArgumentCaptor.forClass(PublishRequest.class);
        when(objectMapper.writeValueAsString(eventCaptor.capture())).thenReturn("{\"event_version\":1}");
        when(snsClient.publish(requestCaptor.capture()))
                .thenReturn(PublishResponse.builder().messageId("message-1").build());
        NotaFiscal notaFiscal = NotaFiscal.builder().idNotaFiscal("nf-1").itens(List.of()).build();
        NotaFiscalGeradaEvent evento = evento(99, notaFiscal);

        new SnsNotaFiscalGeradaAdapter(snsClient, objectMapper, properties()).publicar(evento);

        NotaFiscalGeradaEvent event = eventCaptor.getValue();
        assertSame(evento, event);
        assertEquals("NotaFiscalGerada", event.eventType());
        assertEquals(1, event.eventVersion());
        assertEquals("correlation-1", event.correlationId());
        assertEquals("flow-1", event.flowId());
        assertEquals("nota-fiscal-gerada:pedido:99", event.idempotencyKey());
        assertEquals(99, event.pedidoId());
        assertSame(notaFiscal, event.notaFiscal());

        PublishRequest request = requestCaptor.getValue();
        assertEquals(TOPIC_ARN, request.topicArn());
        assertEquals("{\"event_version\":1}", request.message());
        assertEquals("NotaFiscalGerada", request.messageAttributes().get("eventType").stringValue());
        assertEquals("1", request.messageAttributes().get("eventVersion").stringValue());
        assertEquals(event.eventId(), request.messageAttributes().get("eventId").stringValue());
        assertEquals("correlation-1", request.messageAttributes().get("correlationId").stringValue());
        assertEquals("flow-1", request.messageAttributes().get("flowId").stringValue());
        assertEquals("nota-fiscal-gerada:pedido:99", request.messageAttributes().get("idempotencyKey").stringValue());
        verify(snsClient).publish(any(PublishRequest.class));
    }

    @Test
    void deveTraduzirFalhaDePublicacao() throws Exception {
        SnsClient snsClient = mock(SnsClient.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        RuntimeException causa = new RuntimeException("serializacao indisponivel");
        when(objectMapper.writeValueAsString(any(NotaFiscalGeradaEvent.class))).thenThrow(causa);
        NotaFiscal notaFiscal = NotaFiscal.builder().idNotaFiscal("nf-2").itens(List.of()).build();
        NotaFiscalGeradaEvent evento = evento(100, notaFiscal);

        PublicacaoEventoException exception = assertThrows(
                PublicacaoEventoException.class,
                () -> new SnsNotaFiscalGeradaAdapter(snsClient, objectMapper, properties())
                        .publicar(evento));

        assertEquals("Falha ao publicar o evento NotaFiscalGerada", exception.getMessage());
        assertSame(causa, exception.getCause());
    }

    private NotaFiscalGeradaEvent evento(int pedidoId, NotaFiscal notaFiscal) {
        return new NotaFiscalGeradaEvent(
                "event-1",
                "NotaFiscalGerada",
                1,
                OffsetDateTime.parse("2026-08-11T20:00:00Z"),
                "correlation-1",
                "flow-1",
                "nota-fiscal-gerada:pedido:" + pedidoId,
                pedidoId,
                notaFiscal);
    }

    private AwsProperties properties() {
        return new AwsProperties(
                "us-east-1",
                "http://localhost:4566",
                "test",
                "test",
                new AwsProperties.Sns(TOPIC_ARN),
                new AwsProperties.Retry(4, Duration.ofMillis(200), Duration.ofSeconds(5)),
                new AwsProperties.Secrets("case-nota-fiscal/local"));
    }
}
