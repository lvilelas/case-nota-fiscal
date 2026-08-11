package br.com.itau.geradornotafiscal.adapter.out.aws.sns;

import br.com.itau.geradornotafiscal.application.event.NotaFiscalGeradaEvent;
import br.com.itau.geradornotafiscal.application.exception.PublicacaoEventoException;
import br.com.itau.geradornotafiscal.application.port.out.PublicarNotaFiscalGeradaPort;
import br.com.itau.geradornotafiscal.config.properties.AwsProperties;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class SnsNotaFiscalGeradaAdapter implements PublicarNotaFiscalGeradaPort {
    static final String EVENT_TYPE = "NotaFiscalGerada";
    static final int EVENT_VERSION = 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(SnsNotaFiscalGeradaAdapter.class);

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    private final AwsProperties awsProperties;

    public SnsNotaFiscalGeradaAdapter(
            SnsClient snsClient,
            ObjectMapper objectMapper,
            AwsProperties awsProperties) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
        this.awsProperties = awsProperties;
    }

    @Override
    public void publicar(int pedidoId, NotaFiscal notaFiscal) {
        NotaFiscalGeradaEvent evento = criarEvento(pedidoId, notaFiscal);
        try {
            String mensagem = objectMapper.writeValueAsString(evento);
            PublishResponse response = snsClient.publish(PublishRequest.builder()
                    .topicArn(awsProperties.sns().topicArn())
                    .message(mensagem)
                    .messageAttributes(atributos(evento))
                    .build());
            LOGGER.info(
                    "Evento publicado no SNS: eventId={}, eventVersion={}, messageId={}, notaFiscalId={}",
                    evento.eventId(),
                    evento.eventVersion(),
                    response.messageId(),
                    notaFiscal.getIdNotaFiscal());
        } catch (Exception exception) {
            throw new PublicacaoEventoException(
                    "Falha ao publicar o evento NotaFiscalGerada",
                    exception);
        }
    }

    private NotaFiscalGeradaEvent criarEvento(int pedidoId, NotaFiscal notaFiscal) {
        return new NotaFiscalGeradaEvent(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                EVENT_VERSION,
                OffsetDateTime.now(ZoneOffset.UTC),
                contexto("correlationId"),
                contexto("flowId"),
                "nota-fiscal-gerada:pedido:" + pedidoId,
                pedidoId,
                notaFiscal);
    }

    private Map<String, MessageAttributeValue> atributos(NotaFiscalGeradaEvent evento) {
        return Map.of(
                "eventType", atributo(EVENT_TYPE),
                "eventVersion", atributo(String.valueOf(EVENT_VERSION)),
                "eventId", atributo(evento.eventId()),
                "correlationId", atributo(evento.correlationId()),
                "flowId", atributo(evento.flowId()),
                "idempotencyKey", atributo(evento.idempotencyKey()));
    }

    private MessageAttributeValue atributo(String valor) {
        return MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(valor)
                .build();
    }

    private String contexto(String nome) {
        return Objects.requireNonNullElse(MDC.get(nome), "not-provided");
    }
}
