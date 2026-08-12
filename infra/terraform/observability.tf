resource "aws_sns_topic" "alarms" {
  name              = "${local.application_name}-alarms"
  kms_master_key_id = var.sns_kms_key_id
}

resource "aws_sns_topic_subscription" "alarm_email" {
  count = var.alarm_email == "" ? 0 : 1

  topic_arn = aws_sns_topic.alarms.arn
  protocol  = "email"
  endpoint  = var.alarm_email
}

resource "aws_cloudwatch_metric_alarm" "ecs_cpu" {
  alarm_name          = "${local.application_name}-high-cpu"
  alarm_description   = "CPU media acima de 80% por cinco minutos. Consulte docs/RUNBOOK.md."
  namespace           = "AWS/ECS"
  metric_name         = "CPUUtilization"
  statistic           = "Average"
  period              = 60
  evaluation_periods  = 5
  datapoints_to_alarm = 3
  threshold           = 80
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alarms.arn]

  dimensions = {
    ClusterName = aws_ecs_cluster.application.name
    ServiceName = aws_ecs_service.application.name
  }
}

resource "aws_cloudwatch_metric_alarm" "ecs_memory" {
  alarm_name          = "${local.application_name}-high-memory"
  alarm_description   = "Memoria media acima de 80% por cinco minutos. Consulte docs/RUNBOOK.md."
  namespace           = "AWS/ECS"
  metric_name         = "MemoryUtilization"
  statistic           = "Average"
  period              = 60
  evaluation_periods  = 5
  datapoints_to_alarm = 3
  threshold           = 80
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alarms.arn]

  dimensions = {
    ClusterName = aws_ecs_cluster.application.name
    ServiceName = aws_ecs_service.application.name
  }
}

resource "aws_cloudwatch_metric_alarm" "target_errors" {
  alarm_name          = "${local.application_name}-http-5xx"
  alarm_description   = "O ALB observou cinco ou mais respostas 5xx em cinco minutos."
  namespace           = "AWS/ApplicationELB"
  metric_name         = "HTTPCode_Target_5XX_Count"
  statistic           = "Sum"
  period              = 60
  evaluation_periods  = 5
  datapoints_to_alarm = 1
  threshold           = 5
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alarms.arn]

  dimensions = {
    LoadBalancer = aws_lb.application.arn_suffix
    TargetGroup  = aws_lb_target_group.application.arn_suffix
  }
}

resource "aws_cloudwatch_metric_alarm" "target_latency" {
  alarm_name          = "${local.application_name}-high-latency"
  alarm_description   = "Latencia media do target acima de um segundo."
  namespace           = "AWS/ApplicationELB"
  metric_name         = "TargetResponseTime"
  statistic           = "Average"
  period              = 60
  evaluation_periods  = 5
  datapoints_to_alarm = 3
  threshold           = 1
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alarms.arn]

  dimensions = {
    LoadBalancer = aws_lb.application.arn_suffix
    TargetGroup  = aws_lb_target_group.application.arn_suffix
  }
}

resource "aws_cloudwatch_metric_alarm" "queue_age" {
  for_each = local.consumers

  alarm_name          = "${local.application_name}-${each.key}-queue-age"
  alarm_description   = "Mensagem mais antiga da fila supera cinco minutos."
  namespace           = "AWS/SQS"
  metric_name         = "ApproximateAgeOfOldestMessage"
  statistic           = "Maximum"
  period              = 60
  evaluation_periods  = 5
  datapoints_to_alarm = 3
  threshold           = 300
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alarms.arn]

  dimensions = {
    QueueName = aws_sqs_queue.consumer[each.key].name
  }
}

resource "aws_cloudwatch_metric_alarm" "dlq_messages" {
  for_each = local.consumers

  alarm_name          = "${local.application_name}-${each.key}-dlq-not-empty"
  alarm_description   = "A DLQ possui mensagem e requer triagem/replay."
  namespace           = "AWS/SQS"
  metric_name         = "ApproximateNumberOfMessagesVisible"
  statistic           = "Maximum"
  period              = 60
  evaluation_periods  = 1
  threshold           = 0
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alarms.arn]

  dimensions = {
    QueueName = aws_sqs_queue.dead_letter[each.key].name
  }
}

resource "aws_cloudwatch_log_metric_filter" "outbox_failure" {
  name           = "${local.application_name}-outbox-failure"
  log_group_name = aws_cloudwatch_log_group.application.name
  pattern        = "\"Evento da outbox esgotou tentativas\""

  metric_transformation {
    name      = "OutboxPermanentFailures"
    namespace = "CaseNotaFiscal"
    value     = "1"
  }
}

resource "aws_cloudwatch_metric_alarm" "outbox_failure" {
  alarm_name          = "${local.application_name}-outbox-permanent-failure"
  alarm_description   = "Um evento da outbox esgotou os retries. Execute o runbook de replay."
  namespace           = "CaseNotaFiscal"
  metric_name         = "OutboxPermanentFailures"
  statistic           = "Sum"
  period              = 60
  evaluation_periods  = 1
  threshold           = 1
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alarms.arn]
}

resource "aws_cloudwatch_dashboard" "application" {
  dashboard_name = local.application_name
  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "metric"
        x      = 0
        y      = 0
        width  = 12
        height = 6
        properties = {
          title  = "ECS CPU e memoria"
          region = var.aws_region
          period = 60
          metrics = [
            ["AWS/ECS", "CPUUtilization", "ClusterName", aws_ecs_cluster.application.name, "ServiceName", aws_ecs_service.application.name],
            [".", "MemoryUtilization", ".", ".", ".", "."]
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 0
        width  = 12
        height = 6
        properties = {
          title  = "HTTP latencia e erros"
          region = var.aws_region
          period = 60
          metrics = [
            ["AWS/ApplicationELB", "TargetResponseTime", "LoadBalancer", aws_lb.application.arn_suffix, "TargetGroup", aws_lb_target_group.application.arn_suffix],
            [".", "HTTPCode_Target_5XX_Count", ".", ".", ".", ".", { stat = "Sum", yAxis = "right" }]
          ]
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 6
        width  = 12
        height = 6
        properties = {
          title  = "JVM e processo"
          region = var.aws_region
          period = 60
          metrics = [
            ["CaseNotaFiscal/Application", "process_cpu_usage", "application", "gerador-nota-fiscal"],
            [".", "jvm_memory_used_bytes", ".", ".", "area", "heap", "id", "G1 Old Gen", { yAxis = "right" }]
          ]
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 12
        width  = 24
        height = 6
        properties = {
          title  = "Idade das filas SQS"
          region = var.aws_region
          period = 60
          metrics = [for name, queue in aws_sqs_queue.consumer :
            ["AWS/SQS", "ApproximateAgeOfOldestMessage", "QueueName", queue.name, { label = name }]
          ]
        }
      }
    ]
  })
}
