
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

GO TO [DAY 4](../README.md)