# DAY 2: Agents — ReAct, Memory, Multi-Agent, Workflows, MCP

**Theme:** "From reactive chat to proactive autonomous agents"

---

## 09:00 – 10:00 | Day 2 Kickoff

- Day 1 recap (15 min)
- Q&A on RAG / Spring AI setup issues
- Day 2 objectives and architecture preview: agent stack

---

## 10:00 – 11:30 | Session 4 — AI Agents + Memory Architecture

**Chapters Covered:** 7, 8

### Presentation (35 min)
- **Ch 7:** Agent patterns — ReAct (Reason-Act-Observe), Plan-and-Execute, Reflection, Human-in-the-Loop
- SafetyBoundaryEnforcer — forbidden actions, confidence threshold, graceful escalation
- **Ch 8:** 4-tier memory — session (Redis), summary (ConversationSummarizer), vector recall, leadership principles
- ConversationSummarizer — triggers at 80% token budget, produces compressed summary

### Live Demo (25 min)
```bash
# DEMO 11: ReAct agent execution — watch the THOUGHT/TOOL/OBSERVE cycle
curl -X POST http://localhost:8080/api/v1/ai/agents/general/execute \
  -H "Content-Type: application/json" \
  -d '{"goal":"Find out when Luna Chen last visited and what was the reason","sessionId":"agent-demo-1"}'

# DEMO 12: Agent with tool failure — circuit breaker
# Shut down a tool temporarily and watch graceful degradation
curl -X POST http://localhost:8080/api/v1/debug/tools/lookup_pet/disable
curl -X POST http://localhost:8080/api/v1/ai/agents/general/execute \
  -H "Content-Type: application/json" \
  -d '{"goal":"Look up Biscuit Rodriguez","sessionId":"agent-demo-2"}'
curl -X POST http://localhost:8080/api/v1/debug/tools/lookup_pet/enable

# DEMO 13: Multi-turn memory — agent remembers across turns
SESSION="memory-demo-$(date +%s)"
for msg in "My cat is Luna, she is 3 years old" \
           "She had dental cleaning last month" \
           "What should I monitor after her dental procedure?"; do
  curl -s -X POST http://localhost:8080/api/v1/ai/chat \
    -H "Content-Type: application/json" \
    -d "{\"message\":\"$msg\",\"sessionId\":\"$SESSION\"}" | jq -r '.message'
  echo "---"
done

# DEMO 14: Memory inspection
redis-cli LRANGE "session:$SESSION:history" 0 -1 | head -20
```

### Hands-On Lab 2A — Build the ReAct Agent (30 min)

**Objective:** Wire the SchedulingAgent using the ReAct framework

```java
// LAB 2A: Complete SchedulingAgentExecutor.java
// The scaffold gives you the tool schemas; you wire the reasoning loop

public class SchedulingAgentExecutor {

    // TODO 1: Build the agent system prompt using ReActPrompts.SYSTEM
    // TODO 2: Implement executeReActStep() — parse THOUGHT/TOOL/ARGS from LLM output
    // TODO 3: Dispatch to the correct tool based on TOOL name
    // TODO 4: Handle FINAL response — return AgentResult.success(response)
    // TODO 5: Implement max iteration guard — return AgentResult.escalated after 5 loops

}

// Test:
// curl -X POST http://localhost:8080/api/v1/ai/agents/scheduling/execute \
//   -d '{"goal":"Book Luna Chen for a wellness exam with Dr. Smith next Monday","sessionId":"lab-2a"}'
```

---

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

## 14:15 – 16:00 | Session 6 — Workflows, Event-Driven AI, MCP

**Chapters Covered:** 11, 12, 13, 14

### Presentation (40 min)
- **Ch 11:** Spring State Machine vs Temporal — short workflows vs multi-day human gates
- **Ch 12:** Human approval workflows — RiskScorer, 4×4 decision matrix, 7-state ApprovalRequest
- **Ch 13:** Event-driven AI — Kafka EmergencyTriageConsumer, ReactiveTriagePipeline
- **Ch 14:** MCP protocol — JSON-RPC 2.0, tool schema design, why MCP changes AI composability

### Live Demo (30 min)
```bash
# DEMO 18: Human approval gate — prescription triggers approval
curl -X POST http://localhost:8080/api/v1/ai/agents/diagnosis/execute \
  -H "Content-Type: application/json" \
  -d '{"goal":"Create prescription for Luna for antibiotics","sessionId":"approval-demo"}'

# Watch approval request created
curl http://localhost:8080/api/v1/governance/approvals/pending | jq '.'

# Approve the request
APPROVAL_ID=$(curl -s http://localhost:8080/api/v1/governance/approvals/pending | jq -r '.[0].id')
curl -X POST http://localhost:8080/api/v1/governance/approvals/$APPROVAL_ID/approve \
  -d '{"reviewerId":1,"notes":"Approved — standard dosage"}'

# DEMO 19: Emergency triage via Kafka
kafka-console-producer --topic pet-emergencies \
  --bootstrap-server localhost:9092 << 'EOF'
{"petId":42,"ownerId":7,"symptoms":"collapsed, pale gums, labored breathing","severity":"CRITICAL"}
EOF

# Watch the emergency triage pipeline process in real time
tail -f logs/petclinic-ai.log | grep -E "Emergency|Triage|CRITICAL"

# DEMO 20: MCP tool discovery
curl http://localhost:8080/mcp/v1 \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":"1","method":"tools/list"}'
```

### Hands-On Lab 2C — MCP Tool Call from Agent (35 min)

```bash
# Step 1: Obtain an OAuth2 token
TOKEN=$(curl -s -X POST http://localhost:9000/oauth2/token \
  -u "petclinic-scheduling-agent:agent-secret" \
  -d "grant_type=client_credentials&scope=petclinic:identity:read+petclinic:schedule:read" \
  | jq -r '.access_token')
echo "Token: ${TOKEN:0:40}..."

# Step 2: Call lookup_pet via MCP
curl -X POST http://localhost:8080/mcp/v1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "jsonrpc":"2.0","id":"2","method":"tools/call",
    "params":{
      "name":"lookup_pet",
      "arguments":{"ownerLastName":"Chen","petName":"Luna"}
    }
  }' | jq '.result.content[0].text | fromjson'

# Step 3: Try a scope-restricted call (should FAIL)
curl -X POST http://localhost:8080/mcp/v1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "jsonrpc":"2.0","id":"3","method":"tools/call",
    "params":{
      "name":"book_appointment",
      "arguments":{"petId":42,"vetLastName":"Smith","date":"2025-06-01","timeSlot":"10:00","visitReason":"test"}
    }
  }' | jq '.error'
# Expected: INSUFFICIENT_PERMISSIONS (no schedule:write scope)
```

---

## 16:00 – 17:00 | Day 2 Wrap

- Agents + MCP Q&A
- Recap: what we built — from chat to ReAct agents to multi-agent with MCP
- Preview of Day 3: Knowledge Graph + GraphRAG + Observability + Security

---

---
