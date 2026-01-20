# Kubernetes Deployment Guide
## E-Commerce Microservices Platform

**Date:** 2026-01-20  
**Namespace:** `ecom`  
**Stack:** Loki (logging) + Zipkin (tracing) + Kafka (messaging)

---

## Prerequisites

1. **Kubernetes Cluster** (Minikube, Kind, or Cloud)
   ```powershell
   # For Minikube
   minikube start --cpus=4 --memory=8192 --driver=docker
   
   # For Kind
   kind create cluster --name ecom-cluster
   ```

2. **kubectl** CLI installed and configured
   ```powershell
   kubectl version --client
   ```

3. **Docker** installed for building images
   ```powershell
   docker --version
   ```

4. **Maven** installed for building JARs
   ```powershell
   mvn --version
   ```

---

## STEP 1: Build Docker Images

Navigate to the project root directory and build images for all services.

### Order Service
```powershell
cd order-service

# Single-stage build (faster for development)
docker build -f Dockerfile.single -t ecommerce/order-service:1.0.0 .

# Multi-stage build (optimized for production)
docker build -f Dockerfile.multi -t ecommerce/order-service:1.0.0 .

cd ..
```

### Inventory Service
```powershell
cd inventory-service

# Single-stage build
docker build -f Dockerfile.single -t ecommerce/inventory-service:1.0.0 .

# Multi-stage build (recommended)
docker build -f Dockerfile.multi -t ecommerce/inventory-service:1.0.0 .

cd ..
```

### Gateway Server
```powershell
cd gateway-server

# Single-stage build
docker build -f Dockerfile.single -t ecommerce/gateway-server:1.0.0 .

# Multi-stage build (recommended)
docker build -f Dockerfile.multi -t ecommerce/gateway-server:1.0.0 .

cd ..
```

### Identity Service
```powershell
cd identity-service

# Single-stage build
docker build -f Dockerfile.single -t ecommerce/identity-service:1.0.0 .

# Multi-stage build (recommended)
docker build -f Dockerfile.multi -t ecommerce/identity-service:1.0.0 .

cd ..
```

### Load Images into Cluster (Minikube/Kind only)

**For Minikube:**
```powershell
minikube image load ecommerce/order-service:1.0.0
minikube image load ecommerce/inventory-service:1.0.0
minikube image load ecommerce/gateway-server:1.0.0
minikube image load ecommerce/identity-service:1.0.0
```

**For Kind:**
```powershell
kind load docker-image ecommerce/order-service:1.0.0 --name ecom-cluster
kind load docker-image ecommerce/inventory-service:1.0.0 --name ecom-cluster
kind load docker-image ecommerce/gateway-server:1.0.0 --name ecom-cluster
kind load docker-image ecommerce/identity-service:1.0.0 --name ecom-cluster
```

**For Cloud Kubernetes (AKS/EKS/GKE):**
```powershell
# Tag images with registry URL
docker tag ecommerce/order-service:1.0.0 <your-registry>/order-service:1.0.0
docker tag ecommerce/inventory-service:1.0.0 <your-registry>/inventory-service:1.0.0
docker tag ecommerce/gateway-server:1.0.0 <your-registry>/gateway-server:1.0.0
docker tag ecommerce/identity-service:1.0.0 <your-registry>/identity-service:1.0.0

# Push to registry
docker push <your-registry>/order-service:1.0.0
docker push <your-registry>/inventory-service:1.0.0
docker push <your-registry>/gateway-server:1.0.0
docker push <your-registry>/identity-service:1.0.0

# Update image references in k8s deployment YAML files
```

---

## STEP 2: Deploy Kubernetes Resources

Apply manifests in the correct order to ensure dependencies are satisfied.

### 2.1 Create Namespace and Common Resources
```powershell
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/01-configmap-common.yaml
kubectl apply -f k8s/02-secrets.yaml
```

**Verification:**
```powershell
kubectl get namespace ecom
kubectl get configmap -n ecom
kubectl get secrets -n ecom
```

---

### 2.2 Deploy Infrastructure (Kafka)
```powershell
kubectl apply -f k8s/infra/kafka-deployment.yaml
kubectl apply -f k8s/infra/kafka-service.yaml
```

**Wait for Kafka to be ready:**
```powershell
kubectl wait --for=condition=ready pod -l app=kafka -n ecom --timeout=300s
kubectl get pods -n ecom -l app=kafka
```

---

### 2.3 Deploy Logging Stack (Loki + Promtail + Grafana)
```powershell
kubectl apply -f k8s/logging/loki-statefulset.yaml
kubectl apply -f k8s/logging/promtail-daemonset.yaml
kubectl apply -f k8s/logging/grafana-deployment.yaml
```

**Wait for logging stack:**
```powershell
kubectl wait --for=condition=ready pod -l app=loki -n ecom --timeout=180s
kubectl wait --for=condition=ready pod -l app=grafana -n ecom --timeout=120s
kubectl get pods -n ecom -l app=loki
kubectl get pods -n ecom -l app=promtail
kubectl get pods -n ecom -l app=grafana
```

---

### 2.4 Deploy Tracing (Zipkin)
```powershell
kubectl apply -f k8s/tracing/zipkin-deployment.yaml
```

**Wait for Zipkin:**
```powershell
kubectl wait --for=condition=ready pod -l app=zipkin -n ecom --timeout=120s
kubectl get pods -n ecom -l app=zipkin
```

---

### 2.5 Deploy Microservices

Deploy in order of dependencies: inventory → order → identity → gateway

#### Inventory Service
```powershell
kubectl apply -f k8s/inventory-service-configmap.yaml
kubectl apply -f k8s/inventory-service-deployment.yaml
```

**Wait for ready:**
```powershell
kubectl wait --for=condition=ready pod -l app=inventory-service -n ecom --timeout=180s
kubectl get pods -n ecom -l app=inventory-service
```

#### Order Service
```powershell
kubectl apply -f k8s/order-service-configmap.yaml
kubectl apply -f k8s/order-service-deployment.yaml
```

**Wait for ready:**
```powershell
kubectl wait --for=condition=ready pod -l app=order-service -n ecom --timeout=180s
kubectl get pods -n ecom -l app=order-service
```

#### Identity Service
```powershell
kubectl apply -f k8s/identity-service-configmap.yaml
kubectl apply -f k8s/identity-service-deployment.yaml
```

**Wait for ready:**
```powershell
kubectl wait --for=condition=ready pod -l app=identity-service -n ecom --timeout=180s
kubectl get pods -n ecom -l app=identity-service
```

#### Gateway Server
```powershell
kubectl apply -f k8s/gateway-configmap.yaml
kubectl apply -f k8s/gateway-deployment.yaml
```

**Wait for ready:**
```powershell
kubectl wait --for=condition=ready pod -l app=gateway-server -n ecom --timeout=180s
kubectl get pods -n ecom -l app=gateway-server
```

---

## STEP 3: Verify Deployment

### Check All Pods
```powershell
kubectl get pods -n ecom
```

**Expected output:**
```
NAME                                 READY   STATUS    RESTARTS   AGE
gateway-server-xxxxxxxxxx-xxxxx      1/1     Running   0          2m
grafana-xxxxxxxxxx-xxxxx             1/1     Running   0          5m
identity-service-xxxxxxxxxx-xxxxx    1/1     Running   0          3m
identity-service-xxxxxxxxxx-xxxxx    1/1     Running   0          3m
inventory-service-xxxxxxxxxx-xxxxx   1/1     Running   0          4m
inventory-service-xxxxxxxxxx-xxxxx   1/1     Running   0          4m
kafka-0                              2/2     Running   0          8m
loki-0                               1/1     Running   0          6m
order-service-xxxxxxxxxx-xxxxx       1/1     Running   0          3m
order-service-xxxxxxxxxx-xxxxx       1/1     Running   0          3m
promtail-xxxxx                       1/1     Running   0          5m
zipkin-xxxxxxxxxx-xxxxx              1/1     Running   0          4m
```

### Check All Services
```powershell
kubectl get svc -n ecom
```

**Expected services:**
- `gateway-service` (NodePort 30080)
- `order-service` (ClusterIP)
- `inventory-service` (ClusterIP)
- `identity-service` (ClusterIP)
- `kafka-service` (ClusterIP)
- `loki-service` (ClusterIP)
- `grafana-service` (NodePort 30300)
- `zipkin-service` (NodePort 30941)

---

## STEP 4: Access Services

### Gateway (API Entry Point)

**Minikube:**
```powershell
minikube service gateway-service -n ecom
# Or get URL:
minikube service gateway-service -n ecom --url
```

**Kind/Other:**
```powershell
kubectl port-forward svc/gateway-service -n ecom 8080:8080
# Access at: http://localhost:8080
```

**Test endpoints:**
- Swagger UI: `http://<gateway-url>/swagger-ui.html`
- Health: `http://<gateway-url>/actuator/health`
- Orders API: `http://<gateway-url>/api/v1/orders`
- Inventory API: `http://<gateway-url>/api/v1/inventory`

---

### Grafana (Logs Viewer)

**Minikube:**
```powershell
minikube service grafana-service -n ecom
```

**Kind/Other:**
```powershell
kubectl port-forward svc/grafana-service -n ecom 3000:3000
# Access at: http://localhost:3000
```

**Login:**
- Username: `admin`
- Password: `admin123`

**View logs:**
1. Navigate to "Explore"
2. Select "Loki" data source
3. Query: `{namespace="ecom", app="order-service"}`
4. View JSON logs with trace IDs

---

### Zipkin (Traces Viewer)

**Minikube:**
```powershell
minikube service zipkin-service -n ecom
```

**Kind/Other:**
```powershell
kubectl port-forward svc/zipkin-service -n ecom 9411:9411
# Access at: http://localhost:9411
```

**View traces:**
1. Open Zipkin UI
2. Select service (e.g., "order-service")
3. Click "Run Query"
4. Click on any trace to see distributed flow across services

---

## STEP 5: Debugging Commands

### View Logs
```powershell
# Application logs
kubectl logs -f deployment/order-service -n ecom
kubectl logs -f deployment/inventory-service -n ecom
kubectl logs -f deployment/gateway-server -n ecom
kubectl logs -f deployment/identity-service -n ecom

# Infrastructure logs
kubectl logs -f statefulset/kafka -n ecom -c kafka
kubectl logs -f statefulset/loki -n ecom
kubectl logs -f deployment/zipkin -n ecom
```

### Describe Resources
```powershell
kubectl describe pod <pod-name> -n ecom
kubectl describe deployment <deployment-name> -n ecom
kubectl describe service <service-name> -n ecom
```

### Check Events
```powershell
kubectl get events -n ecom --sort-by='.lastTimestamp'
```

### Restart Deployment
```powershell
kubectl rollout restart deployment/order-service -n ecom
kubectl rollout status deployment/order-service -n ecom
```

### Check Resource Usage
```powershell
kubectl top pods -n ecom
kubectl top nodes
```

---

## STEP 6: Cleanup

### Delete All Resources
```powershell
# Delete all resources in namespace
kubectl delete namespace ecom

# Or delete selectively
kubectl delete -f k8s/order-service-deployment.yaml
kubectl delete -f k8s/inventory-service-deployment.yaml
kubectl delete -f k8s/gateway-deployment.yaml
kubectl delete -f k8s/identity-service-deployment.yaml
kubectl delete -f k8s/tracing/
kubectl delete -f k8s/logging/
kubectl delete -f k8s/infra/
kubectl delete -f k8s/02-secrets.yaml
kubectl delete -f k8s/01-configmap-common.yaml
kubectl delete -f k8s/00-namespace.yaml
```

### Stop Cluster (Minikube)
```powershell
minikube stop
minikube delete
```

### Delete Cluster (Kind)
```powershell
kind delete cluster --name ecom-cluster
```

---

## Troubleshooting

### Pods Not Starting

**Check pod status:**
```powershell
kubectl get pods -n ecom
kubectl describe pod <pod-name> -n ecom
```

**Common issues:**
- Image pull errors: Ensure images are loaded into cluster or accessible from registry
- Resource limits: Check if cluster has sufficient CPU/memory
- ConfigMap/Secret missing: Verify all configs are applied

### Service Not Reachable

**Check service endpoints:**
```powershell
kubectl get endpoints -n ecom
```

**Check network connectivity:**
```powershell
# Test from another pod
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -n ecom -- sh
curl http://order-service:8082/actuator/health
```

### Health Checks Failing

**Check actuator endpoints:**
```powershell
kubectl port-forward pod/<pod-name> -n ecom 8082:8082
curl http://localhost:8082/actuator/health/liveness
curl http://localhost:8082/actuator/health/readiness
```

**Common causes:**
- Application not fully started (increase initialDelaySeconds)
- Database connection issues
- Kafka connection timeout

### Kafka Not Ready

**Check Zookeeper:**
```powershell
kubectl logs statefulset/kafka -n ecom -c zookeeper
```

**Check Kafka broker:**
```powershell
kubectl logs statefulset/kafka -n ecom -c kafka
```

**Test Kafka connectivity:**
```powershell
kubectl exec -it kafka-0 -n ecom -c kafka -- kafka-topics --bootstrap-server localhost:9092 --list
```

---

## Monitoring & Observability

### Health Checks
- Gateway: `http://<gateway-url>/actuator/health`
- Order: `http://<order-url>:8082/actuator/health`
- Inventory: `http://<inventory-url>:8081/actuator/health`
- Identity: `http://<identity-url>:8083/actuator/health`

### Metrics (Prometheus-ready)
- `http://<service-url>/actuator/prometheus`

### Logs (Loki/Grafana)
- Access Grafana at NodePort 30300
- Query: `{namespace="ecom"}` for all logs
- Filter by app: `{namespace="ecom", app="order-service"}`
- Filter by level: `{namespace="ecom"} |= "ERROR"`
- Trace correlation: `{namespace="ecom"} | json | traceId="<trace-id>"`

### Traces (Zipkin)
- Access Zipkin at NodePort 30941
- View end-to-end request flow
- Identify slow services and bottlenecks
- Correlate with logs using trace IDs

---

## Notes

1. **H2 Database:** In-memory database is used for demo. Data is lost on pod restart.
2. **Kafka Storage:** Uses persistent volumes. Data survives pod restarts.
3. **Loki Storage:** Uses persistent volumes (5Gi). Adjust retention in config if needed.
4. **Zipkin Storage:** In-memory. Traces lost on restart. For production, use persistent backend.
5. **Secrets:** Update `k8s/02-secrets.yaml` with real values before production use.
6. **Keycloak:** Not included in manifests. Deploy separately or use external Keycloak instance.
7. **Scaling:** Adjust `replicas` in deployment YAML files as needed.
8. **Resource Limits:** Adjust `requests` and `limits` based on cluster capacity.

---

## Production Considerations

1. **Persistent Storage:** Replace H2 with PostgreSQL/MySQL
2. **Secret Management:** Use Sealed Secrets, External Secrets Operator, or Vault
3. **Ingress Controller:** Deploy Nginx/Traefik ingress for external access
4. **TLS Certificates:** Configure cert-manager for HTTPS
5. **Horizontal Pod Autoscaling:** Add HPA based on CPU/memory/custom metrics
6. **Service Mesh:** Consider Istio/Linkerd for advanced traffic management
7. **Monitoring Stack:** Add Prometheus + Alertmanager for metrics and alerting
8. **Backup Strategy:** Implement backup for Kafka topics and Loki logs
9. **Network Policies:** Restrict pod-to-pod communication for security
10. **Multi-environment:** Use Kustomize or Helm for dev/staging/prod overlays

---

**End of Deployment Guide**
