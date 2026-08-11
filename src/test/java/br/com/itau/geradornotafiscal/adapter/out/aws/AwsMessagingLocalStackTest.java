package br.com.itau.geradornotafiscal.adapter.out.aws;

import br.com.itau.geradornotafiscal.adapter.out.aws.secrets.AwsSecretsManagerAdapter;
import br.com.itau.geradornotafiscal.adapter.out.aws.sns.SnsNotaFiscalGeradaAdapter;
import br.com.itau.geradornotafiscal.config.properties.AwsProperties;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class AwsMessagingLocalStackTest {
    private static final List<String> CONSUMIDORES = List.of("estoque", "registro", "entrega", "financeiro");

    @Container
    static final LocalStackContainer LOCALSTACK = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:4.14.0"))
            .withServices("sns", "sqs", "secretsmanager");

    private static final Map<String, String> QUEUE_URLS = new LinkedHashMap<>();
    private static SnsClient snsClient;
    private static SqsClient sqsClient;
    private static SecretsManagerClient secretsManagerClient;
    private static String topicArn;

    @BeforeAll
    static void provisionarInfraestrutura() {
        var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(
                LOCALSTACK.getAccessKey(),
                LOCALSTACK.getSecretKey()));
        Region region = Region.of(LOCALSTACK.getRegion());
        snsClient = SnsClient.builder()
                .endpointOverride(LOCALSTACK.getEndpoint())
                .credentialsProvider(credentials)
                .region(region)
                .build();
        sqsClient = SqsClient.builder()
                .endpointOverride(LOCALSTACK.getEndpoint())
                .credentialsProvider(credentials)
                .region(region)
                .build();
        secretsManagerClient = SecretsManagerClient.builder()
                .endpointOverride(LOCALSTACK.getEndpoint())
                .credentialsProvider(credentials)
                .region(region)
                .build();

        topicArn = snsClient.createTopic(builder -> builder.name("nota-fiscal-gerada-test")).topicArn();
        CONSUMIDORES.forEach(AwsMessagingLocalStackTest::criarFilaComDlqEAssinarTopico);
    }

    @AfterAll
    static void fecharClientes() {
        if (snsClient != null) {
            snsClient.close();
            sqsClient.close();
            secretsManagerClient.close();
        }
    }

    @Test
    void deveEntregarMesmoEventoVersionadoNasQuatroFilas() throws Exception {
        var properties = new AwsProperties(
                LOCALSTACK.getRegion(),
                LOCALSTACK.getEndpoint().toString(),
                LOCALSTACK.getAccessKey(),
                LOCALSTACK.getSecretKey(),
                new AwsProperties.Sns(topicArn),
                new AwsProperties.Retry(4, Duration.ofMillis(200), Duration.ofSeconds(5)),
                new AwsProperties.Secrets("case-nota-fiscal/test"));
        var objectMapper = JsonMapper.builder()
                .findAndAddModules()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();
        var notaFiscal = NotaFiscal.builder().idNotaFiscal("nf-localstack").itens(List.of()).build();

        new SnsNotaFiscalGeradaAdapter(snsClient, objectMapper, properties).publicar(77, notaFiscal);

        for (Map.Entry<String, String> queue : QUEUE_URLS.entrySet()) {
            var messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queue.getValue())
                    .waitTimeSeconds(10)
                    .maxNumberOfMessages(1)
                    .build()).messages();
            assertEquals(1, messages.size(), "evento ausente na fila " + queue.getKey());
            var event = objectMapper.readTree(messages.getFirst().body());
            assertEquals("NotaFiscalGerada", event.get("event_type").asText());
            assertEquals(1, event.get("event_version").asInt());
            assertEquals(77, event.get("pedido_id").asInt());
            assertEquals("nf-localstack", event.get("nota_fiscal").get("id_nota_fiscal").asText());
            assertEquals("nota-fiscal-gerada:pedido:77", event.get("idempotency_key").asText());
        }
    }

    @Test
    void deveRecuperarSegredoPeloMesmoAdapterUsadoNaAws() {
        secretsManagerClient.createSecret(CreateSecretRequest.builder()
                .name("case-nota-fiscal/test")
                .secretString("{\"token\":\"somente-local\"}")
                .build());

        String segredo = new AwsSecretsManagerAdapter(secretsManagerClient).buscar("case-nota-fiscal/test");

        assertEquals("{\"token\":\"somente-local\"}", segredo);
    }

    private static void criarFilaComDlqEAssinarTopico(String consumidor) {
        String queueName = "nota-fiscal-" + consumidor + "-test";
        String dlqUrl = sqsClient.createQueue(builder -> builder.queueName(queueName + "-dlq")).queueUrl();
        String dlqArn = queueArn(dlqUrl);
        String redrivePolicy = "{\"deadLetterTargetArn\":\"%s\",\"maxReceiveCount\":\"3\"}"
                .formatted(dlqArn);
        String queueUrl = sqsClient.createQueue(CreateQueueRequest.builder()
                .queueName(queueName)
                .attributes(Map.of(QueueAttributeName.REDRIVE_POLICY, redrivePolicy))
                .build()).queueUrl();
        String queueArn = queueArn(queueUrl);
        String policy = """
                {"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"sns.amazonaws.com"},"Action":"sqs:SendMessage","Resource":"%s","Condition":{"ArnEquals":{"aws:SourceArn":"%s"}}}]}
                """.formatted(queueArn, topicArn).trim();
        sqsClient.setQueueAttributes(SetQueueAttributesRequest.builder()
                .queueUrl(queueUrl)
                .attributes(Map.of(QueueAttributeName.POLICY, policy))
                .build());
        snsClient.subscribe(SubscribeRequest.builder()
                .topicArn(topicArn)
                .protocol("sqs")
                .endpoint(queueArn)
                .attributes(Map.of("RawMessageDelivery", "true"))
                .build());

        String configuredRedrivePolicy = sqsClient.getQueueAttributes(GetQueueAttributesRequest.builder()
                        .queueUrl(queueUrl)
                        .attributeNames(QueueAttributeName.REDRIVE_POLICY)
                        .build())
                .attributes().get(QueueAttributeName.REDRIVE_POLICY);
        assertTrue(configuredRedrivePolicy.contains(dlqArn));
        QUEUE_URLS.put(consumidor, queueUrl);
    }

    private static String queueArn(String queueUrl) {
        return sqsClient.getQueueAttributes(GetQueueAttributesRequest.builder()
                        .queueUrl(queueUrl)
                        .attributeNames(QueueAttributeName.QUEUE_ARN)
                        .build())
                .attributes().get(QueueAttributeName.QUEUE_ARN);
    }
}
