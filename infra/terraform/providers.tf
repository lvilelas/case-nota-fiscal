provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Application = "case-nota-fiscal"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

data "aws_vpc" "selected" {
  id = var.vpc_id
}

data "aws_prefix_list" "s3" {
  name = "com.amazonaws.${var.aws_region}.s3"
}
