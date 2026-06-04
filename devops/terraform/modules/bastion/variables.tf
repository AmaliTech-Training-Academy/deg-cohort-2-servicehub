variable "project" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "public_subnet_id" {
  type = string
}

variable "rds_sg_id" {
  type        = string
  description = "RDS security group ID to allow bastion access"
}

variable "key_name" {
  type = string
}

variable "allowed_cidr" {
  type        = string
  description = "CIDR allowed to SSH into bastion (use 0.0.0.0/0 or restrict to your team's IPs)"
  default     = "0.0.0.0/0"
}

variable "tags" {
  type    = map(string)
  default = {}
}
