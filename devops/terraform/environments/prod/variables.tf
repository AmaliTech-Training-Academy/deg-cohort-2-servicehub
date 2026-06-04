variable "region" {
  type    = string
  default = "eu-west-1"
}

variable "key_name" {
  type        = string
  description = "EC2 key pair name for bastion host"
}

variable "db_name" {
  type    = string
  default = "servicehub"
}

variable "db_username" {
  type    = string
  default = "servicehub"
}

variable "db_password" {
  type      = string
  sensitive = true
}

variable "jwt_secret" {
  type      = string
  sensitive = true
}
