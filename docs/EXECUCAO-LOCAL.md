# Execução local

## Opção 1: ambiente completo em containers

```bash
docker compose up -d --build
docker compose ps
```

São iniciados:

- aplicação Java 21: <http://localhost:8080>;
- consumidores simulados (`ALL`): <http://localhost:8081/actuator/health>;
- PostgreSQL 16: `localhost:5432`;
- LocalStack SNS/SQS/Secrets: <http://localhost:4566>;
- Prometheus: <http://localhost:9090>;
- Jaeger: <http://localhost:16686>.

Endpoints úteis:

- readiness: <http://localhost:8080/actuator/health/readiness>;
- liveness: <http://localhost:8080/actuator/health/liveness>;
- métricas: <http://localhost:8080/actuator/prometheus>;
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>;
- Swagger UI: <http://localhost:8080/swagger-ui.html>.

O Flyway cria automaticamente `nota_fiscal_processada` e `outbox_evento`. O container `consumidores-simulados` inicia quatro listeners SQS em virtual threads e remove cada mensagem somente depois do respectivo handler. Prometheus coleta a aplicação e o worker. Os traces OTLP aparecem no Jaeger e os logs apresentam os identificadores de rastreabilidade.

## Consumidores SQS

Por padrão, um container consome estoque, registro, entrega e financeiro:

```bash
docker compose logs -f consumidores-simulados
```

Para demonstrar quatro processos independentes usando a mesma imagem, primeiro pare o modo `ALL` e depois inicie uma instância por tipo. `docker compose run` não publica a porta de management, evitando conflito entre as quatro instâncias:

```bash
docker compose stop consumidores-simulados
docker compose run -d --no-deps --name consumidor-estoque -e CONSUMER_TYPE=ESTOQUE consumidores-simulados
docker compose run -d --no-deps --name consumidor-registro -e CONSUMER_TYPE=REGISTRO consumidores-simulados
docker compose run -d --no-deps --name consumidor-entrega -e CONSUMER_TYPE=ENTREGA consumidores-simulados
docker compose run -d --no-deps --name consumidor-financeiro -e CONSUMER_TYPE=FINANCEIRO consumidores-simulados
```

Essa separação permite escalar e implantar cada tipo de consumidor de forma independente sem gerar quatro artefatos. Para retornar ao modo local padrão:

```bash
docker rm -f consumidor-estoque consumidor-registro consumidor-entrega consumidor-financeiro
docker compose up -d consumidores-simulados
```

## Opção 2: aplicação pelo IntelliJ

Suba apenas as dependências:

```bash
docker compose up -d postgres localstack jaeger
```

No IntelliJ, use **Active profiles** = `local` e execute `GeradorNotaFiscalApplication`. Não suba o serviço `app` ao mesmo tempo, pois ambos usam a porta 8080.

## Idempotência e filas

Envie duas vezes um payload com o mesmo `id_pedido`. As respostas devem possuir o mesmo `id_nota_fiscal`, com apenas uma nota e um evento no banco:

```bash
docker exec case-nota-fiscal-postgres psql -U nota_fiscal_app -d nota_fiscal \
  -c "select pedido_id, nota_fiscal_id from nota_fiscal_processada"

docker exec case-nota-fiscal-postgres psql -U nota_fiscal_app -d nota_fiscal \
  -c "select aggregate_id, status, tentativas from outbox_evento"
```

Consulte uma fila sem consumir definitivamente a mensagem:

```bash
docker exec case-nota-fiscal-localstack sh -lc '
URL=$(awslocal sqs get-queue-url --queue-name nota-fiscal-estoque --query QueueUrl --output text)
awslocal sqs receive-message --queue-url "$URL" --max-number-of-messages 10 \
  --message-attribute-names All --attribute-names All --visibility-timeout 0
'
```

Como o worker local consome rapidamente, pare `consumidores-simulados` antes dessa inspeção se quiser observar mensagens pendentes.

## Testes

```bash
./mvnw verify
./mvnw -f consumidores-simulados/pom.xml verify
```

Com Docker, Testcontainers valida PostgreSQL e LocalStack reais. O teste de carga JMeter está documentado em [performance/README.md](../performance/README.md).

## Encerramento

```bash
docker compose down
```

Para apagar também banco, mensagens e métricas locais:

```bash
docker compose down -v
```
