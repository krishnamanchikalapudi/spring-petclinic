
## 10:00 – 11:30 | Session 1 — Why Local-First AI + Spring PetClinic Architecture

**Chapters Covered:** 0, 1, 2, 3

### Presentation (35 min)
- **Ch 0:** The local-first AI argument — data sovereignty, cost crossover at 1,900 req/day, vendor independence
- **Ch 1:** Why every enterprise application needs AI — from reactive to proactive systems
- **Ch 2:** PetClinic domain model — Owners, Pets, Vets, Visits, and why it's the perfect AI testbed
- **Ch 3:** Model selection framework — qwen3.5:0.8b on CPU vs GPU, quantization (Q4_K_M default), nomic-embed-text

### Live Demo (25 min)

#### DEMO 1: Model comparison — quality vs speed
- Run inference and measure time
```bash
time curl -m 5 -s -X POST http://localhost:11434/api/generate -H "Content-Type: application/json" -d '{"model":"qwen3.5:0.8b","prompt":"What vaccines does an adult cat need? Be concise.","stream":false}' | jq '.response'
```
![Ollama - generate](./SESSION-1/images/ollama-generate.png)

#### DEMO 2: Embedding generation — nomic-embed-text
```bash
curl -s -X POST http://localhost:11434/api/embed -H "Content-Type: application/json" -d '{"model":"nomic-embed-text","input":"feline upper respiratory infection treatment"}' \
  | jq '.embeddings[0][0:5]'
```
![Ollama - embed](./SESSION-1/images/ollama-embed.png)

#### DEMO 3: Model details
```bash
curl -s -X POST http://localhost:11434/api/show  -H "Content-Type: application/json" -d '{"name":"qwen3.5:0.8b"}' | jq '{model:.modelfile,size:.size}'
```
![Ollama - show ](./SESSION-1/images/ollama-show.png)

#### DEMO 4: Inspect PetClinic schema
```bash
psql -h localhost -U petclinic -d petclinic -c "\dt" -c "SELECT count(*) as owners FROM owners;"
```
![PGVector - query](./SESSION-1/images/pgvector-query.png)

### Hands-On Lab 1A — Model Benchmarking (30 min)

**Objective:** Run 5 veterinary queries and measure latency + quality

#### Lab 1A: benchmark_queries.sh
```bash
cd BOOTCAMP/DAY-1/SESSION-1

./benchmark_queries.sh
```

**Lab Deliverable:** Screenshot of 5 query results + latency table in your notes

---

GO TO [DAY 1](../README.md)