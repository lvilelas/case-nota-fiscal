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
