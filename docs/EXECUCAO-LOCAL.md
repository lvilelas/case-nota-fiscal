# Execução local

## Subir PostgreSQL e serviços AWS locais

```bash
docker compose up -d
docker compose ps
```

O Compose sobe:

- PostgreSQL 16, banco `nota_fiscal`, porta `5432`;
- LocalStack com SNS, SQS e Secrets Manager, porta `4566`;
- tópico `nota-fiscal-gerada`;
- filas de estoque, registro, entrega e financeiro, cada uma com DLQ;
- secret fictício `case-nota-fiscal/local`.

Execute a aplicação com o profile `local` (já é o padrão):

```bash
./mvnw spring-boot:run
```

No IntelliJ, basta preencher **Active profiles** com `local`. Os defaults locais apontam para os containers. A migration Flyway cria automaticamente `nota_fiscal_processada` e `outbox_evento`.

Para inspecionar a idempotência:

```bash
docker exec -it case-nota-fiscal-postgres psql -U nota_fiscal_app -d nota_fiscal \
  -c "select pedido_id, nota_fiscal_id, criado_em from nota_fiscal_processada"

docker exec -it case-nota-fiscal-postgres psql -U nota_fiscal_app -d nota_fiscal \
  -c "select event_id, aggregate_id, status, tentativas from outbox_evento"
```

Envie duas vezes o mesmo payload/pedido. As duas respostas devem possuir o mesmo `id_nota_fiscal`, e as consultas devem mostrar uma nota e um evento para aquele pedido.

## Testes

```bash
./mvnw verify
```

Com Docker disponível, Testcontainers inicia PostgreSQL e LocalStack isolados. Sem Docker, apenas os testes de infraestrutura são ignorados; os unitários continuam executando.

## Executar em AWS

O mesmo artefato é usado em `dev`, `homol` e `prod`. Configure:

- `SPRING_PROFILES_ACTIVE`;
- `DB_URL=jdbc:postgresql://<aurora-writer>:5432/nota_fiscal`;
- `DB_USERNAME` e `DB_PASSWORD` por injeção segura do Secrets Manager;
- `AWS_SNS_NOTA_FISCAL_TOPIC_ARN`.

Não configure endpoint do LocalStack nem access/secret key na workload AWS. Use IAM Role. O Aurora fica em subnets privadas e aceita porta 5432 somente do security group da aplicação.

## Provisionamento com Terraform

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars
terraform init
terraform fmt -check
terraform validate
terraform plan
terraform apply
```

O Terraform cria SNS/SQS/DLQs, Secrets Manager e Aurora PostgreSQL Serverless v2. Use os outputs `aurora_writer_endpoint`, `aurora_master_secret_arn` e `nota_fiscal_gerada_topic_arn` na configuração segura do ambiente.
