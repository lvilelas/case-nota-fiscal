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

variable "application_security_group_id" {
  description = "Security group da workload autorizada a acessar o Aurora."
  type        = string
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
