# Infraestrutura AWS com Terraform

Este módulo provisiona:

- um tópico SNS `NotaFiscalGerada`;
- quatro filas SQS inscritas e uma DLQ por consumidor;
- um secret da aplicação;
- Aurora PostgreSQL Serverless v2 em subnets privadas;
- security group permitindo PostgreSQL somente a partir da aplicação;
- credencial master gerenciada pelo RDS no Secrets Manager.

O cluster usa `engine_mode = "provisioned"` e instâncias `db.serverless`, configuração exigida pelo Aurora Serverless v2. Para produção, configure ao menos duas instâncias em zonas distintas para failover.

## Execução

```bash
cp terraform.tfvars.example terraform.tfvars
terraform init
terraform fmt -check
terraform validate
terraform plan
terraform apply
```

O `terraform.tfvars` deve informar a VPC, ao menos duas subnets privadas e o security group da workload. Não coloque senhas no arquivo: `manage_master_user_password` delega sua criação e rotação inicial ao RDS/Secrets Manager.

Depois do provisionamento:

- monte `DB_URL` com `aurora_writer_endpoint` e `aurora_port`;
- injete usuário/senha a partir de `aurora_master_secret_arn` por pipeline ou integração da plataforma;
- configure `AWS_SNS_NOTA_FISCAL_TOPIC_ARN` com `nota_fiscal_gerada_topic_arn`;
- conceda à role da aplicação somente as permissões necessárias.

O secret master é suficiente para a prova, mas uma evolução recomendada é provisionar um usuário de aplicação com privilégios mínimos e separar a identidade que executa migrations da identidade de runtime.

Para state remoto, copie `backend.tf.example` e `backend.hcl.example`, ajuste bucket/chave e use `terraform init -backend-config=backend.hcl`. O bucket deve ter versionamento, criptografia e bloqueio de acesso público.
