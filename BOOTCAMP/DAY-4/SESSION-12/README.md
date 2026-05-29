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

GO TO [DAY 4](../README.md)