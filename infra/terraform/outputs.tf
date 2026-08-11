output "nota_fiscal_gerada_topic_arn" {
  description = "ARN que deve ser configurado em AWS_SNS_NOTA_FISCAL_TOPIC_ARN."
  value       = aws_sns_topic.nota_fiscal_gerada.arn
}

output "consumer_queue_urls" {
  description = "URLs das filas dos consumidores."
  value       = { for name, queue in aws_sqs_queue.consumer : name => queue.id }
}

output "consumer_queue_arns" {
  description = "ARNs das filas dos consumidores."
  value       = { for name, queue in aws_sqs_queue.consumer : name => queue.arn }
}

output "dead_letter_queue_arns" {
  description = "ARNs das DLQs."
  value       = { for name, queue in aws_sqs_queue.dead_letter : name => queue.arn }
}

output "application_secret_arn" {
  description = "ARN do secret do ambiente."
  value       = aws_secretsmanager_secret.application.arn
}
