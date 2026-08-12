variable "environment" {
  description = "Ambiente no qual os recursos serao provisionados."
  type        = string

  validation {
    condition     = contains(["dev", "homol", "prod"], var.environment)
    error_message = "environment deve ser dev, homol ou prod."
  }
}

variable "aws_region" {
  description = "Regiao AWS dos recursos."
  type        = string
  default     = "us-east-1"
}

variable "max_receive_count" {
  description = "Quantidade de recebimentos antes de enviar a mensagem para a DLQ."
  type        = number
  default     = 3

  validation {
    condition     = var.max_receive_count >= 1
    error_message = "max_receive_count deve ser maior ou igual a 1."
  }
}

variable "sns_kms_key_id" {
  description = "Chave KMS usada para criptografar o topico SNS."
  type        = string
  default     = "alias/aws/sns"
}

variable "vpc_id" {
  description = "VPC onde o Aurora sera provisionado."
  type        = string
}

variable "private_subnet_ids" {
  description = "Subnets privadas em pelo menos duas zonas de disponibilidade."
  type        = list(string)

  validation {
    condition     = length(var.private_subnet_ids) >= 2
    error_message = "private_subnet_ids deve possuir pelo menos duas subnets."
  }
}

variable "public_subnet_ids" {
  description = "Subnets publicas em pelo menos duas zonas para o Application Load Balancer."
  type        = list(string)

  validation {
    condition     = length(var.public_subnet_ids) >= 2
    error_message = "public_subnet_ids deve possuir pelo menos duas subnets."
  }
}

variable "acm_certificate_arn" {
  description = "Certificado ACM do listener HTTPS. Vazio habilita apenas HTTP para ambientes efemeros."
  type        = string
  default     = ""
}

variable "container_image_tag" {
  description = "Tag da imagem implantada no ECS. O pipeline publica latest e uma tag imutavel pelo SHA."
  type        = string
  default     = "latest"
}

variable "ecs_task_cpu" {
  description = "CPU da task Fargate."
  type        = number
  default     = 512
}

variable "ecs_task_memory" {
  description = "Memoria em MiB da task Fargate."
  type        = number
  default     = 1024
}

variable "ecs_desired_count" {
  description = "Quantidade desejada inicial de tasks."
  type        = number
  default     = 2
}

variable "ecs_min_capacity" {
  description = "Quantidade minima de tasks no autoscaling."
  type        = number
  default     = 2
}

variable "ecs_max_capacity" {
  description = "Quantidade maxima de tasks no autoscaling."
  type        = number
  default     = 6
}

variable "otel_sampling_probability" {
  description = "Probabilidade de amostragem dos traces em producao."
  type        = number
  default     = 0.1

  validation {
    condition     = var.otel_sampling_probability >= 0 && var.otel_sampling_probability <= 1
    error_message = "otel_sampling_probability deve estar entre 0 e 1."
  }
}

variable "alarm_email" {
  description = "Email opcional para receber alarmes CloudWatch."
  type        = string
  default     = ""
}

variable "github_repository" {
  description = "Repositorio no formato organizacao/repositorio para criar a role OIDC do pipeline."
  type        = string
  default     = ""
}

variable "github_oidc_provider_arn" {
  description = "ARN de um provider OIDC token.actions.githubusercontent.com ja existente na conta."
  type        = string
  default     = ""
}

variable "database_name" {
  description = "Nome do banco da aplicacao."
  type        = string
  default     = "nota_fiscal"
}

variable "database_master_username" {
  description = "Usuario master cujo password sera gerenciado pelo RDS no Secrets Manager."
  type        = string
  default     = "nota_fiscal_admin"
}

variable "aurora_min_capacity" {
  description = "Capacidade minima em ACUs do Aurora Serverless v2."
  type        = number
  default     = 0.5
}

variable "aurora_max_capacity" {
  description = "Capacidade maxima em ACUs do Aurora Serverless v2."
  type        = number
  default     = 4
}

variable "aurora_instance_count" {
  description = "Quantidade de instancias Serverless v2. Use pelo menos duas em producao para failover."
  type        = number
  default     = 1

  validation {
    condition     = var.aurora_instance_count >= 1
    error_message = "aurora_instance_count deve ser maior ou igual a 1."
  }
}

variable "database_backup_retention_days" {
  description = "Retencao de backups do Aurora em dias."
  type        = number
  default     = 7
}
