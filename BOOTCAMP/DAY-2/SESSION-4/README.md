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

GO TO [DAY 2](../README.md)