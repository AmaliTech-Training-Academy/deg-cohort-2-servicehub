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
  name = "servicehub-prod"
  tags = { Project = "servicehub", Environment = "prod", ManagedBy = "terraform" }
}

module "vpc" {
  source               = "../../modules/vpc"
  name                 = local.name
  cidr                 = "10.1.0.0/16"
  azs                  = ["${var.region}a", "${var.region}b"]
  public_subnet_cidrs  = ["10.1.1.0/24", "10.1.2.0/24"]
  private_subnet_cidrs = ["10.1.3.0/24", "10.1.4.0/24"]
  tags                 = local.tags
}

module "rds" {
  source         = "../../modules/rds"
  name           = local.name
  vpc_id         = module.vpc.vpc_id
  subnet_ids     = module.vpc.private_subnet_ids
  allowed_sg_ids = [module.ecs.backend_sg_id]
  instance_class = "db.t3.micro"
  db_name        = var.db_name
  db_username    = var.db_username
  db_password    = var.db_password
  tags           = local.tags
}

module "bastion" {
  source           = "../../modules/bastion"
  project          = local.name
  vpc_id           = module.vpc.vpc_id
  public_subnet_id = module.vpc.public_subnet_ids[0]
  rds_sg_id        = module.rds.sg_id
  key_name         = var.key_name
  allowed_cidr     = "0.0.0.0/0"
  tags             = local.tags
}

module "ecs" {
  source            = "../../modules/ecs"
  name              = local.name
  region            = var.region
  vpc_id            = module.vpc.vpc_id
  public_subnet_ids = module.vpc.public_subnet_ids
  db_address        = module.rds.address
  db_name           = var.db_name
  db_username       = var.db_username
  db_password       = var.db_password
  jwt_secret        = var.jwt_secret
  tags              = local.tags
}
