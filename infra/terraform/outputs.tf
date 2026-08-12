output "nota_fiscal_gerada_topic_arn" {
  description = "ARN que deve ser configurado em AWS_SNS_NOTA_FISCAL_TOPIC_ARN."
  value       = aws_sns_topic.nota_fiscal_gerada.arn
}

output "sns_kms_key_arn" {
  description = "ARN da chave KMS customer-managed usada pelos topicos SNS."
  value       = aws_kms_key.sns.arn
}

output "aurora_kms_key_arn" {
  description = "ARN da chave KMS customer-managed usada pelo Aurora."
  value       = aws_kms_key.aurora.arn
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

output "aurora_writer_endpoint" {
  description = "Endpoint de escrita usado em DB_URL."
  value       = aws_rds_cluster.nota_fiscal.endpoint
}

output "aurora_reader_endpoint" {
  description = "Endpoint de leitura do cluster Aurora."
  value       = aws_rds_cluster.nota_fiscal.reader_endpoint
}

output "aurora_port" {
  description = "Porta PostgreSQL do Aurora."
  value       = aws_rds_cluster.nota_fiscal.port
}

output "aurora_master_secret_arn" {
  description = "Secret gerenciado pelo RDS com as credenciais master do Aurora."
  value       = aws_rds_cluster.nota_fiscal.master_user_secret[0].secret_arn
  sensitive   = true
}

output "ecr_repository_url" {
  description = "Repositorio que recebe a imagem da aplicacao."
  value       = aws_ecr_repository.application.repository_url
}

output "ecs_cluster_name" {
  description = "Cluster usado pelo workflow de deploy."
  value       = aws_ecs_cluster.application.name
}

output "ecs_service_name" {
  description = "Servico usado pelo workflow de deploy."
  value       = aws_ecs_service.application.name
}

output "application_url" {
  description = "URL publica do Application Load Balancer."
  value       = "https://${aws_lb.application.dns_name}"
}

output "cloudwatch_dashboard_name" {
  description = "Dashboard operacional da aplicacao."
  value       = aws_cloudwatch_dashboard.application.dashboard_name
}

output "github_actions_role_arn" {
  description = "Role OIDC configurada em AWS_ROLE_ARN no GitHub."
  value       = length(aws_iam_role.github_actions) == 0 ? null : aws_iam_role.github_actions[0].arn
}
