locals {
  application_name = "${var.environment}-nota-fiscal"
  container_port   = 8080
  management_port  = 9090
  adot_config      = <<-YAML
    extensions:
      health_check:
    receivers:
      otlp:
        protocols:
          grpc:
            endpoint: 0.0.0.0:4317
          http:
            endpoint: 0.0.0.0:4318
      prometheus:
        config:
          scrape_configs:
            - job_name: gerador-nota-fiscal
              scrape_interval: 30s
              metrics_path: /actuator/prometheus
              static_configs:
                - targets: [localhost:${local.management_port}]
    processors:
      batch:
      resourcedetection:
        detectors: [env, ecs]
        timeout: 2s
        override: false
    exporters:
      awsxray:
      awsemf/application:
        region: ${var.aws_region}
        namespace: CaseNotaFiscal/Application
        log_group_name: ${aws_cloudwatch_log_group.application_metrics.name}
        log_stream_name: "{TaskId}"
        dimension_rollup_option: NoDimensionRollup
        resource_to_telemetry_conversion:
          enabled: true
        metric_declarations:
          - dimensions: [[application], [application, area, id]]
            metric_name_selectors: ["^jvm_.*", "^process_.*", "^system_.*"]
          - dimensions: [[application, method, status, uri]]
            metric_name_selectors: ["^http_server_requests_seconds_(count|sum|max)$"]
    service:
      extensions: [health_check]
      pipelines:
        traces:
          receivers: [otlp]
          processors: [resourcedetection, batch]
          exporters: [awsxray]
        metrics/application:
          receivers: [prometheus]
          processors: [resourcedetection, batch]
          exporters: [awsemf/application]
  YAML
}

resource "aws_ecr_repository" "application" {
  name                 = local.application_name
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "AES256"
  }
}

resource "aws_ecr_lifecycle_policy" "application" {
  repository = aws_ecr_repository.application.name
  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Manter as ultimas 30 imagens"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 30
      }
      action = { type = "expire" }
    }]
  })
}

resource "aws_security_group" "alb" {
  name        = "${local.application_name}-alb"
  description = "Entrada publica HTTPS/HTTP do gerador de nota fiscal"
  vpc_id      = var.vpc_id
}

resource "aws_vpc_security_group_ingress_rule" "alb_http" {
  security_group_id = aws_security_group.alb.id
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 80
  to_port           = 80
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "alb_https" {
  security_group_id = aws_security_group.alb.id
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 443
  to_port           = 443
  ip_protocol       = "tcp"
}

resource "aws_security_group" "ecs_tasks" {
  name        = "${local.application_name}-tasks"
  description = "Tasks privadas do gerador de nota fiscal"
  vpc_id      = var.vpc_id
}

resource "aws_vpc_security_group_ingress_rule" "tasks_from_alb" {
  security_group_id            = aws_security_group.ecs_tasks.id
  referenced_security_group_id = aws_security_group.alb.id
  from_port                    = local.container_port
  to_port                      = local.container_port
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "alb_to_tasks" {
  security_group_id            = aws_security_group.alb.id
  referenced_security_group_id = aws_security_group.ecs_tasks.id
  from_port                    = local.container_port
  to_port                      = local.container_port
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "tasks_https_outbound" {
  security_group_id = aws_security_group.ecs_tasks.id
  cidr_ipv4         = data.aws_vpc.selected.cidr_block
  description       = "HTTPS para VPC endpoints privados"
  from_port         = 443
  to_port           = 443
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "tasks_s3" {
  security_group_id = aws_security_group.ecs_tasks.id
  prefix_list_id    = data.aws_prefix_list.s3.id
  description       = "HTTPS para S3 pelo gateway endpoint usado pelo ECR"
  from_port         = 443
  to_port           = 443
  ip_protocol       = "tcp"
}

# A conexao com o banco fica restrita ao security group do Aurora.
resource "aws_vpc_security_group_egress_rule" "tasks_to_aurora" {
  security_group_id            = aws_security_group.ecs_tasks.id
  referenced_security_group_id = aws_security_group.aurora.id
  description                  = "PostgreSQL para o Aurora"
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "tasks_dns_udp" {
  security_group_id = aws_security_group.ecs_tasks.id
  cidr_ipv4         = data.aws_vpc.selected.cidr_block
  description       = "Resolucao DNS dentro da VPC"
  from_port         = 53
  to_port           = 53
  ip_protocol       = "udp"
}

resource "aws_vpc_security_group_egress_rule" "tasks_dns_tcp" {
  security_group_id = aws_security_group.ecs_tasks.id
  cidr_ipv4         = data.aws_vpc.selected.cidr_block
  description       = "Fallback DNS TCP dentro da VPC"
  from_port         = 53
  to_port           = 53
  ip_protocol       = "tcp"
}

# O ALB e intencionalmente publico: ele e o ponto de entrada HTTPS da API.
#trivy:ignore:AWS-0053
resource "aws_lb" "application" {
  name                       = substr("${local.application_name}-alb", 0, 32)
  internal                   = false
  load_balancer_type         = "application"
  drop_invalid_header_fields = true
  security_groups            = [aws_security_group.alb.id]
  subnets                    = var.public_subnet_ids
}

resource "aws_lb_target_group" "application" {
  name        = substr("${local.application_name}-tg", 0, 32)
  port        = local.container_port
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = var.vpc_id

  health_check {
    enabled             = true
    path                = "/readyz"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    interval            = 30
    timeout             = 5
    matcher             = "200"
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.application.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "redirect"

    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.application.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = var.acm_certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.application.arn
  }
}

resource "aws_ecs_cluster" "application" {
  name = local.application_name

  setting {
    name  = "containerInsights"
    value = "enhanced"
  }
}

resource "aws_cloudwatch_log_group" "application" {
  name              = "/ecs/${local.application_name}"
  retention_in_days = 30
}

resource "aws_cloudwatch_log_group" "otel" {
  name              = "/ecs/${local.application_name}-otel"
  retention_in_days = 14
}

resource "aws_cloudwatch_log_group" "application_metrics" {
  name              = "/ecs/${local.application_name}/metrics"
  retention_in_days = 14
}

resource "aws_ecs_task_definition" "application" {
  family                   = local.application_name
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = tostring(var.ecs_task_cpu)
  memory                   = tostring(var.ecs_task_memory)
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  container_definitions = jsonencode([
    {
      name      = "application"
      image     = "${aws_ecr_repository.application.repository_url}:${var.container_image_tag}"
      essential = true
      portMappings = [{
        name          = "http"
        containerPort = local.container_port
        hostPort      = local.container_port
        protocol      = "tcp"
        appProtocol   = "http"
      }]
      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = var.environment },
        { name = "AWS_REGION", value = var.aws_region },
        { name = "AWS_SNS_NOTA_FISCAL_TOPIC_ARN", value = aws_sns_topic.nota_fiscal_gerada.arn },
        { name = "AWS_APPLICATION_SECRET_NAME", value = aws_secretsmanager_secret.application.name },
        { name = "DB_URL", value = "jdbc:postgresql://${aws_rds_cluster.nota_fiscal.endpoint}:${aws_rds_cluster.nota_fiscal.port}/${var.database_name}" },
        { name = "OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", value = "http://localhost:4318/v1/traces" },
        { name = "OTEL_TRACES_SAMPLER_ARG", value = tostring(var.otel_sampling_probability) },
        { name = "MANAGEMENT_PORT", value = tostring(local.management_port) },
        { name = "SWAGGER_ENABLED", value = "false" }
      ]
      secrets = [
        { name = "DB_USERNAME", valueFrom = "${aws_rds_cluster.nota_fiscal.master_user_secret[0].secret_arn}:username::" },
        { name = "DB_PASSWORD", valueFrom = "${aws_rds_cluster.nota_fiscal.master_user_secret[0].secret_arn}:password::" }
      ]
      dependsOn = [{ containerName = "aws-otel-collector", condition = "START" }]
      healthCheck = {
        command     = ["CMD-SHELL", "wget -q -O - http://localhost:8080/livez || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }
      stopTimeout = 30
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.application.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "application"
        }
      }
    },
    {
      name      = "aws-otel-collector"
      image     = "public.ecr.aws/aws-observability/aws-otel-collector:v0.48.0"
      essential = false
      environment = [{
        name  = "AOT_CONFIG_CONTENT"
        value = local.adot_config
      }]
      portMappings = [{
        containerPort = 4318
        hostPort      = 4318
        protocol      = "tcp"
      }]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.otel.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "collector"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "application" {
  name                               = local.application_name
  cluster                            = aws_ecs_cluster.application.id
  task_definition                    = aws_ecs_task_definition.application.arn
  desired_count                      = var.ecs_desired_count
  launch_type                        = "FARGATE"
  platform_version                   = "LATEST"
  health_check_grace_period_seconds  = 90
  enable_ecs_managed_tags            = true
  propagate_tags                     = "SERVICE"
  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.application.arn
    container_name   = "application"
    container_port   = local.container_port
  }

  depends_on = [aws_lb_listener.https]
}

resource "aws_appautoscaling_target" "ecs" {
  max_capacity       = var.ecs_max_capacity
  min_capacity       = var.ecs_min_capacity
  resource_id        = "service/${aws_ecs_cluster.application.name}/${aws_ecs_service.application.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "ecs_cpu" {
  name               = "${local.application_name}-cpu"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs.service_namespace

  target_tracking_scaling_policy_configuration {
    target_value       = 60
    scale_in_cooldown  = 120
    scale_out_cooldown = 60

    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
  }
}
