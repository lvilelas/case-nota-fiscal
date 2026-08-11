locals {
  consumers = toset(["estoque", "registro", "entrega", "financeiro"])
}

resource "aws_sns_topic" "nota_fiscal_gerada" {
  name              = "${var.environment}-nota-fiscal-gerada"
  kms_master_key_id = var.sns_kms_key_id
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
    sid       = "AllowNotaFiscalTopic"
    effect    = "Allow"
    actions   = ["sqs:SendMessage"]
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
