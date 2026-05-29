## 12:30 – 14:00 | Session 5 — Scheduling Agent + Multi-Agent Collaboration

**Chapters Covered:** 9, 10

### Presentation (30 min)
- **Ch 9:** SchedulingAgent internals — ConfidenceScorer, BusinessRuleEngine, IdempotencyManager
- Code anchor: `VisitRepository.save()` — all booking writes converge here
- **Ch 10:** Multi-agent coordinator — 4 specialists (Diagnosis, Scheduling, Billing, Notification)
- CoordinatorState (PostgreSQL), AgentMemoryBus (Redis stream)

### Live Demo (30 min)
```bash
# DEMO 15: Single agent — full booking flow
curl -X POST http://localhost:8080/api/v1/ai/agents/scheduling/execute \
  -H "Content-Type: application/json" \
  -d '{
    "goal": "Book Luna Chen for annual wellness with Dr. Smith on Friday at 10am",
    "sessionId": "booking-demo"
  }'

# Watch the agent steps in logs
tail -f logs/petclinic-ai.log | grep -E "THOUGHT|TOOL|OBSERVE|FINAL"

# DEMO 16: Multi-agent post-visit workflow
# Trigger a completed visit
curl -X POST http://localhost:8080/api/v1/visits/42/complete \
  -H "Content-Type: application/json" \
  -d '{"vetId":1,"diagnosis":"annual wellness — healthy","recommendations":"vaccines due in 6 months"}'

# Watch all 4 agents run: Diagnosis -> Scheduling -> Billing -> Notification
tail -f logs/petclinic-ai.log | grep -E "Agent.*started|Agent.*completed|CoordinatorState"

# DEMO 17: Check coordination state in PostgreSQL
psql -h localhost -U petclinic -d petclinic \
  -c "SELECT workflow_id, phase, status, created_at FROM coordinator_state ORDER BY created_at DESC LIMIT 5;"
```

### Hands-On Lab 2B — Trigger and Observe Multi-Agent Workflow (35 min)

```bash
# Step 1: Complete 3 different visits (beginner/intermediate/complex)
for visit_id in 101 102 103; do
  curl -X POST http://localhost:8080/api/v1/visits/${visit_id}/complete \
    -H "Content-Type: application/json" \
    -d '{"vetId":1,"diagnosis":"see record","recommendations":"follow up in 2 weeks"}'
  sleep 5
done

# Step 2: Monitor each workflow phase
watch -n 2 'psql -h localhost -U petclinic -d petclinic \
  -c "SELECT workflow_id, phase, status FROM coordinator_state ORDER BY created_at DESC LIMIT 10"'

# Step 3: Check what each specialist produced
curl http://localhost:8080/api/v1/workflows/latest/summary
```

---

GO TO [DAY 2](../README.md)