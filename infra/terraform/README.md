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

As credenciais master do Aurora são criadas pelo RDS e armazenadas no Secrets Manager. O ECS injeta somente `username` e `password`; nenhuma senha é armazenada no Terraform ou na task definition.

## IAM

- `ecs_execution`: pull no ECR, logs e injeção do secret do banco.
- `ecs_task`: `sns:Publish`, leitura do secret da aplicação, exportação ao X-Ray e escrita EMF no log group de métricas.
- `github_actions`: opcional, assumida por OIDC somente pela branch `main`, com push no ECR, registro de task definition e deploy do serviço.

Para criar a role do pipeline, informe `github_repository` e o ARN do provider OIDC já existente na conta. Um provider OIDC é compartilhado por conta e, por isso, não é criado automaticamente por este módulo.

## GitHub Actions

Configure como Repository Variables:

- `AWS_ROLE_ARN`: output `github_actions_role_arn`;
- `AWS_REGION`;
- `ECR_REPOSITORY`: nome retornado pelo ECR, por exemplo `prod-nota-fiscal`;
- `ECS_CLUSTER`: output `ecs_cluster_name`;
- `ECS_SERVICE`: output `ecs_service_name`.

O pipeline publica `latest` e uma tag imutável pelo SHA. Cada deploy registra uma nova revisão da task definition apontando para o SHA; assim auditoria e rollback não dependem de uma tag mutável.

## Observabilidade

O ADOT sidecar recebe OTLP da aplicação, envia traces ao X-Ray, coleta o endpoint Prometheus interno e publica métricas JVM/processo/HTTP no CloudWatch via EMF. `awslogs` envia logs ao CloudWatch; Container Insights fornece CPU/memória. Alarmes cobrem CPU, memória, latência, 5xx, idade das filas, DLQ e falha definitiva da outbox.

Consulte [RUNBOOK.md](../../docs/RUNBOOK.md) antes de redrive, replay ou rollback.
