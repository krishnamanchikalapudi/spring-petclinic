## 12:30 – 14:00 | Session 2 — Spring AI Integration + First Chat Assistant

**Chapters Covered:** 4, 5

### Presentation (30 min)
- **Ch 4:** ADR-001: Spring AI vs LangChain4j vs Direct REST — why Spring AI wins on MCP support
- **Ch 5:** Building VetAssistantService — AICompletionPort abstraction, stateless vs stateful chat, SSE streaming

### Live Demo (25 min)
```bash
# DEMO 5: First AI chat endpoint — live coding from scratch
# Open VetAssistantController.java and walk through:

# Test stateless chat
curl -X POST http://localhost:8080/api/v1/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What vaccines does Luna my 3-year-old cat need?"}'

# Test stateful chat (session continuity)
SESSION_ID="demo-$(date +%s)"
curl -X POST http://localhost:8080/api/v1/ai/chat \
  -H "Content-Type: application/json" \
  -d "{\"message\": \"My cat is Luna, she is 3 years old.\", \"sessionId\": \"$SESSION_ID\"}"

curl -X POST http://localhost:8080/api/v1/ai/chat \
  -H "Content-Type: application/json" \
  -d "{\"message\": \"What vaccines does she need?\", \"sessionId\": \"$SESSION_ID\"}"

# DEMO 6: SSE streaming
curl -N -X POST http://localhost:8080/api/v1/ai/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "Explain feline diabetes in 3 sentences.", "sessionId": "stream-demo"}'
```

### Hands-On Lab 1B — Build the Vet Chat Assistant (35 min)

**Objective:** Wire up VetAssistantService from scratch using the AICompletionPort abstraction

```java
// LAB 1B: Complete this implementation
// File: src/main/java/petclinic/ai/VetAssistantService.java

@Service
public class VetAssistantService {

    private final AICompletionPort completionPort;
    private final ConversationMemoryService memoryService;

    // TODO 1: Inject dependencies via constructor

    public ChatResponse chat(String sessionId, String userMessage) {
        // TODO 2: Load conversation history from memoryService
        // TODO 3: Build system prompt using VetAssistantPrompts.SYSTEM_PROMPT_V1_4
        // TODO 4: Call completionPort.chat(systemPrompt, history, userMessage)
        // TODO 5: Save updated history to memoryService
        // TODO 6: Return ChatResponse with message and sessionId
        return null; // replace
    }
}

// Test your implementation:
// curl -X POST http://localhost:8080/api/v1/ai/chat \
//   -H "Content-Type: application/json" \
//   -d '{"message":"Is chocolate toxic to dogs?","sessionId":"lab-1b"}'
```

---

GO TO [DAY 1](../README.md)