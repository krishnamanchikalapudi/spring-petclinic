## 10:00 – 11:30 | Session 7 — Knowledge Graph + GraphRAG

**Chapters Covered:** 17, 18, 19

### Presentation (35 min)
- **Ch 17:** Why knowledge graphs — 11× faster relationship traversal vs JOIN chains, synonym resolution
- **Ch 18:** Neo4j schema — 13 node types, 18 relationships, graph ingestion pipeline
- **Ch 19:** GraphRAG — parallel RAG + KG retrieval fused via RRF, ContextFusionEngine, cited responses

### Live Demo (30 min)
```bash
# DEMO 21: Neo4j schema exploration
cypher-shell -u neo4j -p petclinic \
  "MATCH (n) RETURN labels(n) as type, count(n) as count ORDER BY count DESC"

# DEMO 22: Knowledge graph traversal — drug interactions
cypher-shell -u neo4j -p petclinic \
  "MATCH (d1:Drug {name:'Amoxicillin'})-[:INTERACTS_WITH]->(d2:Drug)
   RETURN d1.name, d2.name, 'INTERACTION'"

# Breed predispositions
cypher-shell -u neo4j -p petclinic \
  "MATCH (b:Breed {name:'Golden Retriever'})-[:PREDISPOSED_TO]->(c:Condition)
   RETURN b.name, c.name LIMIT 10"

# DEMO 23: GraphRAG vs plain RAG — cited response
echo "=== PLAIN RAG RESPONSE ==="
curl -X POST http://localhost:8080/api/v1/ai/chat/grounded \
  -d '{"message":"Is Otomax safe for Luna given her current meds?","sessionId":"plain-rag","clinicId":1}'

echo "=== GRAPHRAG RESPONSE (citations + graph context) ==="
curl -X POST http://localhost:8080/api/v1/ai/graphrag/query \
  -H "Content-Type: application/json" \
  -d '{"query":"Is Otomax safe for Luna given her current meds?","petId":42,"clinicId":1}'

# DEMO 24: Reasoning path inspection
curl http://localhost:8080/api/v1/ai/graphrag/reasoning/latest | jq '.reasoningPath'
```

### Hands-On Lab 3A — Knowledge Graph Queries + GraphRAG (30 min)

```cypher
/* LAB 3A: Write these Cypher queries in Neo4j Browser (http://localhost:7474) */

/* Q1: Find all conditions a 'Persian' cat is predisposed to */
MATCH (b:Breed {name:'Persian'})-[:PREDISPOSED_TO]->(c:Condition)
RETURN b.name, c.name, c.severity

/* Q2: Find drug interactions for Luna's current medications */
MATCH (p:Pet {name:'Luna'})-[:CURRENTLY_ON]->(d1:Drug)
MATCH (d1)-[:INTERACTS_WITH]->(d2:Drug)
RETURN p.name, d1.name as current_drug, d2.name as interacts_with

/* Q3: Find which vets treated the most cases of URI in the last 3 months */
MATCH (v:Vet)-[:TREATED]->(vis:Visit)-[:DIAGNOSED_WITH]->(c:Condition {name:'URI'})
WHERE vis.date > date() - duration({months:3})
RETURN v.lastName, count(vis) as uri_cases ORDER BY uri_cases DESC

/* Q4: Outbreak detection — same condition, multiple pets, same clinic */
MATCH (c:Condition)<-[:DIAGNOSED_WITH]-(vis:Visit)<-[:HAD]-(p:Pet)
WHERE vis.date > date() - duration({days:30})
WITH c, count(DISTINCT p) as case_count
WHERE case_count >= 3
RETURN c.name, case_count ORDER BY case_count DESC
```

---

GO TO [DAY 3](../README.md)