## Prerequisites for BOOTCAMP

**Before Day 1 — participants must have:**
- Java 25 + Apache Maven 3.9+ installed
- Docker Desktop running (8GB+ RAM allocated)
- Git 
- IDE `VS Code` with extensions: 
    - Postman
    ![VS Code - Postman](./images/vscode-postman.png)
    - Docker
    ![VS Code - Docker](./images/vscode-docker.png)
    - Container Tools
    ![VS Code - Container Tools](./mages/vscode-containertools.png)
    - Kafka
    ![VS Code - Kafka](./images/vscode-kafka.png)
    - Redis
    ![VS Code - Redis](./images/vscode-redis.png)
    - Neo4J
    ![VS Code - Neo4J](./images/vscode-neo4j.png)
    - Postgres
    ![VS Code - Postgres](./images/vscode-postgres.png)

- Ollama installed: `curl -fsSL https://ollama.ai/install.sh | sh`
    - Models pre-pulled (do this before arrival — ~1.25GB total):
  ```bash
  ollama pull qwen3.5:0.8b
  ollama pull nomic-embed-text
  ```
![Ollama models](./images/ollama-models.png)
- Clone the starter repo:
  ```bash
  git clone -b bootcamp https://github.com/krishnamanchikalapudi/spring-petclinic.git
  ```

---

GO TO **[README](./README.md)**