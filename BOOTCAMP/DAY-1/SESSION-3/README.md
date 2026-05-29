## 14:15 – 16:00 | Session 3 — RAG Pipeline

**Chapters Covered:** 6

### Presentation (35 min)
- **Ch 6:** RAG architecture — why LLMs hallucinate without grounding
- Chunking strategy for veterinary documents — 512 token chunks, 64 token overlap
- pgvector hybrid search: embedding similarity + BM25 keyword fusion (RRF)
- Multi-tenant SearchFilter — clinicId isolation
- Hallucination reduction measurement before/after RAG

### Live Demo (30 min)
```bash
# DEMO 7: Index clinical documents
curl -X POST http://localhost:8080/api/v1/rag/ingest \
  -H "Content-Type: application/json" \
  -d '{"documentPath": "classpath:clinical-protocols/vaccination-schedule.pdf","clinicId": 1}'

# DEMO 8: Vector search — semantic
curl -X POST http://localhost:8080/api/v1/rag/search \
  -H "Content-Type: application/json" \
  -d '{"query": "feline upper respiratory treatment protocol","clinicId": 1,"topK": 3}'

# DEMO 9: Grounded response vs. non-grounded
echo "=== WITHOUT RAG (hallucination risk) ==="
curl -s -X POST http://localhost:11434/api/generate \
  -d '{"model":"qwen3.5:0.8b","prompt":"What is our clinic protocol for treating feline URIs?","stream":false}' | jq -r '.response'

echo "=== WITH RAG (grounded) ==="
curl -X POST http://localhost:8080/api/v1/ai/chat/grounded \
  -H "Content-Type: application/json" \
  -d '{"message":"What is our clinic protocol for treating feline URIs?","sessionId":"rag-demo","clinicId":1}'

# DEMO 10: Check pgvector data
psql -h localhost -U petclinic -d petclinic \
  -c "SELECT id, content[1:80] as snippet, clinic_id FROM vector_store LIMIT 5;"
```

### Hands-On Lab 1C — RAG Pipeline End-to-End (40 min)

**Objective:** Ingest 3 documents and verify grounded vs. ungrounded responses

```bash
# Step 1: Ingest your documents
for doc in vaccination-schedule flea-treatment dental-protocol; do
  curl -X POST http://localhost:8080/api/v1/rag/ingest \
    -H "Content-Type: application/json" \
    -d "{\"documentPath\":\"classpath:clinical-protocols/${doc}.pdf\",\"clinicId\":1}"
  echo "Ingested: $doc"
done

# Step 2: Run comparison test
python3 lab1c_rag_comparison.py
# Script asks 5 clinical questions and compares:
#   - Raw LLM response (no RAG)
#   - RAG-grounded response
# Measures: contains_citation, response_length, hallucination_keywords

# Step 3: Check chunk count
psql -h localhost -U petclinic -d petclinic \
  -c "SELECT count(*) as total_chunks FROM vector_store WHERE clinic_id=1;"
```

---

GO TO [DAY 1](../README.md)