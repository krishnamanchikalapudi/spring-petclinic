#!/bin/bash

# QUERIES=(
#   "Is ibuprofen safe for dogs?"
#   "What are symptoms of feline diabetes?"
#   "How often should I vaccinate my rabbit?"
#   "My cat has been vomiting for 2 days. What should I do?"
#   "What is the recommended dose of amoxicillin for a 10kg dog?"
# )

QUERIES=(
  "What is Spring PetClinic?"
  "How do I create a new owner in Spring PetClinic?"
  "What database schema changes are needed to store pet vaccination dates?"
)

# ref: https://docs.ollama.com/api/introduction
# curl http://localhost:11434/api/generate -d '{"model": "qwen3.5:0.8b", "prompt": "What database schema changes are needed to store pet vaccination dates?"}'
for q in "${QUERIES[@]}"; do
  echo "QUERY: $q"
  START=$(date +%s%N)
  RESPONSE=$(curl -m 5 http://localhost:11434/api/generate -d "{\"model\":\"qwen3.5:0.8b\",\"prompt\":\"$q Answer in 2 sentences.\"}")
  END=$(date +%s%N)
  MS=$(( (END - START) / 1000000 ))
  echo "RESPONSE: $(echo $RESPONSE | jq -r '.response')"
  echo "TIME: ${MS}ms"
  echo "---"
done
