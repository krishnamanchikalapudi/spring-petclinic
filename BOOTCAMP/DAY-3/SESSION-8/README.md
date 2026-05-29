## 12:30 – 14:00 | Session 8 — AI Observability

**Chapters Covered:** 20

### Presentation (30 min)
- **Ch 20:** 5 observability pillars — metrics, tracing, logging, drift, cost
- 25+ Prometheus metrics — inference latency, hallucination rate, agent outcomes, token budget
- Langfuse integration — distributed tracing for agent reasoning steps
- AIDriftDetector — cosine similarity drift on rolling 1000-sample window
- CostTelemetryService — cloud cost equivalent tracking

### Live Demo (30 min)
```bash
# DEMO 25: Prometheus metrics live
curl -s http://localhost:8080/actuator/prometheus | grep "^ai_" | head -30

# Key metrics to inspect:
curl -s http://localhost:8080/actuator/prometheus | grep -E \
  "ai_inference_latency|ai_hallucination_rate|ai_workflow_outcome|ai_token_budget|ai_cost"

# DEMO 26: Grafana dashboard walkthrough
# Open http://localhost:3000 (admin/petclinic)
# Navigate to: AI Operations → PetClinic AI Dashboard
# Live demo: Row 1 (Inference), Row 2 (Quality), Row 3 (Agent), Row 4 (Cost), Row 5 (Drift)

# DEMO 27: Langfuse trace inspection
# Open http://localhost:3100 (admin/petclinic)
# Run a GraphRAG query and watch the trace appear:
curl -X POST http://localhost:8080/api/v1/ai/graphrag/query \
  -H "Content-Type: application/json" \
  -d '{"query":"What vaccines does Luna need this year?","petId":42,"clinicId":1}'
# Refresh Langfuse — see the full trace: RAG → KG → Fusion → Inference

# DEMO 28: Drift detection
curl http://localhost:8080/api/v1/ai/drift/report | jq '.'
```

### Hands-On Lab 3B — Build Your Own Grafana Panel (35 min)

```bash
# Step 1: Generate load to populate metrics
for i in $(seq 1 20); do
  curl -s -X POST http://localhost:8080/api/v1/ai/chat \
    -H "Content-Type: application/json" \
    -d "{\"message\":\"Query $i: what vaccines does my pet need?\",\"sessionId\":\"load-$i\"}" &
done
wait

# Step 2: Open Grafana → Dashboards → New Panel
# Add this PromQL query for P95 inference latency:
# histogram_quantile(0.95, rate(ai_inference_latency_seconds_bucket[5m]))

# Step 3: Add a second panel for cost per request:
# ai_finops_cost_per_request_after / ai_finops_cost_per_request_before

# Step 4: Export the dashboard JSON to your lab notes
```

---

GO TO [DAY 3](../README.md)