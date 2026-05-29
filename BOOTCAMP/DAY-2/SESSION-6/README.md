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

GO TO [DAY 2](../README.md)