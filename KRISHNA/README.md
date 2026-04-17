# Spring PetClinic — AI Chat Panel (Ollama / Local LLM)

Adds a **floating AI chat assistant** powered by a **locally-running Ollama instance** to every page. No cloud API keys required — everything runs on your machine.

## Architecture

```
Browser  ──POST /api/ai/chat──►  AiChatController  ──►  Ollama (localhost:11434)
         ◄── { "reply": "…" } ────────────────────────────────────────────────
```

The Spring backend also exposes `GET /api/ai/health` which the chat panel calls
on page load to show the active model name (or an "offline" indicator).

---
## Quick-start

### Install Ollama

#### macOS / Linux
```bash
curl -fsSL https://ollama.com/install.sh | sh
```
#### Windows 
 - download from [https://ollama.com/download](https://ollama.com/download)


### Start Ollama and pull a model
 - https://ollama.com/models
```bash
ollama serve            # starts the local API on port 11434

# In a second terminal:
ollama pull llama3.2:1b    # ~1.3 GB, fast & capable (recommended default)
# or
ollama pull qwen3.5:0.8b     # ~1.3 GB, excellent instruction-following
# or
ollama pull phi3.5:latest        # tiny, runs on any hardware
```
![Ollama models](./images/ollama-models.png) 

![Ollama run model](./images/ollama-run.png)

## AI Chat feature code
 - New files:
     - src/main/resources/templates/fragments/ai-chat.html 
         - calls `/api/ai/health` on load; reads `data.reply` (normalised response); shows model name & offline indicator
     - src/main/java/org/springframework/samples/petclinic/system/AiChatController.java
         - calls Ollama `/api/chat`; exposes `/api/ai/health`; returns `{"reply":"…"}`
     - src/main/resources/application-ollama.properties
         - `ollama.base-url=http://localhost:11434`
         - `ollama.model=llama3.2:1b`
 - Updated files:
 - src/main/resources/templates/fragments/layout.html
     - `<div th:replace="~{fragments/ai-chat :: aiChat}">`

```
spring-petclinic/
└── src/
    ├── main/resources
    │   └── application-ollama.properties
    ├── main/resources/templates/fragments/
    │   └── layout.html
    │   └── ai-chat.html             
    └── main/java/org/springframework/samples/petclinic/system/
        └── AiChatController.java 
```

### Run PetClinic
#### Build the package
```bash
./KRISHNA/exec.sh build
```
![Build success](./images/boot-build.png)

#### Start the app
```bash
./KRISHNA/exec.sh run
```
![Build run](./images/boot-run.png)

Open [http://localhost:8080](http://localhost:8080).  
The green 🐾 button appears bottom-right. The header shows the active model name.

![App home](./images/UI-home.png)
![App home](./images/UI-home-chat.png)

---

## Validate Ollama is working (before starting the app)
#### Check the service is up
```bash
curl http://localhost:11434/api/tags
```
![Ollama tags](./images/ollama-api-tags.png)

####  Quick chat test
```bash
curl http://localhost:11434/api/chat -d '{"model":"llama3.2:1b","messages":[{"role":"user","content":"Hello"}],"stream":false}'
```
![Ollama chat](./images/ollama-api-chat.png)

Once the app is running you can also hit the health endpoint:

```bash
curl http://localhost:8080/api/ai/health
# → {"status":"ok","baseUrl":"http://localhost:11434","model":"llama3.2:1b","models":["llama3.2:1b"]}
```

---
## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Header shows "Ollama offline" | Run `ollama serve` |
| Chat replies with error | Check `ollama.model` matches a pulled model (`ollama list`) |
| Very slow replies | Switch to a smaller model (`phi3`) or reduce context |
| Port conflict | Change `ollama.base-url` to match your Ollama port |
