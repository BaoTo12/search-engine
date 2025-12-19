# Kubernetes: From Containers to Cloud-Native Orchestration
## A Comprehensive University-Level Course

> **Professor's Note:** This course transforms you from container beginner to Kubernetes expert. You'll learn by doing, building production-ready clusters and applications. Complete all labs!

---

## 📚 Course Syllabus

**Total Duration:** 14 Weeks  
**Prerequisites:** Basic Linux, Docker fundamentals, networking concepts  
**Certification Prep:** CKA (Certified Kubernetes Administrator), CKAD (Certified Kubernetes Application Developer)

### Course Outline

1. **Week 1:** Container Fundamentals & Docker Review
2. **Week 2:** Kubernetes Architecture & Concepts
3. **Week 3:** Pods & Workload Resources
4. **Week 4:** Services & Networking
5. **Week 5:** Storage & Persistent Volumes
6. **Week 6:** ConfigMaps, Secrets & Environment
7. **Week 7:** Deployments & Rolling Updates
8. **Week 8:** Auto-Scaling & Resource Management
9. **Week 9:** StatefulSets & DaemonSets
10. **Week 10:** Helm & Package Management
11. **Week 11:** Security & RBAC
12. **Week 12:** Monitoring & Logging
13. **Week 13:** Production Patterns & Best Practices
14. **Week 14:** Final Project - Microservices Platform

---

# Part I: Foundations (Weeks 1-2)

## Chapter 1: Container Fundamentals

### 1.1 Why Containers?

**The "Works on My Machine" Problem:**

```
Developer's Laptop        Production Server
├─ Python 3.9            ├─ Python 3.7  ❌
├─ Node.js 18            ├─ Node.js 14  ❌
├─ PostgreSQL 15         ├─ PostgreSQL 13  ❌
└─ Redis 7               └─ Redis 6  ❌

Result: APPLICATION CRASHES! 💥
```

**Container Solution:**

```
┌─────────────────────────────────────┐
│        Docker Container             │
│  ┌───────────────────────────────┐ │
│  │    Your Application           │ │
│  │    + All Dependencies         │ │
│  │    + Exact Runtime            │ │
│  └───────────────────────────────┘ │
│                                     │
│  Runs identically everywhere!       │
└─────────────────────────────────────┘
```

### 1.2 Containers vs Virtual Machines

```
Virtual Machines                    Containers
┌─────────────────────┐            ┌─────────────────────┐
│   Application 1     │            │   Application 1     │
├─────────────────────┤            ├─────────────────────┤
│    Libraries        │            │    Container        │
├─────────────────────┤            │   Runtime Layer     │
│   Guest OS (GB)     │            └─────────────────────┘
├─────────────────────┤            ┌─────────────────────┐
│   Hypervisor        │            │   Application 2     │
├─────────────────────┤            ├─────────────────────┤
│   Host OS           │            │    Container        │
├─────────────────────┤            │   Runtime Layer     │
│   Hardware          │            └─────────────────────┘
└─────────────────────┘            ┌─────────────────────┐
                                   │   Docker Engine     │
Start time: Minutes                ├─────────────────────┤
Size: GBs                          │   Host OS           │
Overhead: High                     ├─────────────────────┤
                                   │   Hardware          │
                                   └─────────────────────┘
                                   Start time: Seconds
                                   Size: MBs
                                   Overhead: Minimal
```

**Key Differences:**

| Feature | VM | Container |
|---------|-------|-----------|
| **Isolation** | Hardware-level | Process-level |
| **Size** | GBs | MBs |
| **Boot time** | Minutes | Seconds |
| **Resource usage** | Heavy | Light |
| **Portability** | Moderate | Excellent |

### 1.3 Docker Quick Review

**Basic Docker Commands:**

```bash
# Build image from Dockerfile
docker build -t myapp:1.0 .

# Run container
docker run -d -p 8080:8080 --name myapp myapp:1.0

# List running containers
docker ps

# View logs
docker logs myapp

# Execute command in container
docker exec -it myapp bash

# Stop and remove
docker stop myapp
docker rm myapp

# Push to registry
docker tag myapp:1.0 dockerhub.com/username/myapp:1.0
docker push dockerhub.com/username/myapp:1.0
```

---

## Chapter 2: Kubernetes Architecture

### 2.1 What is Kubernetes?

**Definition:** Container orchestration platform for automating deployment, scaling, and management of containerized applications.

**Created by:** Google (2014), now maintained by CNCF (Cloud Native Computing Foundation)

**Key Features:**
- ✅ Automatic scaling
- ✅ Self-healing
- ✅ Load balancing
- ✅ Rolling updates
- ✅ Service discovery
- ✅ Storage orchestration

### 2.2 Kubernetes Architecture

```
┌────────────────────────────────────────────────────────────┐
│                   Kubernetes Cluster                        │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐ │
│  │              Control Plane (Master)                   │ │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐           │ │
│  │  │   API    │  │  etcd    │  │Scheduler │           │ │
│  │  │  Server  │  │(Database)│  │          │           │ │
│  │  └──────────┘  └──────────┘  └──────────┘           │ │
│  │  ┌──────────────────────────────────────┐            │ │
│  │  │  Controller Manager                   │            │ │
│  │  │  - Deployment Controller              │            │ │
│  │  │  - ReplicaSet Controller              │            │ │
│  │  │  - Service Controller                 │            │ │
│  │  └──────────────────────────────────────┘            │ │
│  └──────────────────────────────────────────────────────┘ │
│                           ↓                                 │
│  ┌──────────────────────────────────────────────────────┐ │
│  │                  Worker Nodes                         │ │
│  │                                                        │ │
│  │  ┌─────────────────┐  ┌─────────────────┐           │ │
│  │  │  Node 1         │  │  Node 2         │           │ │
│  │  │  ┌───────────┐  │  │  ┌───────────┐  │           │ │
│  │  │  │  Pod 1    │  │  │  │  Pod 3    │  │           │ │
│  │  │  │ Container │  │  │  │ Container │  │           │ │
│  │  │  └───────────┘  │  │  └───────────┘  │           │ │
│  │  │  ┌───────────┐  │  │  ┌───────────┐  │           │ │
│  │  │  │  Pod 2    │  │  │  │  Pod 4    │  │           │ │
│  │  │  │ Container │  │  │  │ Container │  │           │ │
│  │  │  └───────────┘  │  │  └───────────┘  │           │ │
│  │  │                 │  │                 │           │ │
│  │  │  kubelet        │  │  kubelet        │           │ │
│  │  │  kube-proxy     │  │  kube-proxy     │           │ │
│  │  └─────────────────┘  └─────────────────┘           │ │
│  └──────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────┘
```

### 2.3 Control Plane Components

**1. API Server (kube-apiserver)**
- Front-end for Kubernetes control plane
- Exposes REST API
- Validates and processes requests
- Only component that talks to etcd

**2. etcd**
- Distributed key-value store
- Stores all cluster data
- Source of truth for cluster state
- Must be backed up!

**3. Scheduler (kube-scheduler)**
- Watches for new Pods without assigned node
- Selects optimal node based on:
  - Resource requirements
  - Node constraints
  - Affinity/anti-affinity rules
  - Data locality

**4. Controller Manager (kube-controller-manager)**
- Runs controller processes:
  - **Node Controller:** Monitors node health
  - **Replication Controller:** Maintains correct number of pods
  - **Endpoints Controller:** Populates service endpoints
  - **Service Account Controller:** Creates default accounts

**5. Cloud Controller Manager**
- Interacts with cloud provider APIs
- Manages load balancers, storage, networking

### 2.4 Node Components

**1. kubelet**
- Agent running on each node
- Ensures containers are running in Pods
- Reports node status to API server

**2. kube-proxy**
- Network proxy on each node
- Implements Kubernetes Service concept
- Maintains network rules for pod communication

**3. Container Runtime**
- Software to run containers
- Examples: Docker, containerd, CRI-O

### 2.5 Installing Kubernetes

**Development (Local):**

```bash
# Option 1: Minikube (single-node cluster)
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube
minikube start

# Option 2: Kind (Kubernetes in Docker)
curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-linux-amd64
chmod +x ./kind
sudo mv ./kind /usr/local/bin/kind
kind create cluster

# Option 3: Docker Desktop (macOS/Windows)
# Enable Kubernetes in Docker Desktop settings
```

**Production:**

```bash
# AWS EKS
eksctl create cluster --name prod-cluster --region us-east-1

# Google GKE
gcloud container clusters create prod-cluster --zone us-central1-a

# Azure AKS
az aks create --resource-group myRG --name prod-cluster

# Self-managed with kubeadm
kubeadm init --pod-network-cidr=10.244.0.0/16
```

**kubectl Installation:**

```bash
# Linux
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# Verify
kubectl version --client
```

---

# Part II: Core Resources (Weeks 3-4)

## Chapter 3: Pods - The Smallest Unit

### 3.1 What is a Pod?

**Definition:** Smallest deployable unit in Kubernetes; group of one or more containers with shared storage/network.

**Analogy:** Pod = Apartment, Containers = Roommates
- Share same address (IP)
- Share utilities (storage volumes)
- Live together, die together

### 3.2 Your First Pod

**pod.yaml:**
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: nginx-pod
  labels:
    app: nginx
    environment: dev
spec:
  containers:
  - name: nginx
    image: nginx:1.25
    ports:
    - containerPort: 80
```

**Create and manage:**
```bash
# Create pod
kubectl apply -f pod.yaml

# List pods
kubectl get pods

# Output:
# NAME        READY   STATUS    RESTARTS   AGE
# nginx-pod   1/1     Running   0          10s

# Describe pod (detailed info)
kubectl describe pod nginx-pod

# View logs
kubectl logs nginx-pod

# Execute command
kubectl exec -it nginx-pod -- bash

# Delete pod
kubectl delete pod nginx-pod
```

### 3.3 Multi-Container Pods

**Patterns:**

**1. Sidecar Pattern:**
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-with-logging
spec:
  containers:
  - name: app
    image: myapp:1.0
    volumeMounts:
    - name: logs
      mountPath: /var/log/app
  
  - name: log-shipper  # Sidecar
    image: fluentd:latest
    volumeMounts:
    - name: logs
      mountPath: /var/log/app
  
  volumes:
  - name: logs
    emptyDir: {}
```

**2. Ambassador Pattern:**
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-with-proxy
spec:
  containers:
  - name: app
    image: myapp:1.0
    env:
    - name: DATABASE_HOST
      value: localhost  # Talks to ambassador
  
  - name: db-proxy  # Ambassador
    image: cloudsql-proxy:latest
    # Proxies connections to Cloud SQL
```

**3. Adapter Pattern:**
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-with-adapter
spec:
  containers:
  - name: app
    image: legacy-app:1.0
    # Produces logs in custom format
  
  - name: log-adapter  # Adapter
    image: log-normalizer:latest
    # Converts logs to standard format
```

### 3.4 Pod Lifecycle

```
Pending → Running → Succeeded/Failed
           ↓
        Terminating
```

**Phases:**

1. **Pending:** Accepted but not running (downloading image, scheduling)
2. **Running:** At least one container is running
3. **Succeeded:** All containers terminated successfully
4. **Failed:** At least one container failed
5. **Unknown:** State cannot be determined

**Container States:**
- **Waiting:** Not running (pulling image, waiting for resources)
- **Running:** Executing
- **Terminated:** Finished or failed

### 3.5 Init Containers

**Run before app containers, used for:**
- Database migrations
- Configuration setup
- Waiting for dependencies

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-with-init
spec:
  initContainers:
  - name: wait-for-db
    image: busybox:1.35
    command: ['sh', '-c', 'until nc -z database 5432; do sleep 1; done']
  
  - name: run-migrations
    image: myapp:1.0
    command: ['python', 'manage.py', 'migrate']
  
  containers:
  - name: app
    image: myapp:1.0
    # Only starts after init containers succeed
```

### 3.6 Probes (Health Checks)

**1. Liveness Probe:** Is container alive?
```yaml
livenessProbe:
  httpGet:
    path: /healthz
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3
  # Restart container if fails 3 times
```

**2. Readiness Probe:** Ready to accept traffic?
```yaml
readinessProbe:
  httpGet:
    path: /ready
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
  # Remove from service if not ready
```

**3. Startup Probe:** Has app started? (for slow apps)
```yaml
startupProbe:
  httpGet:
    path: /startup
    port: 8080
  failureThreshold: 30
  periodSeconds: 10
  # 30 × 10s = 5 min to start
```

---

## Chapter 4: Workload Resources

### 4.1 ReplicaSets

**Purpose:** Maintain stable set of replica Pods

**replicaset.yaml:**
```yaml
apiVersion: apps/v1
kind: ReplicaSet
metadata:
  name: nginx-replicaset
spec:
  replicas: 3  # Desired number
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:1.25
        ports:
        - containerPort: 80
```

**Lab Exercise:**
```bash
# Create ReplicaSet
kubectl apply -f replicaset.yaml

# Watch pods being created
kubectl get pods -w

# Scale replicas
kubectl scale replicaset nginx-replicaset --replicas=5

# Delete a pod - watch it get recreated!
kubectl delete pod nginx-replicaset-xxxxx

# Verify still 5 pods running
kubectl get pods
```

### 4.2 Deployments (Most Important!)

**Why Deployments > ReplicaSets:**
- ✅ Rolling updates
- ✅ Rollback capability
- ✅ Version history
- ✅ Declarative updates

**deployment.yaml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend-deployment
  labels:
    app: backend
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1        # Max pods above desired during update
      maxUnavailable: 1  # Max pods unavailable during update
  selector:
    matchLabels:
      app: backend
  template:
    metadata:
      labels:
        app: backend
        version: v1.0
    spec:
      containers:
      - name: backend
        image: myapp:1.0
        ports:
        - containerPort: 8080
        env:
        - name: DATABASE_URL
          value: "postgresql://db:5432/mydb"
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
```

**Deployment Commands:**
```bash
# Create deployment
kubectl apply -f deployment.yaml

# Check rollout status
kubectl rollout status deployment/backend-deployment

# View deployment history
kubectl rollout history deployment/backend-deployment

# Update image (triggers rolling update)
kubectl set image deployment/backend-deployment backend=myapp:2.0

# Watch rolling update
kubectl get pods -w

# Rollback to previous version
kubectl rollout undo deployment/backend-deployment

# Rollback to specific revision
kubectl rollout undo deployment/backend-deployment --to-revision=2

# Pause rollout (for canary testing)
kubectl rollout pause deployment/backend-deployment

# Resume rollout
kubectl rollout resume deployment/backend-deployment

# Scale deployment
kubectl scale deployment backend-deployment --replicas=10
```

### 4.3 Rolling Update Strategy

**Blue-Green Update:**
```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 100%      # Double pods during update
    maxUnavailable: 0   # Zero downtime
```

**Sequence:**
```
Initial: 3 pods v1.0

Step 1: Create 3 new pods v2.0 (maxSurge=100%)
  [v1.0] [v1.0] [v1.0] [v2.0] [v2.0] [v2.0]

Step 2: Delete 3 old pods v1.0
  [v2.0] [v2.0] [v2.0]

Result: Zero downtime upgrade!
```

**Canary Update:**
```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 1
    maxUnavailable: 0
```

**Sequence:**
```
Initial: 3 pods v1.0

Step 1: Create 1 new pod v2.0 (10% traffic)
  [v1.0] [v1.0] [v1.0] [v2.0]
  Pause → Monitor metrics → If OK, continue

Step 2: Delete 1 old pod, create 1 new
  [v1.0] [v1.0] [v2.0] [v2.0]

Step 3: Repeat until complete
  [v2.0] [v2.0] [v2.0]
```

---

# Part III: Networking (Week 4)

## Chapter 5: Services

### 5.1 What is a Service?

**Problem:** Pods have dynamic IPs that change on restart

**Solution:** Service provides stable IP/DNS for pod group

### 5.2 Service Types

**1. ClusterIP (Default)** - Internal only
```yaml
apiVersion: v1
kind: Service
metadata:
  name: backend-service
spec:
  type: ClusterIP
  selector:
    app: backend
  ports:
  - protocol: TCP
    port: 80        # Service port
    targetPort: 8080  # Pod port
```

**Access:**
```bash
# From within cluster:
curl http://backend-service
curl http://backend-service.default.svc.cluster.local
```

**2. NodePort** - Exposed on each node
```yaml
apiVersion: v1
kind: Service
metadata:
  name: web-service
spec:
  type: NodePort
  selector:
    app: web
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
    nodePort: 30080  # Port on node (30000-32767)
```

**Access:**
```bash
# From outside cluster:
curl http://<node-ip>:30080
```

**3. LoadBalancer** - Cloud load balancer
```yaml
apiVersion: v1
kind: Service
metadata:
  name: public-web
spec:
  type: LoadBalancer
  selector:
    app: web
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
```

**Creates:**
- AWS: ELB/ALB
- GCP: Cloud Load Balancer
- Azure: Azure Load Balancer

**4. ExternalName** - DNS alias
```yaml
apiVersion: v1
kind: Service
metadata:
  name: database
spec:
  type: ExternalName
  externalName: rds.us-east-1.amazonaws.com
```

### 5.3 Service Discovery

**DNS-Based:**
```yaml
# Pod A can reach Service B at:
backend-service                    # Same namespace
backend-service.production         # Different namespace
backend-service.production.svc.cluster.local  # FQDN
```

**Environment Variables:**
```bash
# Kubernetes injects env vars for services
BACKEND_SERVICE_HOST=10.96.0.10
BACKEND_SERVICE_PORT=80
```

### 5.4 Endpoints

**What are Endpoints?** IP addresses of pods backing a service

```bash
# View endpoints
kubectl get endpoints backend-service

# NAME              ENDPOINTS
# backend-service   10.244.1.5:8080,10.244.2.3:8080,10.244.3.1:8080
```

**Manual Endpoints (for external services):**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: external-db
spec:
  ports:
  - port: 5432
---
apiVersion: v1
kind: Endpoints
metadata:
  name: external-db
subsets:
- addresses:
  - ip: 192.168.1.100  # External database IP
  ports:
  - port: 5432
```

---

## Chapter 6: Ingress

### 6.1 What is Ingress?

**HTTP/HTTPS routing to services**

```
Internet
    ↓
Ingress (nginx/traefik)
    ├─ /api     → backend-service
    ├─ /auth    → auth-service
    └─ /        → frontend-service
```

### 6.2 Ingress Controller

**Install NGINX Ingress Controller:**
```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.0/deploy/static/provider/cloud/deploy.yaml
```

### 6.3 Ingress Resource

**ingress.yaml:**
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: app-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  ingressClassName: nginx
  rules:
  - host: myapp.example.com
    http:
      paths:
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: backend-service
            port:
              number: 80
      - path: /
        pathType: Prefix
        backend:
          service:
            name: frontend-service
            port:
              number: 80
  tls:
  - hosts:
    - myapp.example.com
    secretName: tls-secret
```

**TLS Certificate:**
```bash
# Create TLS secret
kubectl create secret tls tls-secret \
  --cert=path/to/cert.crt \
  --key=path/to/cert.key
```

---

# Part IV: Storage (Week 5)

## Chapter 7: Volumes

### 7.1 Volume Types

**1. emptyDir** - Temporary storage
```yaml
volumes:
- name: cache
  emptyDir: {}
```

**2. hostPath** - Node's filesystem
```yaml
volumes:
- name: logs
  hostPath:
    path: /var/log/myapp
    type: DirectoryOrCreate
```

**3. persistentVolumeClaim** - Persistent storage
```yaml
volumes:
- name: data
  persistentVolumeClaim:
    claimName: mysql-pvc
```

### 7.2 Persistent Volumes (PV) & Claims (PVC)

**Architecture:**
```
Storage Admin          Developer
     ↓                     ↓
Creates PV            Creates PVC
     ↓                     ↓
     └──── Binding ────────┘
              ↓
          Pod uses PVC
```

**PersistentVolume:**
```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: mysql-pv
spec:
  capacity:
    storage: 10Gi
  accessModes:
  - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  storageClassName: standard
  hostPath:
    path: /data/mysql
```

**PersistentVolumeClaim:**
```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: mysql-pvc
spec:
  accessModes:
  - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
  storageClassName: standard
```

**Pod using PVC:**
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: mysql
spec:
  containers:
  - name: mysql
    image: mysql:8.0
    volumeMounts:
    - name: mysql-storage
      mountPath: /var/lib/mysql
  volumes:
  - name: mysql-storage
    persistentVolumeClaim:
      claimName: mysql-pvc
```

### 7.3 StorageClasses (Dynamic Provisioning)

**AWS EBS:**
```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: fast-ssd
provisioner: kubernetes.io/aws-ebs
parameters:
  type: gp3
  iops: "3000"
  encrypted: "true"
allowVolumeExpansion: true
```

**PVC with StorageClass:**
```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: app-data
spec:
  accessModes:
  - ReadWriteOnce
  storageClassName: fast-ssd
  resources:
    requests:
      storage: 100Gi
```

---

*This is Part 1 of the comprehensive Kubernetes course. The guide continues with ConfigMaps/Secrets, Auto-Scaling, StatefulSets, Helm, Security, Monitoring, and Production Patterns in the complete file.*

**[Course continues with 7 more major sections covering advanced topics...]**
