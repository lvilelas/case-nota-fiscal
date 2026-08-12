locals {
  consumers = toset(["estoque", "registro", "entrega", "financeiro"])
}

resource "aws_sns_topic" "nota_fiscal_gerada" {
  name              = "${var.environment}-nota-fiscal-gerada"
  kms_master_key_id = aws_kms_key.sns.arn
}

resource "aws_sqs_queue" "dead_letter" {
  for_each = local.consumers

  name                      = "${var.environment}-nota-fiscal-${each.key}-dlq"
  message_retention_seconds = 1209600
  sqs_managed_sse_enabled   = true
}

resource "aws_sqs_queue" "consumer" {
  for_each = local.consumers

  name                       = "${var.environment}-nota-fiscal-${each.key}"
  message_retention_seconds  = 345600
  receive_wait_time_seconds  = 20
  visibility_timeout_seconds = 30
  sqs_managed_sse_enabled    = true
}

resource "aws_sqs_queue_redrive_policy" "consumer" {
  for_each = local.consumers

  queue_url = aws_sqs_queue.consumer[each.key].id
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dead_letter[each.key].arn
    maxReceiveCount     = var.max_receive_count
  })
}

resource "aws_sqs_queue_redrive_allow_policy" "dead_letter" {
  for_each = local.consumers

  queue_url = aws_sqs_queue.dead_letter[each.key].id
  redrive_allow_policy = jsonencode({
    redrivePermission = "byQueue"
    sourceQueueArns   = [aws_sqs_queue.consumer[each.key].arn]
  })
}

data "aws_iam_policy_document" "sns_to_sqs" {
  for_each = local.consumers

  statement {
    sid     = "AllowNotaFiscalTopic"
    effect  = "Allow"
    actions = ["sqs:SendMessage"]
    resources = [
      aws_sqs_queue.consumer[each.key].arn
    ]

    principals {
      type        = "Service"
      identifiers = ["sns.amazonaws.com"]
    }

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values   = [aws_sns_topic.nota_fiscal_gerada.arn]
    }
  }
}

resource "aws_sqs_queue_policy" "allow_sns" {
  for_each = local.consumers

  queue_url = aws_sqs_queue.consumer[each.key].id
  policy    = data.aws_iam_policy_document.sns_to_sqs[each.key].json
}

resource "aws_sns_topic_subscription" "consumer" {
  for_each = local.consumers

  depends_on = [aws_sqs_queue_policy.allow_sns]

  topic_arn            = aws_sns_topic.nota_fiscal_gerada.arn
  protocol             = "sqs"
  endpoint             = aws_sqs_queue.consumer[each.key].arn
  raw_message_delivery = true
}

resource "aws_secretsmanager_secret" "application" {
  name                    = "case-nota-fiscal/${var.environment}"
  description             = "Configuracoes sensiveis do gerador de nota fiscal"
  recovery_window_in_days = 7
}

resource "aws_db_subnet_group" "aurora" {
  name        = "${var.environment}-nota-fiscal-aurora"
  description = "Subnets privadas do Aurora do gerador de nota fiscal"
  subnet_ids  = var.private_subnet_ids
}

resource "aws_security_group" "aurora" {
  name        = "${var.environment}-nota-fiscal-aurora"
  description = "Acesso PostgreSQL ao Aurora somente pela aplicacao"
  vpc_id      = var.vpc_id
}

resource "aws_vpc_security_group_ingress_rule" "aurora_from_application" {
  security_group_id            = aws_security_group.aurora.id
  referenced_security_group_id = aws_security_group.ecs_tasks.id
  description                  = "PostgreSQL a partir da workload da aplicacao"
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "tcp"
}

resource "aws_rds_cluster" "nota_fiscal" {
  cluster_identifier          = "${var.environment}-nota-fiscal"
  engine                      = "aurora-postgresql"
  engine_mode                 = "provisioned"
  database_name               = var.database_name
  master_username             = var.database_master_username
  manage_master_user_password = true
  db_subnet_group_name        = aws_db_subnet_group.aurora.name
  vpc_security_group_ids      = [aws_security_group.aurora.id]
  storage_encrypted           = true
  kms_key_id                  = aws_kms_key.aurora.arn
  backup_retention_period     = var.database_backup_retention_days
  preferred_backup_window     = "03:00-04:00"
  deletion_protection         = var.environment == "prod"
  skip_final_snapshot         = var.environment != "prod"
  final_snapshot_identifier   = var.environment == "prod" ? "prod-nota-fiscal-final" : null

  serverlessv2_scaling_configuration {
    min_capacity = var.aurora_min_capacity
    max_capacity = var.aurora_max_capacity
  }
}

resource "aws_rds_cluster_instance" "nota_fiscal" {
  count = var.aurora_instance_count

  identifier                   = "${var.environment}-nota-fiscal-${count.index + 1}"
  cluster_identifier           = aws_rds_cluster.nota_fiscal.id
  instance_class               = "db.serverless"
  engine                       = aws_rds_cluster.nota_fiscal.engine
  engine_version               = aws_rds_cluster.nota_fiscal.engine_version
  db_subnet_group_name         = aws_db_subnet_group.aurora.name
  publicly_accessible          = false
  auto_minor_version_upgrade   = true
  performance_insights_enabled = true
}
