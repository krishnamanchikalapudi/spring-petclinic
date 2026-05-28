# DAY 4: Platform — Containers, Kubernetes, GitOps, FinOps, SRE, Team Model

**Theme:** "From working code to production-grade platform"

---

## 09:00 – 10:00 | Day 4 Kickoff

- Day 3 recap
- "Day in the life of an AI Platform Engineer" walkthrough
- Day 4 objectives: deploy, operate, and sustain the system

---

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

## 12:30 – 14:00 | Session 11 — GitOps + FinOps

**Chapters Covered:** 26, 27

### Presentation (30 min)
- **Ch 26:** ArgoCD + Tekton pipeline — 4 stages, canary deployment, AnalysisTemplate quality gates, automatic rollback operator
- **Ch 27:** AI FinOps — cost crossover at 1,900 req/day, SemanticResponseCache, TokenBudgetEnforcer, 4 FinOps metrics

### Live Demo (35 min)
```bash
# DEMO 36: ArgoCD application sync
argocd app create petclinic-ai \
  --repo https://github.com/your-org/petclinic-ai-gitops \
  --path helm/petclinic-ai \
  --dest-namespace petclinic \
  --sync-policy automated

argocd app get petclinic-ai
argocd app sync petclinic-ai

# DEMO 37: Trigger the full Tekton pipeline
cat << 'EOF' | kubectl apply -f -
apiVersion: tekton.dev/v1beta1
kind: PipelineRun
metadata:
  name: petclinic-ai-bootcamp-run
  namespace: tekton-pipelines
spec:
  pipelineRef:
    name: petclinic-ai-delivery
  params:
    - name: git-revision
      value: "main"
    - name: image-tag
      value: "bootcamp-$(date +%s)"
    - name: chat-model-version
      value: "qwen3.5:0.8b-instruct-q4_K_M"
    - name: prompt-version
      value: "v1.4"
EOF

# Watch the pipeline
kubectl get pipelinerun petclinic-ai-bootcamp-run -n tekton-pipelines -w

# DEMO 38: FinOps dashboard
curl http://localhost:8080/api/v1/finops/executive-summary | jq '.'
# Shows: actual monthly $85 vs cloud equivalent, cache hit ratio, savings

# DEMO 39: Cache hit demo
# First call (MISS)
time curl -s -X POST http://localhost:8080/api/v1/ai/chat \
  -d '{"message":"What vaccines does an adult dog need?","sessionId":"cache-test-1"}'

# Second call (HIT — semantic similarity)
time curl -s -X POST http://localhost:8080/api/v1/ai/chat \
  -d '{"message":"What vaccinations does a grown dog require?","sessionId":"cache-test-2"}'

# Check cache metrics
curl -s http://localhost:8080/actuator/prometheus | grep "ai_cache"
```

### Hands-On Lab 4B — Trigger Automatic Rollback (35 min)

```bash
# LAB 4B: Simulate a hallucination rate spike and watch rollback

# Step 1: Inject bad prompt version (artificially degrades quality)
kubectl set env deployment/petclinic-ai \
  -n petclinic \
  PETCLINIC_AI_PROMPT_VET_ASSISTANT_VERSION=v0.1-bad-test

# Step 2: Generate traffic to drive hallucination rate up
for i in $(seq 1 30); do
  curl -s -X POST http://localhost:8080/api/v1/ai/chat \
    -d "{\"message\":\"Is aspirin safe for cats in large doses?\",\"sessionId\":\"hall-test-$i\"}" &
done
wait

# Step 3: Watch hallucination rate climb in Prometheus
watch -n 5 'curl -s http://localhost:8080/actuator/prometheus | \
  grep ai_hallucination_rate_total | head -5'

# Step 4: Watch AIRollbackOperator trigger (after ~10 min if rate > 5%)
# For demo: manually trigger
curl -X POST http://localhost:8080/api/v1/debug/rollback/simulate \
  -d '{"hallucinationRate":0.06}'

# Step 5: Confirm rollback
kubectl logs -n petclinic -l app=petclinic-ai | grep "TRIGGERING AUTOMATIC AI ROLLBACK"
```

---

## 14:15 – 16:00 | Session 12 — AI SRE + Team Model + Capstone

**Chapters Covered:** 28, 29, 30

### Presentation (35 min)
- **Ch 28:** AI SRE — 5 SLOs, error budget policy, 5 incident runbooks, MTTR targets
- **Ch 29:** From AI applications to autonomous platforms — self-healing, MCP ecosystem, local-first thesis
- **Ch 30:** AI Team Operating Model — 5 roles, ritual calendar, model promotion gate, 4-phase roadmap

### Live Demo (25 min)
```bash
# DEMO 40: SLO compliance dashboard
# Open Grafana → Row 6: AI SRE — SLO Compliance
# Live values: Availability 99.94%, Agent Completion 87.3%, Hallucination 0.9%

# DEMO 41: Error budget consumption
curl http://localhost:8080/api/v1/sre/error-budget | jq '.'

# DEMO 42: Simulate P1 incident — Ollama down runbook
kubectl scale deployment petclinic-ollama -n petclinic --replicas=0

# Watch the health check fail
watch -n 3 'curl -s http://localhost:8080/actuator/health | jq ".components.ollama"'

# Follow Runbook RB-01
kubectl get pods -n petclinic -l app=ollama
kubectl scale deployment petclinic-ollama -n petclinic --replicas=1
kubectl wait --for=condition=available deployment/petclinic-ollama -n petclinic --timeout=120s

# Verify recovery
curl http://localhost:8080/actuator/health | jq '.components.ollama.status'

# DEMO 43: Capacity planning report
curl http://localhost:8080/api/v1/sre/capacity | jq '.scalingRecommendation'
```

### Hands-On Lab 4C — Capstone: Full System Walk-Through (45 min)

**Objective:** Each participant executes the complete PetClinic AI flow end-to-end

```bash
# CAPSTONE LAB — complete this sequence independently

# Step 1: Owner inquiry via chat
SESSION="capstone-$(whoami)-$(date +%s)"
curl -X POST http://localhost:8080/api/v1/ai/chat \
  -d "{\"message\":\"My dog Max has been scratching constantly. What could it be?\",\"sessionId\":\"$SESSION\"}"

# Step 2: Grounded recommendation via GraphRAG
curl -X POST http://localhost:8080/api/v1/ai/graphrag/query \
  -d "{\"query\":\"What are the common causes of pruritus in dogs?\",\"clinicId\":1}"

# Step 3: Book an appointment via agent
curl -X POST http://localhost:8080/api/v1/ai/agents/scheduling/execute \
  -d "{\"goal\":\"Book Max for a dermatology consult with Dr. Taylor next week\",\"sessionId\":\"$SESSION\"}"

# Step 4: Complete the visit and trigger multi-agent workflow
# (use the confirmation ID from step 3)
CONFIRMATION_ID="CONF-XXX" # replace with actual
curl -X POST http://localhost:8080/api/v1/visits/by-confirmation/$CONFIRMATION_ID/complete \
  -d '{"vetId":2,"diagnosis":"atopic dermatitis","recommendations":"antihistamines + diet change"}'

# Step 5: Check governance compliance log
curl http://localhost:8080/api/v1/governance/compliance/log?session=$SESSION | jq '.'

# Step 6: Check FinOps cost for this session
curl http://localhost:8080/api/v1/finops/session/$SESSION | jq '.'

# Step 7: Verify all SLOs still green
curl http://localhost:8080/api/v1/sre/slo-summary | jq '.allGreen'
```

---

## 16:00 – 17:00 | Day 4 Wrap + Bootcamp Close

### Agenda (60 min)
- Capstone results sharing — each participant presents their flow output (5 min each)
- Open Q&A — architecture, production concerns, scaling
- **Graduation checklist** — "Can you do these 10 things?"
- Next steps: reading list, community, further study

### Graduation Checklist

```
□ Can you start the full PetClinic AI stack from scratch (Docker + Ollama + Spring Boot)?
□ Can you add a new prompt version and run it through the quality gate?
□ Can you write a new golden-set entry and verify it catches a hallucination?
□ Can you trace an agent execution through Langfuse and identify a tool call?
□ Can you write a Cypher query to find drug interactions for a specific pet?
□ Can you deploy a new version via GitOps and monitor the canary in ArgoCD?
□ Can you read the FinOps dashboard and explain what drives cost per request?
□ Can you trigger a P1 incident runbook from memory (Ollama down)?
□ Can you explain the 5 AI SLOs and what each one measures?
□ Can you describe the 4-phase roadmap for AI capability expansion?
```

---

---