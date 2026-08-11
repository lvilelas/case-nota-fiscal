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
