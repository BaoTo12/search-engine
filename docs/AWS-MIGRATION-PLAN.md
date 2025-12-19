# AWS Migration & DevOps Implementation Plan

> **Purpose:** Complete AWS migration strategy for enterprise search engine  
> **Target:** AWS Solutions Architect Associate certification demonstration  
> **Approach:** Infrastructure as Code + Full CI/CD pipeline

---

## Table of Contents

1. [Current vs AWS Architecture](#1-current-vs-aws-architecture)
2. [AWS Service Mapping](#2-aws-service-mapping)
3. [Infrastructure as Code (Terraform)](#3-infrastructure-as-code)
4. [CI/CD Pipeline (GitHub Actions)](#4-cicd-pipeline)
5. [Migration Steps](#5-migration-steps)
6. [Cost Estimation](#6-cost-estimation)
7. [Security & Compliance](#7-security--compliance)
8. [Monitoring & Observability](#8-monitoring--observability)

---

## 1. Current vs AWS Architecture

### Current Architecture (Local Docker)

```
┌─────────────────────────────────────────────────────┐
│ Local Development Machine                           │
│                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────┐│
│  │ PostgreSQL   │  │ Redis        │  │ Kafka      ││
│  │ (Docker)     │  │ (Docker)     │  │ (Docker)   ││
│  └──────────────┘  └──────────────┘  └────────────┘│
│                                                      │
│  ┌──────────────┐  ┌──────────────┐                │
│  │ Elasticsearch│  │ Spring Boot  │                │
│  │ (Docker)     │  │ App (JAR)    │                │
│  └──────────────┘  └──────────────┘                │
│                                                      │
│  ┌──────────────┐                                   │
│  │ Next.js UI   │                                   │
│  └──────────────┘                                   │
└─────────────────────────────────────────────────────┘
```

### Target AWS Architecture

```
┌────────────────────────────────────────────────────────────────────┐
│                         AWS Cloud                                   │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │                    Route 53 (DNS)                             │ │
│  └────────────────────────┬─────────────────────────────────────┘ │
│                           ↓                                         │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │              CloudFront (CDN) + WAF                           │ │
│  └────────────────┬──────────────────────┬──────────────────────┘ │
│                   ↓                      ↓                          │
│  ┌────────────────────────┐   ┌─────────────────────────────────┐ │
│  │ S3 (Static Frontend)   │   │ Application Load Balancer       │ │
│  │ - Next.js Build        │   │ (ALB)                           │ │
│  └────────────────────────┘   └──────────┬──────────────────────┘ │
│                                           ↓                          │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │                      VPC (10.0.0.0/16)                        │ │
│  │                                                                │ │
│  │  ┌─────────────────────────────────────────────────────────┐ │ │
│  │  │ Public Subnet (10.0.1.0/24, 10.0.2.0/24)                │ │ │
│  │  │  ┌──────────────────┐    ┌──────────────────┐          │ │ │
│  │  │  │ NAT Gateway AZ-A │    │ NAT Gateway AZ-B │          │ │ │
│  │  │  └──────────────────┘    └──────────────────┘          │ │ │
│  │  └─────────────────────────────────────────────────────────┘ │ │
│  │                                                                │ │
│  │  ┌─────────────────────────────────────────────────────────┐ │ │
│  │  │ Private Subnet - App (10.0.3.0/24, 10.0.4.0/24)        │ │ │
│  │  │  ┌────────────────────────────────────────────────────┐│ │ │
│  │  │  │ ECS Fargate Cluster                                ││ │ │
│  │  │  │  ┌──────────────┐  ┌──────────────┐               ││ │ │
│  │  │  │  │ Backend      │  │ Crawler      │               ││ │ │
│  │  │  │  │ Service      │  │ Workers      │               ││ │ │
│  │  │  │  │ (3 tasks)    │  │ (5 tasks)    │               ││ │ │
│  │  │  │  └──────────────┘  └──────────────┘               ││ │ │
│  │  │  └────────────────────────────────────────────────────┘│ │ │
│  │  └─────────────────────────────────────────────────────────┘ │ │
│  │                                                                │ │
│  │  ┌─────────────────────────────────────────────────────────┐ │ │
│  │  │ Private Subnet - Data (10.0.5.0/24, 10.0.6.0/24)       │ │ │
│  │  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐│ │ │
│  │  │  │ RDS          │  │ ElastiCache  │  │ MSK          ││ │ │
│  │  │  │ PostgreSQL   │  │ Redis        │  │ (Kafka)      ││ │ │
│  │  │  │ Multi-AZ     │  │ Cluster      │  │ 3 brokers    ││ │ │
│  │  │  └──────────────┘  └──────────────┘  └──────────────┘│ │ │
│  │  │                                                         │ │ │
│  │  │  ┌──────────────────────────────────────────────────┐ │ │ │
│  │  │  │ OpenSearch Service (Elasticsearch)               │ │ │ │
│  │  │  │ - 3 data nodes (Multi-AZ)                        │ │ │ │
│  │  │  │ - 3 master nodes                                 │ │ │ │
│  │  │  └──────────────────────────────────────────────────┘ │ │ │
│  │  └─────────────────────────────────────────────────────────┘ │ │
│  └───────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  Supporting Services:                                                │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌────────────┐│
│  │ ECR          │ │ CloudWatch   │ │ Secrets      │ │ Parameter  ││
│  │ (Docker      │ │ (Logs &      │ │ Manager      │ │ Store      ││
│  │  Registry)   │ │  Metrics)    │ │              │ │            ││
│  └──────────────┘ └──────────────┘ └──────────────┘ └────────────┘│
└────────────────────────────────────────────────────────────────────┘
```

---

## 2. AWS Service Mapping

### Infrastructure Components

| Current Service | AWS Service | Justification | Configuration |
|----------------|-------------|---------------|---------------|
| **PostgreSQL (Docker)** | **RDS PostgreSQL** | Managed, Multi-AZ, auto-backup | `db.t3.medium`, 100GB, Multi-AZ |
| **Redis (Docker)** | **ElastiCache Redis** | Managed cluster, auto-failover | `cache.t3.medium`, 2 nodes |
| **Kafka (Docker)** | **MSK (Managed Kafka)** | Fully managed Kafka, metrics | 3 brokers, `kafka.t3.small` |
| **Elasticsearch (Docker)** | **OpenSearch Service** | Managed, auto-scaling, security | 3 data + 3 master, `t3.medium` |
| **Spring Boot JAR** | **ECS Fargate** | Serverless containers, auto-scale | 3 tasks, 2 vCPU, 4GB RAM |
| **Next.js UI** | **S3 + CloudFront** | Static hosting, CDN, low cost | S3 bucket + CloudFront distribution |
| **Local network** | **VPC** | Isolated network, security groups | 10.0.0.0/16, 3-tier subnet design |
| **None** | **ALB** | Load balancing, health checks | Application Load Balancer |
| **None** | **Route 53** | DNS management | Hosted zone |

### Supporting Services

| Service | Purpose | Configuration |
|---------|---------|---------------|
| **ECR** | Docker image registry | Private repository |
| **Secrets Manager** | DB credentials, API keys | Auto-rotation enabled |
| **Parameter Store** | Application config | Encrypted parameters |
| **CloudWatch** | Logs, metrics, alarms | Log groups, dashboards |
| **CloudWatch Alarms** | Alerting | SNS notifications |
| **SNS** | Email/SMS notifications | Topics for alerts |
| **IAM** | Access management | Least privilege policies |
| **CloudFront** | CDN for frontend | Edge locations worldwide |
| **WAF** | Web application firewall | DDoS protection |
| **S3** | Static assets, backups | Versioning, encryption |
| **AWS Backup** | Automated backups | Daily snapshots |

---

## 3. Infrastructure as Code (Terraform)

### Project Structure

```
terraform/
├── environments/
│   ├── dev/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   └── terraform.tfvars
│   ├── staging/
│   │   └── ...
│   └── prod/
│       └── ...
├── modules/
│   ├── vpc/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   └── outputs.tf
│   ├── ecs/
│   │   └── ...
│   ├── rds/
│   │   └── ...
│   ├── elasticache/
│   │   └── ...
│   ├── msk/
│   │   └── ...
│   ├── opensearch/
│   │   └── ...
│   ├── alb/
│   │   └── ...
│   └── cloudfront/
│       └── ...
├── backend.tf
└── provider.tf
```

### VPC Module (`modules/vpc/main.tf`)

```hcl
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name        = "${var.project_name}-vpc"
    Environment = var.environment
  }
}

# Internet Gateway
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "${var.project_name}-igw"
  }
}

# Public Subnets (for ALB, NAT Gateway)
resource "aws_subnet" "public" {
  count                   = length(var.availability_zones)
  vpc_id                  = aws_vpc.main.id
  cidr_block              = cidrsubnet(var.vpc_cidr, 8, count.index)
  availability_zone       = var.availability_zones[count.index]
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.project_name}-public-${var.availability_zones[count.index]}"
    Tier = "Public"
  }
}

# Private Subnets - Application Layer
resource "aws_subnet" "private_app" {
  count             = length(var.availability_zones)
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, count.index + 10)
  availability_zone = var.availability_zones[count.index]

  tags = {
    Name = "${var.project_name}-private-app-${var.availability_zones[count.index]}"
    Tier = "Private-App"
  }
}

# Private Subnets - Data Layer
resource "aws_subnet" "private_data" {
  count             = length(var.availability_zones)
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, count.index + 20)
  availability_zone = var.availability_zones[count.index]

  tags = {
    Name = "${var.project_name}-private-data-${var.availability_zones[count.index]}"
    Tier = "Private-Data"
  }
}

# NAT Gateways (one per AZ for HA)
resource "aws_eip" "nat" {
  count  = length(var.availability_zones)
  domain = "vpc"

  tags = {
    Name = "${var.project_name}-nat-eip-${count.index + 1}"
  }
}

resource "aws_nat_gateway" "main" {
  count         = length(var.availability_zones)
  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.public[count.index].id

  tags = {
    Name = "${var.project_name}-nat-${var.availability_zones[count.index]}"
  }
}

# Route Tables
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = "${var.project_name}-public-rt"
  }
}

resource "aws_route_table" "private" {
  count  = length(var.availability_zones)
  vpc_id = aws_vpc.main.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main[count.index].id
  }

  tags = {
    Name = "${var.project_name}-private-rt-${count.index + 1}"
  }
}

# Route Table Associations
resource "aws_route_table_association" "public" {
  count          = length(aws_subnet.public)
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "private_app" {
  count          = length(aws_subnet.private_app)
  subnet_id      = aws_subnet.private_app[count.index].id
  route_table_id = aws_route_table.private[count.index].id
}

resource "aws_route_table_association" "private_data" {
  count          = length(aws_subnet.private_data)
  subnet_id      = aws_subnet.private_data[count.index].id
  route_table_id = aws_route_table.private[count.index].id
}
```

### RDS Module (`modules/rds/main.tf`)

```hcl
resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-db-subnet-group"
  subnet_ids = var.private_subnet_ids

  tags = {
    Name = "${var.project_name}-db-subnet-group"
  }
}

resource "aws_security_group" "rds" {
  name        = "${var.project_name}-rds-sg"
  description = "Security group for RDS PostgreSQL"
  vpc_id      = var.vpc_id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [var.app_security_group_id]
    description     = "PostgreSQL from application"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-rds-sg"
  }
}

resource "aws_db_instance" "postgres" {
  identifier     = "${var.project_name}-postgres"
  engine         = "postgres"
  engine_version = "15.4"
  instance_class = var.instance_class  # db.t3.medium

  allocated_storage     = var.allocated_storage  # 100
  max_allocated_storage = var.max_allocated_storage  # 500
  storage_type          = "gp3"
  storage_encrypted     = true

  db_name  = var.database_name
  username = var.master_username
  password = var.master_password  # From Secrets Manager

  multi_az               = var.multi_az  # true for prod
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  backup_retention_period = 7
  backup_window           = "03:00-04:00"
  maintenance_window      = "mon:04:00-mon:05:00"

  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]
  performance_insights_enabled    = true
  monitoring_interval             = 60
  monitoring_role_arn             = aws_iam_role.rds_monitoring.arn

  deletion_protection = var.deletion_protection
  skip_final_snapshot = var.environment != "prod"
  final_snapshot_identifier = var.environment == "prod" ? "${var.project_name}-final-snapshot" : null

  tags = {
    Name        = "${var.project_name}-postgres"
    Environment = var.environment
  }
}

# IAM Role for Enhanced Monitoring
resource "aws_iam_role" "rds_monitoring" {
  name = "${var.project_name}-rds-monitoring-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "monitoring.rds.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "rds_monitoring" {
  role       = aws_iam_role.rds_monitoring.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole"
}
```

### ECS Fargate Module (`modules/ecs/main.tf`)

```hcl
resource "aws_ecs_cluster" "main" {
  name = "${var.project_name}-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = {
    Name = "${var.project_name}-ecs-cluster"
  }
}

# Task Execution Role
resource "aws_iam_role" "ecs_task_execution" {
  name = "${var.project_name}-ecs-task-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ecs-tasks.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution" {
  role       = aws_iam_role.ecs_task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# Additional policy for Secrets Manager
resource "aws_iam_role_policy" "ecs_task_execution_secrets" {
  name = "ecs-task-execution-secrets"
  role = aws_iam_role.ecs_task_execution.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "secretsmanager:GetSecretValue",
        "ssm:GetParameters"
      ]
      Resource = "*"
    }]
  })
}

# Task Role (application permissions)
resource "aws_iam_role" "ecs_task" {
  name = "${var.project_name}-ecs-task-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ecs-tasks.amazonaws.com"
      }
    }]
  })
}

# Security Group
resource "aws_security_group" "ecs_tasks" {
  name        = "${var.project_name}-ecs-tasks-sg"
  description = "Security group for ECS tasks"
  vpc_id      = var.vpc_id

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [var.alb_security_group_id]
    description     = "Allow inbound from ALB"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow all outbound"
  }

  tags = {
    Name = "${var.project_name}-ecs-tasks-sg"
  }
}

# CloudWatch Log Group
resource "aws_cloudwatch_log_group" "ecs" {
  name              = "/ecs/${var.project_name}"
  retention_in_days = 30

  tags = {
    Name = "${var.project_name}-ecs-logs"
  }
}

# Task Definition
resource "aws_ecs_task_definition" "backend" {
  family                   = "${var.project_name}-backend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.task_cpu  # 2048 (2 vCPU)
  memory                   = var.task_memory  # 4096 (4 GB)
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name  = "backend"
    image = "${var.ecr_repository_url}:${var.image_tag}"

    portMappings = [{
      containerPort = 8080
      protocol      = "tcp"
    }]

    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = var.environment },
      { name = "SERVER_PORT", value = "8080" }
    ]

    secrets = [
      {
        name      = "SPRING_DATASOURCE_URL"
        valueFrom = "${var.db_credentials_secret_arn}:url::"
      },
      {
        name      = "SPRING_DATASOURCE_USERNAME"
        valueFrom = "${var.db_credentials_secret_arn}:username::"
      },
      {
        name      = "SPRING_DATASOURCE_PASSWORD"
        valueFrom = "${var.db_credentials_secret_arn}:password::"
      }
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.ecs.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "backend"
      }
    }

    healthCheck = {
      command     = ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 60
    }
  }])

  tags = {
    Name = "${var.project_name}-backend-task"
  }
}

# ECS Service
resource "aws_ecs_service" "backend" {
  name            = "${var.project_name}-backend-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = var.desired_count  # 3 for HA
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = var.target_group_arn
    container_name   = "backend"
    container_port   = 8080
  }

  deployment_configuration {
    maximum_percent         = 200
    minimum_healthy_percent = 100
  }

  # Auto-scaling
  lifecycle {
    ignore_changes = [desired_count]
  }

  depends_on = [var.alb_listener_arn]

  tags = {
    Name = "${var.project_name}-backend-service"
  }
}

# Auto-scaling
resource "aws_appautoscaling_target" "ecs" {
  max_capacity       = var.max_capacity  # 10
  min_capacity       = var.min_capacity  # 2
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.backend.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "ecs_cpu" {
  name               = "${var.project_name}-cpu-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value = 70.0
  }
}
```

---

## 4. CI/CD Pipeline (GitHub Actions)

### GitHub Repository Structure

```
.github/
└── workflows/
    ├── ci.yml                  # Continuous Integration
    ├── cd-dev.yml              # Deploy to Dev
    ├── cd-staging.yml          # Deploy to Staging
    ├── cd-prod.yml             # Deploy to Production
    └── terraform-plan.yml      # Infrastructure changes
```

### CI Workflow (`.github/workflows/ci.yml`)

```yaml
name: CI - Build and Test

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

env:
  JAVA_VERSION: '21'
  NODE_VERSION: '18'
  AWS_REGION: us-east-1

jobs:
  backend-build:
    name: Backend - Build & Test
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Cache Maven packages
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
          restore-keys: ${{ runner.os }}-m2

      - name: Run tests
        working-directory: ./search-engine
        run: mvn clean test

      - name: Build JAR
        working-directory: ./search-engine
        run: mvn package -DskipTests

      - name: Upload JAR artifact
        uses: actions/upload-artifact@v3
        with:
          name: backend-jar
          path: search-engine/target/*.jar

  frontend-build:
    name: Frontend - Build & Test
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '18'
          cache: 'npm'
          cache-dependency-path: search-engine-ui/package-lock.json

      - name: Install dependencies
        working-directory: ./search-engine-ui
        run: npm ci

      - name: Run linter
        working-directory: ./search-engine-ui
        run: npm run lint

      - name: Build frontend
        working-directory: ./search-engine-ui
        run: npm run build
        env:
          NEXT_PUBLIC_API_URL: https://api.example.com

      - name: Upload build artifact
        uses: actions/upload-artifact@v3
        with:
          name: frontend-build
          path: search-engine-ui/.next

  docker-build:
    name: Build Docker Images
    runs-on: ubuntu-latest
    needs: [backend-build, frontend-build]

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Download backend JAR
        uses: actions/download-artifact@v3
        with:
          name: backend-jar
          path: search-engine/target

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2

      - name: Build and push backend image
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          ECR_REPOSITORY: search-engine-backend
          IMAGE_TAG: ${{ github.sha }}
        run: |
          docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG \
            -t $ECR_REGISTRY/$ECR_REPOSITORY:latest \
            -f search-engine/Dockerfile search-engine
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:latest

      - name: Build and push crawler image
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          ECR_REPOSITORY: search-engine-crawler
          IMAGE_TAG: ${{ github.sha }}
        run: |
          docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG \
            -t $ECR_REGISTRY/$ECR_REPOSITORY:latest \
            -f search-engine/Dockerfile.crawler search-engine
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:latest
```

### CD - Deploy to Production (`.github/workflows/cd-prod.yml`)

```yaml
name: CD - Deploy to Production

on:
  release:
    types: [published]
  workflow_dispatch:

env:
  AWS_REGION: us-east-1
  ENVIRONMENT: prod

jobs:
  terraform-apply:
    name: Apply Terraform Changes
    runs-on: ubuntu-latest
    environment: production

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup Terraform
        uses: hashicorp/setup-terraform@v3
        with:
          terraform_version: 1.6.0

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Terraform Init
        working-directory: ./terraform/environments/prod
        run: terraform init

      - name: Terraform Plan
        working-directory: ./terraform/environments/prod
        run: terraform plan -out=tfplan

      - name: Terraform Apply
        working-directory: ./terraform/environments/prod
        run: terraform apply -auto-approve tfplan

  deploy-backend:
    name: Deploy Backend Service
    runs-on: ubuntu-latest
    needs: terraform-apply
    environment: production

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2

      - name: Update ECS Service
        run: |
          aws ecs update-service \
            --cluster search-engine-prod-cluster \
            --service search-engine-prod-backend-service \
            --force-new-deployment \
            --region ${{ env.AWS_REGION }}

      - name: Wait for service stability
        run: |
          aws ecs wait services-stable \
            --cluster search-engine-prod-cluster \
            --services search-engine-prod-backend-service \
            --region ${{ env.AWS_REGION }}

  deploy-frontend:
    name: Deploy Frontend to S3
    runs-on: ubuntu-latest
    needs: terraform-apply
    environment: production

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '18'

      - name: Install and build
        working-directory: ./search-engine-ui
        run: |
          npm ci
          npm run build
        env:
          NEXT_PUBLIC_API_URL: https://api.searchengine.com

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Deploy to S3
        run: |
          aws s3 sync search-engine-ui/out s3://search-engine-prod-frontend \
            --delete \
            --cache-control max-age=31536000,public

      - name: Invalidate CloudFront
        run: |
          aws cloudfront create-invalidation \
            --distribution-id ${{ secrets.CLOUDFRONT_DISTRIBUTION_ID }} \
            --paths "/*"

  run-smoke-tests:
    name: Run Smoke Tests
    runs-on: ubuntu-latest
    needs: [deploy-backend, deploy-frontend]

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Run health check
        run: |
          response=$(curl -s -o /dev/null -w "%{http_code}" https://api.searchengine.com/actuator/health)
          if [ $response -ne 200 ]; then
            echo "Health check failed with status $response"
            exit 1
          fi
          echo "Health check passed"

      - name: Test search endpoint
        run: |
          response=$(curl -s "https://api.searchengine.com/api/v1/search?query=test")
          echo "Search response: $response"
```

---

## 5. Migration Steps

### Phase 1: Infrastructure Setup (Week 1)

**Day 1-2: AWS Account Setup**
```bash
# 1. Create AWS account (if new)
# 2. Enable MFA for root user
# 3. Create IAM admin user
# 4. Configure AWS CLI
aws configure --profile search-engine-prod

# 5. Create S3 bucket for Terraform state
aws s3 mb s3://search-engine-terraform-state \
  --region us-east-1

# 6. Enable versioning
aws s3api put-bucket-versioning \
  --bucket search-engine-terraform-state \
  --versioning-configuration Status=Enabled

# 7. Create DynamoDB table for state locking
aws dynamodb create-table \
  --table-name terraform-lock \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

**Day 3-4: Terraform Infrastructure**
```bash
# 1. Clone repository
git clone https://github.com/yourusername/search-engine
cd search-engine

# 2. Create terraform backend config
cat > terraform/backend.tf <<EOF
terraform {
  backend "s3" {
    bucket         = "search-engine-terraform-state"
    key            = "prod/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "terraform-lock"
    encrypt        = true
  }
}
EOF

# 3. Initialize terraform
cd terraform/environments/prod
terraform init

# 4. Plan infrastructure
terraform plan -out=tfplan

# 5. Review plan carefully
terraform show tfplan

# 6. Apply (create VPC, subnets, security groups)
terraform apply tfplan
```

**Day 5-7: Database Migration**
```bash
# 1. Export local PostgreSQL data
docker exec crawler-postgres pg_dump \
  -U crawler_user crawler_db > backup.sql

# 2. Get RDS endpoint from Terraform
RDS_ENDPOINT=$(terraform output -raw rds_endpoint)

# 3. Create database
PGPASSWORD=$DB_PASSWORD psql \
  -h $RDS_ENDPOINT \
  -U crawler_user \
  -c "CREATE DATABASE crawler_db;"

# 4. Restore data
PGPASSWORD=$DB_PASSWORD psql \
  -h $RDS_ENDPOINT \
  -U crawler_user \
  -d crawler_db \
  < backup.sql

# 5. Run Flyway migrations
cd search-engine
mvn flyway:migrate \
  -Dflyway.url=jdbc:postgresql://$RDS_ENDPOINT:5432/crawler_db \
  -Dflyway.user=crawler_user \
  -Dflyway.password=$DB_PASSWORD
```

### Phase 2: Application Deployment (Week 2)

**Step 1: Create ECR Repositories**
```bash
# Backend repository
aws ecr create-repository \
  --repository-name search-engine-backend \
  --image-scanning-configuration scanOnPush=true \
  --region us-east-1

# Crawler repository
aws ecr create-repository \
  --repository-name search-engine-crawler \
  --region us-east-1
```

**Step 2: Build and Push Docker Images**
```bash
# Login to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  123456789012.dkr.ecr.us-east-1.amazonaws.com

# Build backend
cd search-engine
mvn clean package -DskipTests
docker build -t search-engine-backend .

# Tag and push
docker tag search-engine-backend:latest \
  123456789012.dkr.ecr.us-east-1.amazonaws.com/search-engine-backend:v1.0.0
docker push \
  123456789012.dkr.ecr.us-east-1.amazonaws.com/search-engine-backend:v1.0.0
```

**Step 3: Deploy ECS Services**
```bash
# Apply ECS terraform module
cd terraform/modules/ecs
terraform apply

# Verify deployment
aws ecs list-services --cluster search-engine-prod-cluster
aws ecs describe-services \
  --cluster search-engine-prod-cluster \
  --services search-engine-prod-backend-service
```

**Step 4: Deploy Frontend to S3**
```bash
cd search-engine-ui
npm run build

# Sync to S3
aws s3 sync out/ s3://search-engine-prod-frontend/ \
  --delete \
  --cache-control max-age=86400

# Invalidate CloudFront cache
aws cloudfront create-invalidation \
  --distribution-id E1234567890ABC \
  --paths "/*"
```

### Phase 3: DNS & SSL (Week 2)

**Step 1: Request SSL Certificate**
```bash
# Request certificate in ACM
aws acm request-certificate \
  --domain-name searchengine.com \
  --subject-alternative-names "*.searchengine.com" \
  --validation-method DNS \
  --region us-east-1

# Get validation records
aws acm describe-certificate \
  --certificate-arn arn:aws:acm:us-east-1:123456789012:certificate/abc123
```

**Step 2: Configure Route 53**
```bash
# Create hosted zone
aws route53 create-hosted-zone \
  --name searchengine.com \
  --caller-reference $(date +%s)

# Create A record for ALB
aws route53 change-resource-record-sets \
  --hosted-zone-id Z1234567890ABC \
  --change-batch file://route53-alb.json
```

### Phase 4: Monitoring & Alerts (Week 3)

**CloudWatch Dashboards**
```bash
# Create dashboard
aws cloudwatch put-dashboard \
  --dashboard-name search-engine-prod \
  --dashboard-body file://dashboard.json
```

**Alarms**
```bash
# CPU alarm
aws cloudwatch put-metric-alarm \
  --alarm-name search-engine-high-cpu \
  --alarm-description "Alert when CPU exceeds 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/ECS \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \
  --dimensions Name=ServiceName,Value=search-engine-prod-backend-service \
  --alarm-actions arn:aws:sns:us-east-1:123456789012:alerts
```

---

## 6. Cost Estimation

### Monthly Cost Breakdown (Production)

| Service | Specification | Monthly Cost |
|---------|---------------|--------------|
| **EC2 (NAT Gateway)** | 2 × t3.micro (Multi-AZ) | $66 |
| **ECS Fargate** | 3 × 2vCPU, 4GB (backend) | $73 |
| **ECS Fargate** | 5 × 1vCPU, 2GB (crawler) | $61 |
| **RDS PostgreSQL** | db.t3.medium, 100GB, Multi-AZ | $142 |
| **ElastiCache Redis** | cache.t3.medium, 2 nodes | $97 |
| **MSK (Kafka)** | 3 × kafka.t3.small | $244 |
| **OpenSearch** | 3 data + 3 master (t3.medium) | $438 |
| **ALB** | Application Load Balancer | $23 |
| **CloudFront** | 1TB transfer | $85 |
| **S3** | 50GB storage, requests | $5 |
| **CloudWatch** | Logs, metrics, dashboards | $15 |
| **Route 53** | Hosted zone | $0.50 |
| **Data Transfer** | Outbound 500GB | $45 |
| **TOTAL** | | **~$1,294/month** |

### Cost Optimization Strategies

**Development Environment:**
- Single AZ deployments: -50%
- Smaller instance types: -40%
- Spot instances for crawlers: -70%
- **Dev cost: ~$350/month**

**Reserved Instances (1-year):**
- RDS Reserved: -35%
- ElastiCache Reserved: -35%
- **Savings: ~$200/month**

**Auto-scaling:**
- Scale down crawler workers at night: -30%
- Scale ECS tasks based on load: -20%

---

## 7. Security & Compliance

### Network Security

```hcl
# Security Group - Zero Trust Model
resource "aws_security_group" "ecs_tasks" {
  # Only allow traffic from ALB
  ingress {
    from_port       = 8080
    to_port         = 8080
    security_groups = [aws_security_group.alb.id]
  }
}

resource "aws_security_group" "rds" {
  # Only allow PostgreSQL from ECS tasks
  ingress {
    from_port = 5432
    to_port   = 5432
    security_groups = [aws_security_group.ecs_tasks.id]
  }
}

# Web Application Firewall
resource "aws_wafv2_web_acl" "main" {
  name  = "search-engine-waf"
  scope = "CLOUDFRONT"

  default_action {
    allow {}
  }

  rule {
    name     = "RateLimitRule"
    priority = 1

    statement {
      rate_based_statement {
        limit              = 2000
        aggregate_key_type = "IP"
      }
    }

    action {
      block {}
    }

    visibility_config {
      sampled_requests_enabled   = true
      cloudwatch_metrics_enabled = true
      metric_name               = "RateLimitRule"
    }
  }

  rule {
    name     = "AWSManagedRulesCommonRuleSet"
    priority = 2

    override_action {
      none {}
    }

    statement {
      managed_rule_group_statement {
        vendor_name = "AWS"
        name        = "AWSManagedRulesCommonRuleSet"
      }
    }

    visibility_config {
      sampled_requests_enabled   = true
      cloudwatch_metrics_enabled = true
      metric_name               = "AWSManagedRulesCommonRuleSetMetric"
    }
  }
}
```

### Secrets Management

```bash
# Store database credentials
aws secretsmanager create-secret \
  --name search-engine/prod/db-credentials \
  --secret-string '{
    "username": "crawler_user",
    "password": "STRONG_PASSWORD_HERE",
    "host": "search-engine-prod-postgres.abc.us-east-1.rds.amazonaws.com",
    "port": "5432",
    "database": "crawler_db"
  }'

# Enable automatic rotation
aws secretsmanager rotate-secret \
  --secret-id search-engine/prod/db-credentials \
  --rotation-lambda-arn arn:aws:lambda:us-east-1:123456789012:function:SecretsManagerRotation \
  --rotation-rules AutomaticallyAfterDays=30
```

---

## 8. Monitoring & Observability

### CloudWatch Dashboard Configuration

```json
{
  "widgets": [
    {
      "type": "metric",
      "properties": {
        "metrics": [
          ["AWS/ECS", "CPUUtilization", {"stat": "Average"}],
          [".", "MemoryUtilization", {"stat": "Average"}]
        ],
        "period": 300,
        "stat": "Average",
        "region": "us-east-1",
        "title": "ECS Resource Utilization"
      }
    },
    {
      "type": "metric",
      "properties": {
        "metrics": [
          ["AWS/RDS", "DatabaseConnections"],
          [".", "CPUUtilization"],
          [".", "FreeableMemory"]
        ],
        "period": 300,
        "stat": "Average",
        "region": "us-east-1",
        "title": "RDS Performance"
      }
    },
    {
      "type": "log",
      "properties": {
        "query": "SOURCE '/ecs/search-engine' | fields @timestamp, @message | filter @message like /ERROR/",
        "region": "us-east-1",
        "title": "Application Errors"
      }
    }
  ]
}
```

### Alarms Configuration

```hcl
resource "aws_cloudwatch_metric_alarm" "ecs_cpu_high" {
  alarm_name          = "search-engine-ecs-cpu-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/ECS"
  period              = "300"
  statistic           = "Average"
  threshold           = "80"
  alarm_description   = "ECS CPU usage is too high"
  alarm_actions       = [aws_sns_topic.alerts.arn]

  dimensions = {
    ServiceName = aws_ecs_service.backend.name
    ClusterName = aws_ecs_cluster.main.name
  }
}

resource "aws_cloudwatch_metric_alarm" "rds_storage_low" {
  alarm_name          = "search-engine-rds-storage-low"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "FreeStorageSpace"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "10737418240"  # 10GB
  alarm_description   = "RDS free storage space is low"
  alarm_actions       = [aws_sns_topic.alerts.arn]

  dimensions = {
    DBInstanceIdentifier = aws_db_instance.postgres.id
  }
}

resource "aws_sns_topic" "alerts" {
  name = "search-engine-alerts"
}

resource "aws_sns_topic_subscription" "email" {
  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "email"
  endpoint  = "your-email@example.com"
}
```

---

*This migration plan demonstrates AWS Solutions Architect Associate level skills with production-ready infrastructure, security best practices, and complete DevOps automation.*
