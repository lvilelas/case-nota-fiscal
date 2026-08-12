package br.com.itau.geradornotafiscal.adapter.out.aws.sns;

import br.com.itau.geradornotafiscal.application.event.NotaFiscalGeradaEvent;
import br.com.itau.geradornotafiscal.application.exception.PublicacaoEventoException;
import br.com.itau.geradornotafiscal.application.port.out.PublicarNotaFiscalGeradaPort;
import br.com.itau.geradornotafiscal.config.properties.AwsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

public class SnsNotaFiscalGeradaAdapter implements PublicarNotaFiscalGeradaPort {
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
    public void publicar(NotaFiscalGeradaEvent evento) {
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
                    evento.notaFiscal().getIdNotaFiscal());
        } catch (Exception exception) {
            throw new PublicacaoEventoException(
                    "Falha ao publicar o evento NotaFiscalGerada",
                    exception);
        }
    }

    private Map<String, MessageAttributeValue> atributos(NotaFiscalGeradaEvent evento) {
        return Map.of(
                "eventType", atributo(evento.eventType()),
                "eventVersion", atributo(String.valueOf(evento.eventVersion())),
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

}
