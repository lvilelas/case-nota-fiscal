# Execução local da infraestrutura AWS

## Docker Compose

Suba SNS, SQS, DLQs e Secrets Manager:

```bash
docker compose up -d
docker compose ps
```

O script de inicialização cria:

- tópico `nota-fiscal-gerada`;
- filas `nota-fiscal-{estoque|registro|entrega|financeiro}`;
- uma fila `-dlq` para cada consumidor;
- subscriptions SNS com raw message delivery;
- secret fictício `case-nota-fiscal/local`.

Execute a aplicação com o profile local (ele já é o profile padrão):

```bash
./mvnw spring-boot:run
```

O mesmo código é usado na AWS. Em `dev`, `homol` ou `prod`, configure `SPRING_PROFILES_ACTIVE`, `AWS_SNS_NOTA_FISCAL_TOPIC_ARN` e, se necessário, `AWS_APPLICATION_SECRET_NAME`. Não configure `AWS_ENDPOINT_URL`, access key ou secret key na workload AWS; conceda permissões por IAM Role.

## Testes

```bash
./mvnw verify
```

Com Docker disponível, Testcontainers inicia seu próprio LocalStack isolado. Sem Docker, os testes de infraestrutura são ignorados e os testes unitários continuam sendo executados.

O LocalStack está fixado em `4.14.0` para uma prova local reproduzível sem dependência de conta. Antes de atualizar a imagem, revise os termos e requisitos de autenticação da versão escolhida.

## Provisionamento AWS com Terraform

O Terraform em `infra/terraform` cria em `dev`, `homol` ou `prod` os mesmos recursos usados localmente, com criptografia gerenciada habilitada:

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars
terraform init
terraform fmt -check
terraform validate
terraform plan
terraform apply
```

Para usar state remoto, copie `backend.tf.example` para `backend.tf` e `backend.hcl.example` para `backend.hcl`, ajuste bucket e chave e inicialize com `terraform init -backend-config=backend.hcl`. O bucket de state deve ter versionamento, criptografia, bloqueio de acesso público e permissões restritas. Esses arquivos não devem ser commitados quando contiverem informações específicas da conta.

Use o output `nota_fiscal_gerada_topic_arn` como `AWS_SNS_NOTA_FISCAL_TOPIC_ARN`. A role da aplicação precisa somente de `sns:Publish` nesse tópico e `secretsmanager:GetSecretValue` no secret do ambiente.

O Terraform cria somente o container do secret. O valor deve ser inserido por pipeline ou processo operacional seguro para que não apareça no repositório nem no state do Terraform.
