# ServiceHub — Deployment Guide

## Overview

ServiceHub runs across two environments:

| Environment | Infrastructure | Purpose |
|---|---|---|
| **Dev** | EC2 t3.small + RDS PostgreSQL | Testing and QA |
| **Production** | ECS Fargate + RDS PostgreSQL | Live users |

Images are built once in CI, pushed to ECR, and promoted from dev to prod — the exact same binary QA tested goes to production.

---

## Local Development

### Prerequisites

- Docker and Docker Compose
- Java 21 (for running backend without Docker)
- Node.js 22 (for running frontend without Docker)

### Setup

```sh
git clone https://github.com/AmaliTech-Training-Academy/deg-cohort-2-servicehub.git
cd deg-cohort-2-servicehub
cp .env.example .env
```

Edit `.env` and fill in:

```env
DB_URL=jdbc:postgresql://localhost:5432/servicehub
DB_USERNAME=servicehub
DB_PASSWORD=your-password
JWT_SECRET=your-strong-random-secret  # openssl rand -hex 64
```

### Run

```sh
docker compose up --build
```

| Service | URL |
|---|---|
| Frontend | http://localhost:4200 |
| Backend API | http://localhost:8080 |
| API Docs | http://localhost:8080/swagger-ui.html |

### Default credentials

| Role | Email | Password |
|---|---|---|
| ADMIN | admin@amalitech.com | password123 |
| AGENT | agent@amalitech.com | password123 |
| USER | user@amalitech.com | password123 |

---

## Run the ETL Pipeline (Data Engineering)

The ETL pipeline generates SLA analytics and saves CSVs to `data-engineering/output/`.

```sh
# Start postgres first
docker compose up db

# Run the ETL (exits after completion)
docker compose run --rm data-engineering

# Output files
ls data-engineering/output/
```

---

## Infrastructure Setup (First Time)

### Prerequisites

- Terraform >= 1.5
- AWS CLI configured (`aws sts get-caller-identity`)
- An EC2 key pair created in eu-west-1

```sh
# Create key pair
aws ec2 create-key-pair \
  --key-name servicehub-dev \
  --region eu-west-1 \
  --query 'KeyMaterial' \
  --output text > ~/.ssh/servicehub-dev.pem
chmod 400 ~/.ssh/servicehub-dev.pem
```

### Step 1 — Bootstrap remote state (run once)

```sh
cd devops/terraform/bootstrap
terraform init && terraform apply
```

### Step 2 — Dev server (EC2 + RDS)

```sh
cd devops/terraform/environments/dev
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your values
terraform init && terraform apply
```

Outputs:
```
ec2_public_ip = "x.x.x.x"       → DEV_SERVER_HOST secret
rds_endpoint  = "xxx.rds.amazonaws.com:5432"  → DEV_RDS_ENDPOINT secret
```

### Step 3 — Production (ECS Fargate + RDS)

```sh
cd devops/terraform/environments/prod
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your values
terraform init && terraform apply
```

Outputs:
```
alb_url           → production app URL
backend_ecr_url   → PROD_ECR_BACKEND_URL secret
frontend_ecr_url  → PROD_ECR_FRONTEND_URL secret
etl_ecr_url       → PROD_ECR_ETL_URL secret
ecs_cluster       → PROD_ECS_CLUSTER secret
bastion_ip        → for database access
```

---

## GitHub Secrets Required

### Repository secrets

| Secret | Description |
|---|---|
| `RESEND_API_KEY` | Resend.com API key for failure email alerts |

### Repository variables

| Variable | Description |
|---|---|
| `MAIL_TO` | DevOps email address for CI/deploy failure alerts |

### Dev environment secrets (Settings → Environments → dev)

| Secret | Value |
|---|---|
| `AWS_ACCESS_KEY_ID` | AWS IAM credentials |
| `AWS_SECRET_ACCESS_KEY` | AWS IAM credentials |
| `DEV_SERVER_HOST` | EC2 public IP |
| `DEV_SERVER_USER` | `ec2-user` |
| `DEV_SERVER_SSH_KEY` | Contents of `~/.ssh/servicehub-dev.pem` |
| `DEV_RDS_ENDPOINT` | From `terraform output rds_endpoint` |
| `DEV_DB_NAME` | `servicehub` |
| `DEV_DB_USERNAME` | `servicehub` |
| `DEV_DB_PASSWORD` | Your RDS password |
| `DEV_JWT_SECRET` | Strong random string (min 32 chars) |
| `PROD_ECR_BACKEND_URL` | From `terraform output backend_ecr_url` |
| `PROD_ECR_FRONTEND_URL` | From `terraform output frontend_ecr_url` |
| `PROD_ECR_ETL_URL` | From `terraform output etl_ecr_url` |

### Production environment secrets (Settings → Environments → production)

| Secret | Value |
|---|---|
| `AWS_ACCESS_KEY_ID` | AWS IAM credentials |
| `AWS_SECRET_ACCESS_KEY` | AWS IAM credentials |
| `PROD_ECR_BACKEND_URL` | From `terraform output backend_ecr_url` |
| `PROD_ECR_FRONTEND_URL` | From `terraform output frontend_ecr_url` |
| `PROD_ECR_ETL_URL` | From `terraform output etl_ecr_url` |
| `PROD_ECS_CLUSTER` | From `terraform output ecs_cluster` |
| `PROD_ECS_BACKEND_SERVICE` | `servicehub-prod-backend` |
| `PROD_ECS_FRONTEND_SERVICE` | `servicehub-prod-frontend` |

---

## Deploy Pipeline

### Dev deploy (automatic on push to `dev`)

1. CI builds backend + frontend + ETL Docker images
2. Pushes to ECR as `:SHA` and `:dev-latest`
3. SSHes into EC2, pulls images from ECR
4. Runs `docker compose up -d --no-build`
5. Runs ETL pipeline to refresh analytics
6. Emails DevOps on failure

### Production deploy (automatic on merge to `main`)

Production uses **image promotion** — the exact same images QA tested on dev are promoted to production without rebuilding.

1. Retags `backend:dev-latest` → `backend:prod-latest` in ECR
2. Retags `frontend:dev-latest` → `frontend:prod-latest` in ECR
3. Retags `etl:dev-latest` → `etl:prod-latest` in ECR
4. Updates ECS task definitions
5. ECS rolls out new tasks — old tasks stay up until new ones pass health checks
6. ETL runs hourly via EventBridge in production

---

## Database Access

### Dev server (EC2)

SSH tunnel through the EC2 server:

```sh
ssh -i ~/.ssh/servicehub-dev.pem \
  -L 5433:servicehub-dev-postgres.c1cou6044uur.eu-west-1.rds.amazonaws.com:5432 \
  ec2-user@<EC2_PUBLIC_IP> \
  -N
```

Connect to `localhost:5433` with any PostgreSQL client.

### Production (ECS)

SSH tunnel through the bastion host:

```sh
ssh -i ~/.ssh/servicehub-dev.pem \
  -L 5433:servicehub-prod-postgres.c1cou6044uur.eu-west-1.rds.amazonaws.com:5432 \
  ec2-user@<BASTION_IP> \
  -N
```

Connect to `localhost:5433`.

---

## Terraform State

Remote state is stored in S3 with DynamoDB locking:

- **Bucket:** `servicehub-terraform-state-673588459780`
- **Lock table:** `servicehub-terraform-locks`
- **Region:** `eu-west-1`

Each environment has its own state key (`dev/terraform.tfstate`, `prod/terraform.tfstate`).

---

## On-call Runbook

### Dev server not responding

```sh
# SSH in
ssh -i ~/.ssh/servicehub-dev.pem ec2-user@<EC2_IP>

# Check containers
docker compose ps

# View backend logs
docker logs app-backend-1 --tail 50

# Restart
docker compose -f docker-compose.yml -f docker-compose.aws.yml restart
```

### Production ECS task unhealthy

1. Check CloudWatch logs in AWS Console → ECS → servicehub-prod-cluster
2. If backend: check RDS connectivity and startup time
3. If frontend: check nginx logs
4. Rollback: re-run the previous dev→main merge (ECS will promote the previous `dev-latest` image)

### Destroy dev infrastructure (cost saving)

```sh
cd devops/terraform/environments/dev
terraform destroy
```
