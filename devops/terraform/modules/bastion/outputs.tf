output "public_ip" {
  value       = aws_instance.bastion.public_ip
  description = "Bastion public IP — use for SSH tunnel to RDS"
}

output "sg_id" {
  value = aws_security_group.bastion.id
}
