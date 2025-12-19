# GitHub Actions CI/CD Pipeline

> **Complete automation from code commit to production deployment**

---

## Workflow Architecture

```
Push to main/develop
    ↓
Build & Test (CI)
    ↓
Docker Build & Push to ECR
    ↓
Deploy to Environment
    ↓
Smoke Tests
    ↓
Notify Team
```

---

## Complete CI Workflow

**.github/workflows/ci.yml**

```yaml
name: CI - Build and Test

on:
  push:
    branches: [ main, develop, 'feature/**' ]
  pull_request:
    branches: [ main, develop ]

env:
  JAVA_VERSION: '21'
  NODE_VERSION: '18'

jobs:
  backend-test:
    name: Backend Tests
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_DB: test_db
          POSTGRES_USER: test_user
          POSTGRES_PASSWORD: test_password
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432
      
      redis:
        image: redis:7-alpine
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 6379:6379

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Run unit tests
        working-directory: ./search-engine
        run: mvn test
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/test_db
          SPRING_DATASOURCE_USERNAME: test_user
          SPRING_DATASOURCE_PASSWORD: test_password

      - name: Run integration tests
        working-directory: ./search-engine
        run: mvn verify -Pintegration-tests

      - name: Publish test results
        uses: EnricoMi/publish-unit-test-result-action@v2
        if: always()
        with:
          files: search-engine/target/surefire-reports/*.xml

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./search-engine/target/site/jacoco/jacoco.xml

  backend-build:
    name: Build Backend JAR
    runs-on: ubuntu-latest
    needs: backend-test

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Build JAR
        working-directory: ./search-engine
        run: mvn clean package -DskipTests

      - name: Upload artifact
        uses: actions/upload-artifact@v3
        with:
          name: backend-jar
          path: search-engine/target/*.jar
          retention-days: 7

  frontend-build:
    name: Build Frontend
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

      - name: Run ESLint
        working-directory: ./search-engine-ui
        run: npm run lint

      - name: Run TypeScript check
        working-directory: ./search-engine-ui
        run: npm run type-check

      - name: Build production bundle
        working-directory: ./search-engine-ui
        run: npm run build
        env:
          NEXT_PUBLIC_API_URL: ${{ secrets.API_URL }}

      - name: Upload build
        uses: actions/upload-artifact@v3
        with:
          name: frontend-build
          path: search-engine-ui/.next

  docker-build:
    name: Build Docker Images
    runs-on: ubuntu-latest
    needs: [backend-build, frontend-build]
    if: github.ref == 'refs/heads/main' || github.ref == 'refs/heads/develop'

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
          aws-region: us-east-1

      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Build and push backend image
        uses: docker/build-push-action@v5
        with:
          context: ./search-engine
          file: ./search-engine/Dockerfile
          push: true
          tags: |
            ${{ steps.login-ecr.outputs.registry }}/search-engine-backend:${{ github.sha }}
            ${{ steps.login-ecr.outputs.registry }}/search-engine-backend:latest
          cache-from: type=gha
          cache-to: type=gha,mode=max

      - name: Build and push frontend image
        uses: docker/build-push-action@v5
        with:
          context: ./search-engine-ui
          file: ./search-engine-ui/Dockerfile
          push: true
          tags: |
            ${{ steps.login-ecr.outputs.registry }}/search-engine-frontend:${{ github.sha }}
            ${{ steps.login-ecr.outputs.registry }}/search-engine-frontend:latest

  security-scan:
    name: Security Scanning
    runs-on: ubuntu-latest
    needs: docker-build

    steps:
      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: ${{ steps.login-ecr.outputs.registry }}/search-engine-backend:latest
          format: 'sarif'
          output: 'trivy-results.sarif'

      - name: Upload Trivy results to GitHub Security
        uses: github/codeql-action/upload-sarif@v2
        with:
          sarif_file: 'trivy-results.sarif'
```

---

## CD Workflow - Production

**.github/workflows/cd-prod.yml**

```yaml
name: CD - Deploy to Production

on:
  release:
    types: [published]
  workflow_dispatch:
    inputs:
      version:
        description: 'Version to deploy'
        required: true

env:
  AWS_REGION: us-east-1
  ENVIRONMENT: prod

jobs:
  deploy:
    name: Deploy to Production
    runs-on: ubuntu-latest
    environment:
      name: production
      url: https://searchengine.com

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}
          role-to-assume: ${{ secrets.AWS_DEPLOY_ROLE_ARN }}
          role-duration-seconds: 3600

      - name: Get image tag
        id: image
        run: |
          if [ "${{ github.event_name }}" == "workflow_dispatch" ]; then
            echo "tag=${{ github.event.inputs.version }}" >> $GITHUB_OUTPUT
          else
            echo "tag=${{ github.ref_name }}" >> $GITHUB_OUTPUT
          fi

      - name: Update ECS service - Backend
        run: |
          aws ecs update-service \
            --cluster search-engine-prod-cluster \
            --service search-engine-prod-backend-service \
            --force-new-deployment \
            --task-definition search-engine-backend:${{ steps.image.outputs.tag }}

      - name: Wait for deployment stability
        run: |
          aws ecs wait services-stable \
            --cluster search-engine-prod-cluster \
            --services search-engine-prod-backend-service

      - name: Deploy frontend to S3
        run: |
          aws s3 sync search-engine-ui/out s3://search-engine-prod-frontend \
            --delete \
            --cache-control "public,max-age=31536000,immutable"

      - name: Invalidate CloudFront cache
        run: |
          aws cloudfront create-invalidation \
            --distribution-id ${{ secrets.CLOUDFRONT_DISTRIBUTION_ID }} \
            --paths "/*"

  smoke-tests:
    name: Run Smoke Tests
    runs-on: ubuntu-latest
    needs: deploy

    steps:
      - name: Health check
        run: |
          for i in {1..10}; do
            response=$(curl -s -o /dev/null -w "%{http_code}" https://api.searchengine.com/actuator/health)
            if [ $response -eq 200 ]; then
              echo "Health check passed"
              exit 0
            fi
            echo "Attempt $i failed, retrying..."
            sleep 10
          done
          echo "Health check failed after 10 attempts"
          exit 1

      - name: Test search endpoint
        run: |
          response=$(curl -s "https://api.searchengine.com/api/v1/search?query=test")
          echo "Search response: $response"
          
          # Verify response contains results
          if echo "$response" | jq -e '.results' > /dev/null; then
            echo "Search endpoint working"
          else
            echo "Search endpoint failed"
            exit 1
          fi

  rollback:
    name: Rollback on Failure
    runs-on: ubuntu-latest
    needs: smoke-tests
    if: failure()

    steps:
      - name: Rollback deployment
        run: |
          echo "Rolling back to previous version..."
          
          # Get previous task definition
          PREV_TASK_DEF=$(aws ecs describe-services \
            --cluster search-engine-prod-cluster \
            --services search-engine-prod-backend-service \
            --query 'services[0].deployments[1].taskDefinition' \
            --output text)
          
          # Update to previous version
          aws ecs update-service \
            --cluster search-engine-prod-cluster \
            --service search-engine-prod-backend-service \
            --task-definition $PREV_TASK_DEF \
            --force-new-deployment

      - name: Notify team
        uses: slackapi/slack-github-action@v1
        with:
          channel-id: 'deployments'
          slack-message: |
            ⚠️ Production deployment FAILED and was rolled back
            Commit: ${{ github.sha }}
            Author: ${{ github.actor }}
        env:
          SLACK_BOT_TOKEN: ${{ secrets.SLACK_BOT_TOKEN }}
```

---

## Secrets Management

### GitHub Secrets Required

```bash
# AWS Credentials
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
AWS_DEPLOY_ROLE_ARN

# Application Secrets
DB_PASSWORD
REDIS_PASSWORD
API_SECRET_KEY

# External Services
CLOUDFRONT_DISTRIBUTION_ID
SLACK_BOT_TOKEN
CODECOV_TOKEN
```

### Setting Secrets

```bash
# Via GitHub CLI
gh secret set AWS_ACCESS_KEY_ID -b "AKIA..."

# Or via UI
Settings → Secrets and variables → Actions → New repository secret
```

---

## Caching Strategy

```yaml
- name: Cache Maven packages
  uses: actions/cache@v3
  with:
    path: ~/.m2
    key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
    restore-keys: |
      ${{ runner.os }}-m2-

- name: Cache npm packages
  uses: actions/cache@v3
  with:
    path: ~/.npm
    key: ${{ runner.os }}-npm-${{ hashFiles('**/package-lock.json') }}
```

---

## Deployment Environments

```yaml
environment:
  name: production
  url: https://searchengine.com
```

**Protection Rules:**
- Require approval from 2 reviewers
- Wait 5 minutes before deployment
- Restrict to specific branches (main only)

---

*Complete CI/CD automation with GitHub Actions ensures fast, reliable deployments!*
