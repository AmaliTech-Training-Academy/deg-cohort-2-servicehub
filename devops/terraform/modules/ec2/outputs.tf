output "public_ip" {
  value = aws_eip.this.public_ip
}

output "sg_id" {
  value = aws_security_group.ec2.id
}

output "instance_id" {
  value = aws_instance.this.id
}
