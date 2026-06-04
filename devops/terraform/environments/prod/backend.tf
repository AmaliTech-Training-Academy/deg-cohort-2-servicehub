terraform {
  backend "s3" {
    bucket         = "servicehub-terraform-state-673588459780"
    key            = "prod/terraform.tfstate"
    region         = "eu-west-1"
    dynamodb_table = "servicehub-terraform-locks"
    encrypt        = true
  }
}
