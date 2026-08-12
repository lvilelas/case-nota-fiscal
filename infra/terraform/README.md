# Infraestrutura AWS com Terraform

O módulo provisiona ECR, ALB, ECS/Fargate, IAM, Aurora PostgreSQL Serverless v2, SNS, quatro SQS/DLQs, Secrets Manager, KMS, autoscaling, ADOT/X-Ray, CloudWatch Logs, alarmes e dashboard.

## Pré-requisitos de rede

- VPC existente;
- ao menos duas subnets públicas para o ALB;
- ao menos duas subnets privadas para Fargate e Aurora;
- interface VPC endpoints nas subnets privadas para ECR API/DKR, Logs, Secrets Manager, SNS, X-Ray e KMS;
- gateway VPC endpoint para S3, necessário para baixar as camadas das imagens do ECR;
- certificado ACM para HTTPS; HTTP existe somente para redirecionar para TLS.

## Execução

```bash
cp terraform.tfvars.example terraform.tfvars
terraform init
terraform fmt -check
terraform validate
terraform plan
terraform apply
```

Para produção use o arquivo dedicado, que configura duas instâncias Aurora Serverless v2. A validação do módulo rejeita qualquer plano `prod` com menos de duas instâncias:

```bash
terraform plan -var-file=environments/prod.tfvars
terraform apply -var-file=environments/prod.tfvars
```

Copie `environments/prod.tfvars.example` para `environments/prod.tfvars` e preencha os identificadores reais. Duas instâncias permitem failover do writer; o cluster continua usando subnets privadas em pelo menos duas zonas de disponibilidade.

As credenciais master do Aurora são criadas pelo RDS e armazenadas no Secrets Manager. O ECS injeta somente `username` e `password`; nenhuma senha é armazenada no Terraform ou na task definition.

Os tópicos de eventos e alarmes usam uma chave KMS customer-managed exclusiva por ambiente, com rotação habilitada e janela de exclusão de 30 dias. A policy permite administração pela conta e uso criptográfico somente pelo SNS e pelo CloudWatch da própria conta; a task da aplicação recebe apenas `Decrypt` e `GenerateDataKey` nessa chave. O Aurora usa outra CMK, isolando dados e mensageria. Essa escolha elimina chaves AWS-managed nesses recursos, mas gera o custo mensal de duas CMKs quando a infraestrutura for aplicada na AWS.

O ALB é público por decisão arquitetural, pois representa a entrada da API; o finding `AWS-0053` é suprimido somente nesse recurso e documentado ao lado dele. Headers inválidos são descartados, a porta 80 apenas redireciona para HTTPS e o certificado ACM é obrigatório. As tasks não possuem mais egress irrestrito: acessam HTTPS somente dentro do CIDR da VPC, S3 pela prefix list gerenciada, DNS dentro da VPC e PostgreSQL exclusivamente para o security group do Aurora. Por isso, os VPC endpoints listados acima passam a ser obrigatórios, não apenas uma alternativa ao NAT.

## IAM

- `ecs_execution`: pull no ECR, logs e injeção do secret do banco.
- `ecs_task`: `sns:Publish`, leitura do secret da aplicação, exportação ao X-Ray e escrita EMF no log group de métricas.
- `github_actions`: role opcional deixada apenas como proposta de uma futura etapa de CD; ela não é utilizada pela pipeline educacional atual.

Para criar a role do pipeline, informe `github_repository` e o ARN do provider OIDC já existente na conta. Um provider OIDC é compartilhado por conta e, por isso, não é criado automaticamente por este módulo.

## GitHub Actions

A action atual é somente de integração contínua e não precisa de variáveis, secrets ou ambiente AWS. Ela executa build, testes, cobertura, secret scanning, análise de vulnerabilidades e build/scan local das imagens com `push: false`.

A infraestrutura de ECR/ECS e a role OIDC permanecem no Terraform para demonstrar como um CD futuro poderia ser construído, mas não há workflow que as utilize. Um eventual deploy deve ficar em workflow separado, vinculado a um GitHub Environment protegido e habilitado somente quando existir uma conta AWS de destino.

## Observabilidade

O ADOT sidecar recebe OTLP da aplicação, envia traces ao X-Ray, coleta o endpoint Prometheus interno e publica métricas JVM/processo/HTTP no CloudWatch via EMF. `awslogs` envia logs ao CloudWatch; Container Insights fornece CPU/memória. Alarmes cobrem CPU, memória, latência, 5xx, idade das filas, DLQ e falha definitiva da outbox.

Consulte [RUNBOOK.md](../../docs/RUNBOOK.md) antes de redrive, replay ou rollback.
