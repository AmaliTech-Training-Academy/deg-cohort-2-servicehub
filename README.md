# Project Name

## Overview
Monorepo containing backend, frontend, QA and DevOps code.

## Folder Structure
  backend/   Spring Boot API        → backend team
  frontend/  Frontend app           → frontend team
  qa/        Tests and QA scripts   → QA team
  devops/    Infrastructure and CI  → DevOps team

## Prerequisites
- Docker Desktop: https://www.docker.com/products/docker-desktop
- Git: https://git-scm.com

You do NOT need Java or Node installed locally.
Docker handles everything.

## Run Locally
  cp .env.example .env
  docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build

  Backend:  http://localhost:8080/actuator/health
  Frontend: http://localhost:3000

  Stop: docker compose down

## Branch Naming
  feature/backend/name
  feature/frontend/name
  feature/devops/name
  fix/backend/name
  fix/frontend/name

## Contributing
1. Branch from dev
2. Open PR targeting dev
3. Get 1 approval
4. Merge

Never push directly to dev or main.
