data "aws_iam_policy_document" "ecs_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

data "aws_caller_identity" "current" {}

data "aws_partition" "current" {}

resource "aws_iam_role" "ecs_execution" {
  name               = "${local.application_name}-ecs-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume_role.json
}

resource "aws_iam_role_policy_attachment" "ecs_execution" {
  role       = aws_iam_role.ecs_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

data "aws_iam_policy_document" "ecs_execution_secrets" {
  statement {
    actions   = ["secretsmanager:GetSecretValue"]
    resources = [aws_rds_cluster.nota_fiscal.master_user_secret[0].secret_arn]
  }
}

resource "aws_iam_role_policy" "ecs_execution_secrets" {
  name   = "read-database-secret"
  role   = aws_iam_role.ecs_execution.id
  policy = data.aws_iam_policy_document.ecs_execution_secrets.json
}

resource "aws_iam_role" "ecs_task" {
  name               = "${local.application_name}-ecs-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume_role.json
}

data "aws_iam_policy_document" "ecs_task" {
  statement {
    sid       = "PublishNotaFiscal"
    actions   = ["sns:Publish"]
    resources = [aws_sns_topic.nota_fiscal_gerada.arn]
  }

  statement {
    sid = "EncryptNotaFiscalTopic"
    actions = [
      "kms:Decrypt",
      "kms:GenerateDataKey*"
    ]
    resources = [aws_kms_key.sns.arn]
  }

  statement {
    sid       = "ReadApplicationSecret"
    actions   = ["secretsmanager:GetSecretValue"]
    resources = [aws_secretsmanager_secret.application.arn]
  }

  statement {
    sid       = "ExportTraces"
    actions   = ["xray:PutTraceSegments", "xray:PutTelemetryRecords"]
    resources = ["*"]
  }

  statement {
    sid = "PublishApplicationMetrics"
    actions = [
      "logs:CreateLogStream",
      "logs:DescribeLogStreams",
      "logs:PutLogEvents"
    ]
    resources = ["${aws_cloudwatch_log_group.application_metrics.arn}:*"]
  }
}

resource "aws_iam_role_policy" "ecs_task" {
  name   = "application-least-privilege"
  role   = aws_iam_role.ecs_task.id
  policy = data.aws_iam_policy_document.ecs_task.json
}

data "aws_iam_policy_document" "github_assume_role" {
  count = var.github_repository != "" && var.github_oidc_provider_arn != "" ? 1 : 0

  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    principals {
      type        = "Federated"
      identifiers = [var.github_oidc_provider_arn]
    }
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:${var.github_repository}:ref:refs/heads/main"]
    }
  }
}

resource "aws_iam_role" "github_actions" {
  count = var.github_repository != "" && var.github_oidc_provider_arn != "" ? 1 : 0

  name               = "${local.application_name}-github-actions"
  assume_role_policy = data.aws_iam_policy_document.github_assume_role[0].json
}

data "aws_iam_policy_document" "github_actions" {
  count = var.github_repository != "" && var.github_oidc_provider_arn != "" ? 1 : 0

  statement {
    sid       = "EcrLogin"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid = "PushImage"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:CompleteLayerUpload",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart"
    ]
    resources = [aws_ecr_repository.application.arn]
  }

  statement {
    sid       = "DescribeService"
    actions   = ["ecs:DescribeServices"]
    resources = ["*"]
  }

  statement {
    sid     = "DeployService"
    actions = ["ecs:UpdateService"]
    resources = [
      "arn:${data.aws_partition.current.partition}:ecs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:service/${aws_ecs_cluster.application.name}/${aws_ecs_service.application.name}"
    ]
  }

  statement {
    sid = "RegisterTaskDefinition"
    actions = [
      "ecs:DescribeTaskDefinition",
      "ecs:RegisterTaskDefinition"
    ]
    resources = ["*"]
  }

  statement {
    sid       = "PassEcsRoles"
    actions   = ["iam:PassRole"]
    resources = [aws_iam_role.ecs_execution.arn, aws_iam_role.ecs_task.arn]
    condition {
      test     = "StringEquals"
      variable = "iam:PassedToService"
      values   = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role_policy" "github_actions" {
  count = var.github_repository != "" && var.github_oidc_provider_arn != "" ? 1 : 0

  name   = "publish-and-deploy"
  role   = aws_iam_role.github_actions[0].id
  policy = data.aws_iam_policy_document.github_actions[0].json
}
