terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 5.0" }
  }
}

provider "aws" {
  region = var.region
}

locals {
  name = "servicehub-dev"
  tags = { Project = "servicehub", Environment = "dev", ManagedBy = "terraform" }
}

module "vpc" {
  source               = "../../modules/vpc"
  name                 = local.name
  cidr                 = "10.0.0.0/16"
  azs                  = ["${var.region}a", "${var.region}b"]
  public_subnet_cidrs  = ["10.0.1.0/24", "10.0.2.0/24"]
  private_subnet_cidrs = ["10.0.3.0/24", "10.0.4.0/24"]
  tags                 = local.tags
}

module "rds" {
  source         = "../../modules/rds"
  name           = local.name
  vpc_id         = module.vpc.vpc_id
  subnet_ids     = module.vpc.private_subnet_ids
  allowed_sg_ids = [module.ec2.sg_id]
  instance_class = "db.t3.micro"
  db_name        = var.db_name
  db_username    = var.db_username
  db_password    = var.db_password
  tags           = local.tags
}

module "ec2" {
  source        = "../../modules/ec2"
  name          = local.name
  vpc_id        = module.vpc.vpc_id
  subnet_id     = module.vpc.public_subnet_ids[0]
  instance_type = "t3.small"
  key_name      = var.key_name
  tags          = local.tags
}
