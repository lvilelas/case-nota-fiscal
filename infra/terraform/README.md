# Infraestrutura AWS com Terraform

Este módulo provisiona um tópico SNS `NotaFiscalGerada`, quatro filas SQS inscritas, uma DLQ por fila e o container do secret da aplicação.

O `for_each` usa as chaves `estoque`, `registro`, `entrega` e `financeiro`. Acrescentar um consumidor ao conjunto cria sua fila, DLQ, redrive policies, policy SNS para SQS e subscription.

## Execução com state local

```bash
cp terraform.tfvars.example terraform.tfvars
terraform init
terraform fmt -check
terraform validate
terraform plan
terraform apply
```

## State remoto em S3

```bash
cp backend.tf.example backend.tf
cp backend.hcl.example backend.hcl
# ajuste o bucket e a chave antes de continuar
terraform init -backend-config=backend.hcl
```

O bucket de state deve existir antes do `terraform init` e ter versionamento, criptografia e bloqueio de acesso público. `backend.tf`, `backend.hcl`, `terraform.tfvars` e arquivos de state locais não devem ser commitados.

O recurso `aws_secretsmanager_secret` cria somente o container. O valor do secret deve ser preenchido fora do Terraform para não ser armazenado no state.
