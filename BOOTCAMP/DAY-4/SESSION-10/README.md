## 10:00 – 11:30 | Session 10 — Containerization + Kubernetes

**Chapters Covered:** 24, 25

### Presentation (35 min)
- **Ch 24:** Multi-stage Dockerfile — model warm-up init container, sidecar pattern, docker-compose stack
- **Ch 25:** Kubernetes deployment — Helm chart structure, Ollama Deployment (Recreate strategy, startup probe), HPA, VPA

### Live Demo (25 min)
```bash
# DEMO 33: Build and run the full Docker stack
docker build -t petclinic-ai:bootcamp .

docker-compose -f docker-compose-full.yml up -d

# Watch model warm-up init container
docker logs petclinic-ai-ollama-1 -f | head -20

# Verify all services
docker-compose -f docker-compose-full.yml ps

# DEMO 34: Deploy to Minikube
minikube start --memory=8192 --cpus=4
eval $(minikube docker-env)

helm install petclinic ./helm/petclinic-ai \
  --namespace petclinic \
  --create-namespace \
  -f helm/petclinic-ai/environments/local/values.yaml

kubectl get pods -n petclinic -w

# DEMO 35: HPA in action — generate load and watch scale-out
kubectl run -it --rm load-test --image=busybox \
  --restart=Never -- sh -c \
  "for i in \$(seq 1 100); do wget -qO- http://petclinic-ai:8080/api/v1/ai/chat --post-data '{\"message\":\"test\",\"sessionId\":\"load-\$i\"}'; done"

kubectl get hpa -n petclinic -w
```

### Hands-On Lab 4A — Helm Values Customization (30 min)

```yaml
# LAB 4A: Create environments/bootcamp/values.yaml
# Override these values for a resource-constrained single-node deployment:

ollama:
  image:
    tag: latest
  resources:
    requests:
      memory: "2Gi"    # Reduced from 4Gi
      cpu: "1"
    limits:
      memory: "4Gi"
      cpu: "2"
  model:
    chat: qwen3.5:0.8b
    embed: nomic-embed-text

app:
  replicaCount: 1      # Single replica for bootcamp
  resources:
    requests:
      memory: "512Mi"
      cpu: "250m"
    limits:
      memory: "1Gi"
      cpu: "1"

# Deploy with your custom values:
# helm upgrade --install petclinic ./helm/petclinic-ai \
#   -f helm/petclinic-ai/environments/bootcamp/values.yaml \
#   --namespace petclinic
```

---

GO TO [DAY 4](../README.md)