output "bastion_ip" {
  value       = module.bastion.public_ip
  description = "SSH tunnel: ssh -L 5432:<rds-endpoint>:5432 ec2-user@<bastion-ip> -i key.pem -N"
}

output "alb_url" {
  value = "http://${module.ecs.alb_dns_name}"
}

output "backend_ecr_url" {
  value = module.ecs.backend_ecr_url
}

output "frontend_ecr_url" {
  value = module.ecs.frontend_ecr_url
}

output "ecs_cluster" {
  value = module.ecs.ecs_cluster_name
}

output "rds_endpoint" {
  value = module.rds.endpoint
}

output "etl_ecr_url" {
  value = module.ecs.etl_ecr_url
}
