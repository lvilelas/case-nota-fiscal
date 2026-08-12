# Infraestrutura AWS com Terraform

O módulo provisiona ECR, ALB, ECS/Fargate, IAM, Aurora PostgreSQL Serverless v2, SNS, quatro SQS/DLQs, Secrets Manager, autoscaling, ADOT/X-Ray, CloudWatch Logs, alarmes e dashboard.

## Pré-requisitos de rede

- VPC existente;
- ao menos duas subnets públicas para o ALB;
- ao menos duas subnets privadas para Fargate e Aurora;
- NAT Gateway ou VPC endpoints nas subnets privadas para ECR, Logs, Secrets Manager, SNS e X-Ray;
- certificado ACM para HTTPS, exceto em ambiente efêmero de demonstração.

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
