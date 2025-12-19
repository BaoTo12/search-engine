# Terraform: From Zero to Cloud Architect
## A Comprehensive University-Level Course

> **Professor's Note:** This course takes you from complete beginner to production-ready Infrastructure as Code expert. Each chapter builds upon previous concepts. Complete all exercises!

---

## 📚 Course Syllabus

**Total Duration:** 12 Weeks  
**Prerequisites:** Basic Linux/command line knowledge  
**Certification Prep:** Terraform Associate, AWS Solutions Architect

### Course Outline

1. **Week 1-2:** Foundations & Core Concepts
2. **Week 3-4:** Variables, Outputs & Data Sources
3. **Week 5-6:** Modules & Code Organization
4. **Week 7-8:** State Management & Remote Backends
5. **Week 9-10:** Advanced Patterns & Best Practices
6. **Week 11:** Production Deployment Strategies
7. **Week 12:** Final Project - Multi-Environment Infrastructure

---

# Part I: Foundations (Weeks 1-2)

## Chapter 1: Introduction to Infrastructure as Code

### 1.1 What is Infrastructure as Code?

**Traditional Approach (Manual):**
```
Developer: "I need a server"
         ↓
Operations: Click, click, click in AWS console
         ↓
Server created... wait, what were the settings?
         ↓
Documentation? Maybe in a wiki... if we're lucky
```

**Problems:**
- Manual errors
- Inconsistent environments
- No version control
- Difficult to replicate
- Knowledge locked in people's heads

**IaC Approach:**
```hcl
# server.tf
resource "aws_instance" "web" {
  ami           = "ami-12345678"
  instance_type = "t3.medium"
  
  tags = {
    Name = "WebServer"
  }
}
```

**Benefits:**
- ✅ Version controlled (Git)
- ✅ Reproducible
- ✅ Self-documenting
- ✅ Testable
- ✅ Automated

### 1.2 Why Terraform?

**Comparison with Competitors:**

| Feature | Terraform | CloudFormation | Ansible | Pulumi |
|---------|-----------|----------------|---------|--------|
| **Multi-cloud** | ✅ Yes | ❌ AWS only | ✅ Yes | ✅ Yes |
| **Language** | HCL (declarative) | YAML/JSON | YAML | Real programming languages |
| **State** | Explicit state file | Managed by AWS | Stateless | Explicit state |
| **Learning curve** | Medium | Medium | Easy | Hard (requires programming) |
| **Community** | Huge | Large | Huge | Growing |

**When to use Terraform:**
- Multi-cloud deployments
- Need declarative syntax
- Want large community support
- Infrastructure-focused (not config management)

### 1.3 Core Concepts

```
┌─────────────────────────────────────────────┐
│         Terraform Architecture              │
│                                             │
│  ┌──────────┐                              │
│  │   .tf    │  Terraform Configuration     │
│  │  files   │  (What you write)            │
│  └────┬─────┘                              │
│       │                                     │
│       ↓                                     │
│  ┌──────────┐                              │
│  │terraform │  CLI Commands                │
│  │   CLI    │  (plan, apply, destroy)      │
│  └────┬─────┘                              │
│       │                                     │
│       ↓                                     │
│  ┌──────────┐                              │
│  │ Provider │  AWS, Azure, GCP plugins     │
│  │  Plugin  │  (Talks to cloud APIs)       │
│  └────┬─────┘                              │
│       │                                     │
│       ↓                                     │
│  ┌──────────┐                              │
│  │  Cloud   │  Actual Infrastructure       │
│  │   API    │  (AWS, Azure, etc.)          │
│  └──────────┘                              │
└─────────────────────────────────────────────┘
```

**Key Terms:**

- **Provider:** Plugin for cloud platform (aws, azure, google)
- **Resource:** Infrastructure object (EC2, S3, RDS)
- **State:** Current infrastructure snapshot
- **Plan:** Preview of changes
- **Apply:** Execute changes

---

## Chapter 2: Your First Terraform Project

### 2.1 Installation

**macOS:**
```bash
brew tap hashicorp/tap
brew install hashicorp/tap/terraform
```

**Windows:**
```bash
choco install terraform
```

**Linux:**
```bash
wget https://releases.hashicorp.com/terraform/1.6.0/terraform_1.6.0_linux_amd64.zip
unzip terraform_1.6.0_linux_amd64.zip
sudo mv terraform /usr/local/bin/
```

**Verify:**
```bash
terraform version
# Terraform v1.6.0
```

### 2.2 Hello World - AWS EC2 Instance

**Project Structure:**
```
hello-terraform/
├── main.tf         # Main configuration
├── variables.tf    # Input variables
├── outputs.tf      # Output values
└── terraform.tfstate  # State file (generated)
```

**main.tf:**
```hcl
# Specify Terraform version
terraform {
  required_version = ">= 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# Configure AWS Provider
provider "aws" {
  region = "us-east-1"
}

# Create EC2 Instance
resource "aws_instance" "my_first_server" {
  ami           = "ami-0c55b159cbfafe1f0"  # Amazon Linux 2
  instance_type = "t2.micro"
  
  tags = {
    Name        = "MyFirstTerraformServer"
    Environment = "Learning"
    ManagedBy   = "Terraform"
  }
}
```

**Step-by-Step Execution:**

```bash
# 1. Initialize - Download provider plugins
terraform init

# Output:
# Initializing provider plugins...
# - Finding hashicorp/aws versions matching "~> 5.0"...
# - Installing hashicorp/aws v5.31.0...
# Terraform has been successfully initialized!

# 2. Format code (optional but recommended)
terraform fmt

# 3. Validate syntax
terraform validate

# Output:
# Success! The configuration is valid.

# 4. Plan - See what will be created
terraform plan

# Output:
# Terraform will perform the following actions:
#
#   # aws_instance.my_first_server will be created
#   + resource "aws_instance" "my_first_server" {
#       + ami                          = "ami-0c55b159cbfafe1f0"
#       + instance_type                = "t2.micro"
#       + tags                         = {
#           + "Environment" = "Learning"
#           + "ManagedBy"   = "Terraform"
#           + "Name"        = "MyFirstTerraformServer"
#         }
#       ...
#     }
#
# Plan: 1 to add, 0 to change, 0 to destroy.

# 5. Apply - Create the infrastructure
terraform apply

# Type 'yes' when prompted
# ...
# Apply complete! Resources: 1 added, 0 changed, 0 destroyed.

# 6. Destroy - Clean up
terraform destroy

# Type 'yes' when prompted
# Destroy complete! Resources: 1 destroyed.
```

### 2.3 Understanding State

**terraform.tfstate:**
```json
{
  "version": 4,
  "terraform_version": "1.6.0",
  "resources": [
    {
      "type": "aws_instance",
      "name": "my_first_server",
      "provider": "provider[\"registry.terraform.io/hashicorp/aws\"]",
      "instances": [
        {
          "attributes": {
            "id": "i-0123456789abcdef0",
            "ami": "ami-0c55b159cbfafe1f0",
            "instance_type": "t2.micro",
            "public_ip": "54.123.45.67"
          }
        }
      ]
    }
  ]
}
```

**What is State?**
- Mapping between config and real infrastructure
- Metadata and resource dependencies
- Performance optimization (caches values)

**⚠️ CRITICAL:** Never edit state files manually!

---

# Part II: Variables & Outputs (Weeks 3-4)

## Chapter 3: Input Variables

### 3.1 Why Variables?

**Without Variables (Hard-coded):**
```hcl
resource "aws_instance" "web" {
  ami           = "ami-0c55b159cbfafe1f0"  # What if different region?
  instance_type = "t2.micro"                # What if prod needs larger?
  
  tags = {
    Environment = "dev"                     # Manual change for prod?
  }
}
```

**Problems:**
- No flexibility
- Manual edits needed
- Error-prone
- Can't reuse code

**With Variables:**
```hcl
# variables.tf
variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t2.micro"
}

variable "environment" {
  description = "Environment name"
  type        = string
}

# main.tf
resource "aws_instance" "web" {
  ami           = var.ami_id
  instance_type = var.instance_type
  
  tags = {
    Environment = var.environment
  }
}
```

### 3.2 Variable Types

```hcl
# String
variable "aws_region" {
  type    = string
  default = "us-east-1"
}

# Number
variable "instance_count" {
  type    = number
  default = 3
}

# Boolean
variable "enable_monitoring" {
  type    = bool
  default = true
}

# List
variable "availability_zones" {
  type    = list(string)
  default = ["us-east-1a", "us-east-1b", "us-east-1c"]
}

# Map
variable "instance_types" {
  type = map(string)
  default = {
    dev  = "t2.micro"
    prod = "t3.large"
  }
}

# Object (complex type)
variable "server_config" {
  type = object({
    instance_type = string
    disk_size     = number
    monitoring    = bool
  })
  
  default = {
    instance_type = "t2.micro"
    disk_size     = 20
    monitoring    = false
  }
}
```

### 3.3 Variable Validation

```hcl
variable "environment" {
  type        = string
  description = "Environment name"
  
  validation {
    condition     = can(regex("^(dev|staging|prod)$", var.environment))
    error_message = "Environment must be dev, staging, or prod"
  }
}

variable "instance_type" {
  type = string
  
  validation {
    condition     = can(regex("^t[23]\\.", var.instance_type))
    error_message = "Only t2 and t3 instances allowed in this project"
  }
}
```

### 3.4 Providing Variable Values

**Method 1: Command Line**
```bash
terraform apply -var="environment=prod" -var="instance_count=5"
```

**Method 2: terraform.tfvars**
```hcl
# terraform.tfvars
environment    = "prod"
instance_count = 5
aws_region     = "us-west-2"
```

**Method 3: Environment Variables**
```bash
export TF_VAR_environment="prod"
export TF_VAR_instance_count=5
terraform apply
```

**Method 4: Auto-loaded Files**
```
terraform.tfvars          # Automatically loaded
terraform.tfvars.json     # JSON format
*.auto.tfvars             # Any file matching pattern
```

**Variable Precedence (highest to lowest):**
1. Command line `-var`
2. `*.auto.tfvars` (alphabetical)
3. `terraform.tfvars`
4. Environment variables
5. Default values in variable blocks

### 3.5 Sensitive Variables

```hcl
variable "database_password" {
  type      = string
  sensitive = true
  
  validation {
    condition     = length(var.database_password) >= 16
    error_message = "Password must be at least 16 characters"
  }
}

# In outputs, value will be hidden:
# Apply complete! Resources: 1 added, 0 changed, 0 destroyed.
# Outputs:
# db_password = <sensitive>
```

**Best Practice:** Never commit sensitive values!
```bash
# .gitignore
*.tfvars
!terraform.tfvars.example
terraform.tfstate
terraform.tfstate.backup
.terraform/
```

---

## Chapter 4: Outputs

### 4.1 What are Outputs?

**Purpose:**
- Extract values from resources
- Pass information between modules
- Display important info to users

**Example:**
```hcl
# outputs.tf
output "instance_id" {
  description = "ID of EC2 instance"
  value       = aws_instance.web.id
}

output "public_ip" {
  description = "Public IP of instance"
  value       = aws_instance.web.public_ip
}

output "connection_string" {
  description = "SSH connection command"
  value       = "ssh ec2-user@${aws_instance.web.public_ip}"
}
```

**After apply:**
```bash
terraform apply

# Outputs:
# instance_id = "i-0123456789abcdef0"
# public_ip = "54.123.45.67"
# connection_string = "ssh ec2-user@54.123.45.67"
```

### 4.2 Querying Outputs

```bash
# Show all outputs
terraform output

# Get specific output
terraform output public_ip
# 54.123.45.67

# JSON format (for scripts)
terraform output -json
```

### 4.3 Sensitive Outputs

```hcl
output "database_password" {
  value     = aws_db_instance.main.password
  sensitive = true
}

# Won't be displayed in logs
# To view:
terraform output database_password
```

---

# Part III: Modules (Weeks 5-6)

## Chapter 5: Introduction to Modules

### 5.1 Why Modules?

**Problem: Duplicate Code**
```hcl
# dev/main.tf
resource "aws_instance" "web" {
  ami           = "ami-12345"
  instance_type = "t2.micro"
  # ... 50 lines of config
}

# prod/main.tf
resource "aws_instance" "web" {
  ami           = "ami-12345"
  instance_type = "t3.large"  # Only difference!
  # ... 50 lines of SAME config
}
```

**Solution: Module**
```hcl
# modules/ec2-instance/main.tf
variable "instance_type" {}

resource "aws_instance" "this" {
  ami           = "ami-12345"
  instance_type = var.instance_type
  # ... config once
}

# dev/main.tf
module "web_server" {
  source        = "../modules/ec2-instance"
  instance_type = "t2.micro"
}

# prod/main.tf
module "web_server" {
  source        = "../modules/ec2-instance"
  instance_type = "t3.large"
}
```

### 5.2 Module Structure

```
modules/
└── vpc/
    ├── main.tf         # Resources
    ├── variables.tf    # Input variables
    ├── outputs.tf      # Output values
    ├── README.md       # Documentation
    └── examples/       # Usage examples
        └── basic/
            └── main.tf
```

**Complete VPC Module:**

**modules/vpc/variables.tf:**
```hcl
variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "List of AZs"
  type        = list(string)
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets"
  type        = list(string)
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets"
  type        = list(string)
}

variable "tags" {
  description = "Common tags"
  type        = map(string)
  default     = {}
}
```

**modules/vpc/main.tf:**
```hcl
# VPC
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true
  
  tags = merge(var.tags, {
    Name = "${var.tags["Project"]}-vpc"
  })
}

# Internet Gateway
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id
  
  tags = merge(var.tags, {
    Name = "${var.tags["Project"]}-igw"
  })
}

# Public Subnets
resource "aws_subnet" "public" {
  count                   = length(var.public_subnet_cidrs)
  vpc_id                  = aws_vpc.main.id
  cidr_block              = var.public_subnet_cidrs[count.index]
  availability_zone       = var.availability_zones[count.index]
  map_public_ip_on_launch = true
  
  tags = merge(var.tags, {
    Name = "${var.tags["Project"]}-public-${count.index + 1}"
    Tier = "Public"
  })
}

# Private Subnets
resource "aws_subnet" "private" {
  count             = length(var.private_subnet_cidrs)
  vpc_id            = aws_vpc.main.id
  cidr_block        = var.private_subnet_cidrs[count.index]
  availability_zone = var.availability_zones[count.index]
  
  tags = merge(var.tags, {
    Name = "${var.tags["Project"]}-private-${count.index + 1}"
    Tier = "Private"
  })
}

# NAT Gateway
resource "aws_eip" "nat" {
  count  = length(var.public_subnet_cidrs)
  domain = "vpc"
  
  tags = merge(var.tags, {
    Name = "${var.tags["Project"]}-nat-eip-${count.index + 1}"
  })
}

resource "aws_nat_gateway" "main" {
  count         = length(var.public_subnet_cidrs)
  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.public[count.index].id
  
  tags = merge(var.tags, {
    Name = "${var.tags["Project"]}-nat-${count.index + 1}"
  })
}

# Route Tables
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id
  
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }
  
  tags = merge(var.tags, {
    Name = "${var.tags["Project"]}-public-rt"
  })
}

resource "aws_route_table" "private" {
  count  = length(var.private_subnet_cidrs)
  vpc_id = aws_vpc.main.id
  
  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main[count.index].id
  }
  
  tags = merge(var.tags, {
    Name = "${var.tags["Project"]}-private-rt-${count.index + 1}"
  })
}

# Route Table Associations
resource "aws_route_table_association" "public" {
  count          = length(var.public_subnet_cidrs)
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "private" {
  count          = length(var.private_subnet_cidrs)
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private[count.index].id
}
```

**modules/vpc/outputs.tf:**
```hcl
output "vpc_id" {
  description = "ID of the VPC"
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "List of public subnet IDs"
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "List of private subnet IDs"
  value       = aws_subnet.private[*].id
}

output "nat_gateway_ids" {
  description = "List of NAT Gateway IDs"
  value       = aws_nat_gateway.main[*].id
}
```

### 5.3 Using Modules

**Root module:**
```hcl
module "vpc" {
  source = "./modules/vpc"
  
  vpc_cidr               = "10.0.0.0/16"
  availability_zones     = ["us-east-1a", "us-east-1b"]
  public_subnet_cidrs    = ["10.0.1.0/24", "10.0.2.0/24"]
  private_subnet_cidrs   = ["10.0.10.0/24", "10.0.20.0/24"]
  
  tags = {
    Project     = "SearchEngine"
    Environment = "Production"
  }
}

# Use module outputs
resource "aws_instance" "app" {
  subnet_id = module.vpc.private_subnet_ids[0]
  # ...
}
```

### 5.4 Public Module Registry

**Using public modules:**
```hcl
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "5.1.0"
  
  name = "my-vpc"
  cidr = "10.0.0.0/16"
  
  azs             = ["us-east-1a", "us-east-1b"]
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24"]
  public_subnets  = ["10.0.101.0/24", "10.0.102.0/24"]
  
  enable_nat_gateway = true
  enable_vpn_gateway = false
  
  tags = {
    Terraform   = "true"
    Environment = "dev"
  }
}
```

---

# Part IV: State Management (Weeks 7-8)

## Chapter 6: Understanding State

### 6.1 The State File Deep Dive

**What Terraform tracks:**
```json
{
  "version": 4,
  "terraform_version": "1.6.0",
  "serial": 42,
  "lineage": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "outputs": {},
  "resources": [
    {
      "mode": "managed",
      "type": "aws_instance",
      "name": "web",
      "provider": "provider[\"registry.terraform.io/hashicorp/aws\"]",
      "instances": [
        {
          "schema_version": 1,
          "attributes": {
            "id": "i-0123456789abcdef0",
            "ami": "ami-12345678",
            "instance_type": "t2.micro",
            "public_ip": "54.123.45.67",
            "private_ip": "10.0.1.15"
          },
          "sensitive_attributes": [],
          "dependencies": ["aws_vpc.main", "aws_subnet.public"]
        }
      ]
    }
  ]
}
```

**Key fields:**
- **serial:** Increments with each change (concurrent edit detection)
- **lineage:** UUID ensures correct state file
- **dependencies:** Resource creation order

### 6.2 Remote State

**Problem with Local State:**
```
Developer A              Developer B
    ↓                        ↓
terraform apply          terraform apply
    ↓                        ↓
local state              local state
    ↓                        ↓
  CONFLICT! Different states, infrastructure inconsistent!
```

**Solution: Remote Backend**
```
Developer A              Developer B
    ↓                        ↓
    └──→ S3 Backend ←────────┘
         (shared state)
         + DynamoDB lock
```

### 6.3 S3 Backend Configuration  

**backend.tf:**
```hcl
terraform {
  backend "s3" {
    bucket         = "my-terraform-state-bucket"
    key            = "prod/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "terraform-locks"
    encrypt        = true
  }
}
```

**Create S3 bucket (one-time setup):**
```hcl
# bootstrap/main.tf
resource "aws_s3_bucket" "terraform_state" {
  bucket = "my-terraform-state-bucket"
  
  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_versioning" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id
  
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id
  
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# DynamoDB for state locking
resource "aws_dynamodb_table" "terraform_locks" {
  name         = "terraform-locks"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "LockID"
  
  attribute {
    name = "LockID"
    type = "S"
  }
}
```

### 6.4 State Locking

**How it works:**
```
Developer A: terraform apply
    ↓
DynamoDB: Create lock "terraform-state-lock"
    ↓
Developer B: terraform apply
    ↓
DynamoDB: Lock exists! ERROR
    ↓
"Error: Error locking state: resource temporarily unavailable"
```

**Manual unlock (if crashed):**
```bash
terraform force-unlock LOCK_ID
```

### 6.5 State Commands

```bash
# List all resources
terraform state list

# Show specific resource
terraform state show aws_instance.web

# Move resource to different name
terraform state mv aws_instance.web aws_instance.web_server

# Remove resource from state (doesn't destroy)
terraform state rm aws_instance.old_server

# Import existing resource
terraform import aws_instance.web i-1234567890abcdef0

# Pull current state
terraform state pull > backup.tfstate

# Replace provider
terraform state replace-provider hashicorp/aws registry.terraform.io/hashicorp/aws
```

---

## Chapter 7: Workspaces

### 7.1 What are Workspaces?

**Single State File Problem:**
```
terraform/
└── main.tf
    ├── Creates VPC in us-east-1
    └── How to create same in us-west-2?
```

**Solution: Workspaces**
```
terraform/
└── main.tf
    ├── workspace: default → us-east-1
    ├── workspace: dev → us-east-1  
    └── workspace: prod → us-west-2
```

### 7.2 Workspace Commands

```bash
# List workspaces
terraform workspace list
# * default

# Create new workspace
terraform workspace new dev

# Switch workspace
terraform workspace select dev

# Show current workspace
terraform workspace show
# dev

# Delete workspace
terraform workspace delete dev
```

### 7.3 Using Workspaces in Code

```hcl
# Different instance types per workspace
resource "aws_instance" "web" {
  ami = "ami-12345678"
  
  instance_type = terraform.workspace == "prod" ? "t3.large" : "t2.micro"
  
  tags = {
    Name        = "web-${terraform.workspace}"
    Environment = terraform.workspace
  }
}

# Or use locals
locals {
  instance_config = {
    dev = {
      instance_type = "t2.micro"
      instance_count = 1
    }
    staging = {
      instance_type = "t3.medium"
      instance_count = 2
    }
    prod = {
      instance_type = "t3.large"
      instance_count = 5
    }
  }
  
  config = local.instance_config[terraform.workspace]
}

resource "aws_instance" "web" {
  count         = local.config.instance_count
  ami           = "ami-12345678"
  instance_type = local.config.instance_type
}
```

### 7.4 Workspace Best Practices

**✅ Good use cases:**
- Development environments
- Feature branches
- Testing

**❌ Avoid for:**
- Production vs Non-production (use separate state files)
- Multi-region (use modules)
- Different AWS accounts (separate backends)

**Recommended:**
```
environments/
├── dev/
│   ├── backend.tf
│   └── main.tf
├── staging/
│   ├── backend.tf
│   └── main.tf
└── prod/
    ├── backend.tf
    └── main.tf
```

---

# Part V: Advanced Patterns (Weeks 9-10)

## Chapter 8: Meta-Arguments

### 8.1 count

**Create multiple resources:**
```hcl
resource "aws_instance" "web" {
  count         = 3
  ami           = "ami-12345678"
  instance_type = "t2.micro"
  
  tags = {
    Name = "web-${count.index}"  # web-0, web-1, web-2
  }
}

# Access instances
output "instance_ids" {
  value = aws_instance.web[*].id
}
```

**Conditional creation:**
```hcl
resource "aws_instance" "web" {
  count = var.create_instance ? 1 : 0
  # Creates instance only if variable is true
}
```

### 8.2 for_each

**Better than count for maps:**
```hcl
variable "instances" {
  type = map(object({
    instance_type = string
    ami           = string
  }))
  
  default = {
    web = {
      instance_type = "t2.micro"
      ami           = "ami-12345678"
    }
    api = {
      instance_type = "t3.medium"
      ami           = "ami-87654321"
    }
  }
}

resource "aws_instance" "servers" {
  for_each      = var.instances
  ami           = each.value.ami
  instance_type = each.value.instance_type
  
  tags = {
    Name = each.key  # "web" or "api"
  }
}

# Access specific instance
output "web_server_id" {
  value = aws_instance.servers["web"].id
}
```

**for_each with sets:**
```hcl
resource "aws_iam_user" "developers" {
  for_each = toset(["alice", "bob", "charlie"])
  name     = each.key
}
```

### 8.3 depends_on

**Explicit dependencies:**
```hcl
resource "aws_s3_bucket" "data" {
  bucket = "my-data-bucket"
}

resource "aws_s3_bucket_policy" "data_policy" {
  bucket = aws_s3_bucket.data.id
  policy = jsonencode({...})
}

resource "aws_instance" "processor" {
  # Ensure bucket and policy exist first
  depends_on = [
    aws_s3_bucket.data,
    aws_s3_bucket_policy.data_policy
  ]
  
  ami           = "ami-12345678"
  instance_type = "t2.micro"
}
```

### 8.4 lifecycle

```hcl
resource "aws_instance" "web" {
  ami           = "ami-12345678"
  instance_type = "t2.micro"
  
  lifecycle {
    # Prevent accidental deletion
    prevent_destroy = true
    
    # Create new before destroying old (zero downtime)
    create_before_destroy = true
    
    # Ignore changes to tags (modified externally)
    ignore_changes = [
      tags,
      user_data
    ]
  }
}
```

---

## Chapter 9: Functions & Expressions

### 9.1 Built-in Functions

**String functions:**
```hcl
locals {
  # Concatenation
  full_name = join("-", ["search", "engine", "prod"])  # "search-engine-prod"
  
  # Formatting
  server_name = format("web-%03d", 42)  # "web-042"
  
  # Manipulation
  upper_env = upper(var.environment)  # "PRODUCTION"
  lower_env = lower(var.environment)  # "production"
  
  # Replacement
  clean_name = replace(var.bucket_name, "_", "-")
}
```

**Collection functions:**
```hcl
locals {
  # Merge maps
  all_tags = merge(var.common_tags, var.specific_tags)
  
  # Lookup with default
  instance_type = lookup(var.instance_types, var.environment, "t2.micro")
  
  # Element (wraparound)
  az = element(var.availability_zones, count.index)
  
  # Slice
  first_two_azs = slice(var.availability_zones, 0, 2)
  
  # Flatten nested lists
  all_subnets = flatten([var.public_subnets, var.private_subnets])
}
```

**Type conversion:**
```hcl
locals {
  # String to number
  port_number = tonumber("8080")
  
  # To set (removes duplicates)
unique_azs = toset(["us-east-1a", "us-east-1a", "us-east-1b"])
  
  # To map
  instance_map = tomap({
    "small"  = "t2.micro"
    "medium" = "t3.medium"
  })
}
```

### 9.2 Dynamic Blocks

**Without dynamic:**
```hcl
resource "aws_security_group" "web" {
  name = "web-sg"
  
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/16"]
  }
}
```

**With dynamic:**
```hcl
variable "ingress_rules" {
  type = list(object({
    port        = number
    protocol    = string
    cidr_blocks = list(string)
  }))
  
  default = [
    {
      port        = 80
      protocol    = "tcp"
      cidr_blocks = ["0.0.0.0/0"]
    },
    {
      port        = 443
      protocol    = "tcp"
      cidr_blocks = ["0.0.0.0/0"]
    },
    {
      port        = 22
      protocol    = "tcp"
      cidr_blocks = ["10.0.0.0/16"]
    }
  ]
}

resource "aws_security_group" "web" {
  name = "web-sg"
  
  dynamic "ingress" {
    for_each = var.ingress_rules
    
    content {
      from_port   = ingress.value.port
      to_port     = ingress.value.port
      protocol    = ingress.value.protocol
      cidr_blocks = ingress.value.cidr_blocks
    }
  }
}
```

### 9.3 Conditionals

**Ternary operator:**
```hcl
locals {
  # condition ? true_value : false_value
  instance_type = var.environment == "prod" ? "t3.large" : "t2.micro"
  
  # Nested
  db_instance_class = (
    var.environment == "prod" ? "db.r5.large" :
    var.environment == "staging" ? "db.t3.medium" :
    "db.t3.micro"
  )
}
```

**Conditional resources:**
```hcl
resource "aws_instance" "bastion" {
  count = var.create_bastion ? 1 : 0
  
  ami           = "ami-12345678"
  instance_type = "t2.micro"
}
```

---

## Chapter 10: Testing & Validation

### 10.1 Terraform Validate

```bash
terraform validate
```

**Checks:**
- Syntax errors
- Invalid variable references
- Type mismatches

### 10.2 Terraform Plan

```bash
# Basic plan
terraform plan

# Save plan to file
terraform plan -out=tfplan

# Apply saved plan
terraform apply tfplan

# Destroy plan
terraform plan -destroy
```

### 10.3 Terratest (Go-based testing)

**Install:**
```bash
go get github.com/gruntwork-io/terratest
```

**Test file (test/vpc_test.go):**
```go
package test

import (
    "testing"
    "github.com/gruntwork-io/terratest/modules/terraform"
    "github.com/stretchr/testify/assert"
)

func TestVPCCreation(t *testing.T) {
    terraformOptions := &terraform.Options{
        TerraformDir: "../examples/vpc",
        Vars: map[string]interface{}{
            "vpc_cidr": "10.0.0.0/16",
        },
    }
    
    defer terraform.Destroy(t, terraformOptions)
    
    terraform.InitAndApply(t, terraformOptions)
    
    vpcID := terraform.Output(t, terraformOptions, "vpc_id")
    assert.NotEmpty(t, vpcID)
}
```

---

# Part VI: Production Patterns (Week 11)

## Chapter 11: Production Best Practices

### 11.1 Directory Structure

```
terraform/
├── environments/
│   ├── dev/
│   │   ├── backend.tf
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   └── terraform.tfvars
│   ├── staging/
│   │   └── ...
│   └── prod/
│       └── ...
├── modules/
│   ├── vpc/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   └── README.md
│   ├── ecs/
│   │   └── ...
│   └── rds/
│       └── ...
├── .gitignore
└── README.md
```

### 11.2 Naming Conventions

```hcl
# Resources: type_description
resource "aws_instance" "web_server" {}
resource "aws_s3_bucket" "static_assets" {}

# Variables: descriptive_name
variable "vpc_cidr_block" {}
variable "database_instance_class" {}

# Outputs: resource_attribute
output "vpc_id" {}
output "instance_public_ip" {}

# Modules: purpose
module "networking" {}
module "application_tier" {}
```

### 11.3 Security Best Practices

**1. Never commit secrets:**
```hcl
# DON'T
variable "db_password" {
  default = "MyPassword123"  # ❌ NEVER!
}

# DO
variable "db_password" {
  type      = string
  sensitive = true
  # No default - must be provided securely
}
```

**2. Use AWS Secrets Manager:**
```hcl
data "aws_secretsmanager_secret_version" "db_password" {
  secret_id = "prod/database/password"
}

resource "aws_db_instance" "main" {
  password = data.aws_secretsmanager_secret_version.db_password.secret_string
}
```

**3. Least privilege IAM:**
```hcl
data "aws_iam_policy_document" "ec2_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]
    
    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_kms_key" "encryption" {
  description             = "Encryption key"
  deletion_window_in_days = 10
  enable_key_rotation     = true
}
```

### 11.4 Code Review Checklist

**Before Merge:**
- [ ] `terraform fmt` run
- [ ] `terraform validate` passes
- [ ] `terraform plan` reviewed
- [ ] No secrets in code
- [ ] Variables documented
- [ ] Outputs defined
- [ ] README updated
- [ ] Tests passing

---

## Chapter 12: Troubleshooting

### 12.1 Common Errors

**Error: "Error locking state"**
```
Error: Error locking state: resource temporarily unavailable
Lock Info:
  ID:        abc123-def456-...
  Path:      terraform.tfstate
  Operation: OperationTypeApply
  Who:       user@hostname
```

**Solution:**
```bash
# If you're sure no one is running terraform:
terraform force-unlock abc123-def456-...

# Better: Wait or coordinate with team
```

**Error: "Provider configuration not found"**
```
Error: Provider configuration not found
```

**Solution:**
```bash
terraform init  # Re-download providers
```

**Error: "Cycle in resource dependencies"**
```
Error: Cycle: aws_security_group.web, aws_instance.web
```

**Solution:** Remove circular reference, use `depends_on` properly

### 12.2 Debugging

```bash
# Enable detailed logs
export TF_LOG=DEBUG
export TF_LOG_PATH=terraform.log

terraform apply

# Disable
unset TF_LOG
```

**Log levels:**
- TRACE (most verbose)
- DEBUG
- INFO
- WARN
- ERROR

---

# Part VII: Final Project (Week 12)

## Real-World Project: Multi-Tier Web Application

**Requirements:**
- 3 environments (dev, staging, prod)
- VPC with public/private subnets
- Application Load Balancer
- Auto-scaling ECS cluster
- RDS database with Multi-AZ
- S3 for static assets
- CloudFront CDN
- Route 53 DNS
- CloudWatch monitoring
- All state in S3 with locking

**Deliverables:**
1. Complete Terraform code
2. README with architecture diagram
3. Deployment guide
4. Cost estimate
5. Security review

---

## 📝 Exam Preparation

### Terraform Associate Certification Topics

1. **IaC Concepts** (20%)
   - Benefits of IaC
   - Multi-cloud deployment

2. **Terraform Basics** (25%)
   - Install and version
   - Terraform workflow (init, plan, apply)
   - Terraform state

3. **Terraform Configuration** (30%)
   - Resources and data sources
   - Variables and outputs
   - Resource addressing

4. **Terraform Modules** (15%)
   - Module sources
   - Module inputs/outputs
   - Module versioning

5. **Terraform Workflow** (10%)
   - Terraform Cloud/Enterprise
   - Remote state
   - Sentinel policies

### Sample Questions

**Q1:** What command creates infrastructure?
- A) `terraform create`
- B) `terraform build`
- C) `terraform apply` ✓
- D) `terraform provision`

**Q2:** How to reference another resource's attribute?
```hcl
resource "aws_instance" "web" {
  ami = "ami-12345"
}

resource "aws_eip" "web_ip" {
  instance = ?
}
```
- A) `aws_instance.web`
- B) `aws_instance.web.id` ✓
- C) `${aws_instance.web.id}`
- D) `resource.aws_instance.web.id`

---

## 🎓 Graduation Checklist

By completing this course, you should be able to:

- [ ] Explain IaC benefits
- [ ] Write Terraform configurations
- [ ] Use variables and outputs
- [ ] Create and use modules
- [ ] Manage state remotely
- [ ] Use workspaces
- [ ] Implement for_each and count
- [ ] Write dynamic blocks
- [ ] Test Terraform code
- [ ] Deploy multi-environment infrastructure
- [ ] Debug Terraform issues
- [ ] Pass Terraform Associate exam

---

## 📚 Additional Resources

**Official Documentation:**
- https://terraform.io/docs
- https://registry.terraform.io (module registry)

**Books:**
- "Terraform: Up & Running" by Yevgeniy Brikman
- "Infrastructure as Code" by Kief Morris

**Practice:**
- HashiCorp Learn: https://learn.hashicorp.com/terraform
- AWS Free Tier: https://aws.amazon.com/free

**Community:**
- HashiCorp Discuss: https://discuss.hashicorp.com
- Terraform GitHub: https://github.com/hashicorp/terraform

---

*Congratulations on completing the Terraform course! You now have the skills to manage infrastructure at scale using Infrastructure as Code. Keep practicing and stay updated with Terraform releases!*

**Final Advice:** The best way to learn is by doing. Start with small projects and gradually increase complexity. Always read documentation, test in dev before prod, and never stop learning!

— Professor's signature
