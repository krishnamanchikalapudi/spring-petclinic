
## MCP Server: Expose PetClinic Tools 
Decouple LLM from DB by exposing listOwners, getOwner, addVisit as safe JSON-RPC tools.

### Architecture


### Feature code
 - New files:
     - src/main/java/org/springframework/samples/petclinic/ai/
     - src/main/resources/application-ollama.properties
 - Updated files:
    - src/main/resources/templates/fragments/layout.html
```
spring-petclinic/
└── pom.xml
└── src/
    ├── main/java/org/springframework/samples/petclinic/ai/system/
```

[![Video demo](https://img.youtube.com/vi/?/0.jpg)](https://youtu.be/?)
---
---
 
## Toolformer: LLM Calls MCP Tools 
Let users say “Book Leo Tuesday” and watch AI call your API with full audit trail.

### Architecture


### Feature code
 - New files:
     - src/main/java/org/springframework/samples/petclinic/ai/
     - src/main/resources/application-ollama.properties
 - Updated files:
    - src/main/resources/templates/fragments/layout.html
```
spring-petclinic/
└── pom.xml
└── src/
    ├── main/java/org/springframework/samples/petclinic/ai/system/
```

[![Video demo](https://img.youtube.com/vi/?/0.jpg)](https://youtu.be/?)
---
---
 
## ReAct Agent with LangGraph4j 
Upgrade to multi-step THINK → ACT → OBSERVE reasoning with persistent traces for every decision.

### Architecture


### Feature code
 - New files:
     - src/main/java/org/springframework/samples/petclinic/ai/
     - src/main/resources/application-ollama.properties
 - Updated files:
    - src/main/resources/templates/fragments/layout.html
```
spring-petclinic/
└── pom.xml
└── src/
    ├── main/java/org/springframework/samples/petclinic/ai/system/
```

[![Video demo](https://img.youtube.com/vi/?/0.jpg)](https://youtu.be/?)
---
---
 
## HITL: Human Approval for Prescriptions 
Pause dangerous actions, email the vet, and resume only after human click for zero compliance risk.


[![Video demo](https://img.youtube.com/vi/?/0.jpg)](https://youtu.be/?)
---
---

## Temporal Workflow: Pet Onboarding 
Survive pod crashes with durable 5-step sagas that auto-retry and never lose a new customer.

[![Video demo](https://img.youtube.com/vi/?/0.jpg)](https://youtu.be/?)
---
---

## Memory: STM Redis + LTM H2 
Give AI short-term chat context and long-term memory so “Leo likes mornings” works next week.

[![Video demo](https://img.youtube.com/vi/?/0.jpg)](https://youtu.be/?)
---
---

## Multi-Agent: Triage Router 
Cut GPU costs 60% by routing 70% of queries to qwen3:0.5b and escalating only medical to 5b.

[![Video demo](https://img.youtube.com/vi/?/0.jpg)](https://youtu.be/?)
---
---

## Agent Eval Harness + Nightly Gate 
Sleep easy with 50 e2e tests that page you in 5 min if agent success drops below 90%.

[![Video demo](https://img.youtube.com/vi/?/0.jpg)](https://youtu.be/?)
---
---

## Guardrails: OPA + Prompt + Tool Deny 
Enforce “never prescribe, never delete” at 3 layers: prompt, MCP, and Kubernetes admission.

[![Video demo](https://img.youtube.com/vi/?/0.jpg)](https://youtu.be/?)
---
---

## HITL in Workflow: Vet Approval 
Embed human checkpoints inside 6-step DAGs so AI drafts, vet approves, system executes.

[![Video demo](https://img.youtube.com/vi/?/0.jpg)](https://youtu.be/?)
---
---

## Event-Driven Saga: Kafka + DLQ + Compensation 
Chain 10 microservices with replay and auto-rollback so email failures don’t double-charge.

[![Video demo](https://img.youtube.com/vi/?/0.jpg)](https://youtu.be/?)
---
---

## Observability: SLO + Jaeger + Loki + Trivy 
Prove 99.95% SLO, trace every AI call end-to-end, and ship with 0 CRITICAL vulns.

[![Video demo](https://img.youtube.com/vi/?/0.jpg)](https://youtu.be/?)
---
---

<!--
Read spring-petclinic https://github.com/krishnamanchikalapudi/spring-petclinic and share  step-by-step code enhancement java, html, service, h2database enhancement to build RAG using Embeddings, H2 Vector Table, Semantic Search, Citations on using locally deployed Ollama model qwen3.5 for AI enablement. The topic outcome for director swe, enterprise & principal architect, devops & SRE with security mindset metrics along with Strategy, Enterprise, & Governance. Share story narration for video content must be between 10 & 15 minutes.
-->