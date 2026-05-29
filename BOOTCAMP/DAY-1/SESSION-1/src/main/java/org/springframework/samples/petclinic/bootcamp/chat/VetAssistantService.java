package org.springframework.samples.petclinic.bootcamp.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.samples.petclinic.bootcamp.chat.ChatApiModel.*;
import org.springframework.samples.petclinic.bootcamp.memory.ChatMessage;
import org.springframework.samples.petclinic.bootcamp.memory.ChatSession;
import org.springframework.samples.petclinic.bootcamp.memory.ChatSessionRepository;
import org.springframework.samples.petclinic.bootcamp.metrics.AiMetricsService;
import org.springframework.samples.petclinic.bootcamp.system.AiContextService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Session 1 — VetAssistantService
 * <p>
 * Wraps the Ollama HTTP API with a typed, Java-25-idiomatic service layer.
 * Builds incrementally on the existing {@code AiChatController} and
 * {@code AiContextService} — it reuses the context builder and session
 * repository rather than replacing them.
 *
 * <h3>Java 25 features used</h3>
 * <ul>
 *   <li>Text blocks — system prompt assembly and JSON body construction</li>
 *   <li>Virtual threads — non-blocking Ollama HTTP calls</li>
 *   <li>Records — all request/response types from ChatApiModel</li>
 *   <li>Pattern matching for switch — ChatOutcome dispatch</li>
 *   <li>var — local type inference throughout</li>
 * </ul>
 *
 * <h3>Incremental from</h3>
 * Existing: {@code AiChatController} uses raw RestTemplate + Map responses.
 * Session 1 adds: typed API layer on top; next session (Session 2) adds RAG.
 *
 * @author Anil Kumar Veldurthi
 * @author Krishna Manchikalapudi
 * @since Session 1
 */
@Service
public class VetAssistantService {

    private static final Logger log = LoggerFactory.getLogger(VetAssistantService.class);

    // ── Configuration ─────────────────────────────────────────────────────────
    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model:qwen3.5:0.8b}")
    private String chatModel;

    @Value("${petclinic.bootcamp.prompt.vet-assistant-version:v1}")
    private String promptVersion;

    @Value("${petclinic.bootcamp.inference.timeout-seconds:30}")
    private int inferenceTimeoutSeconds;

    private static final String OLLAMA_CHAT_PATH = "/api/chat";
    private static final String OLLAMA_TAGS_PATH = "/api/tags";
    private static final int    MAX_HISTORY_PAIRS = 10;

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final AiContextService      contextService;
    private final ChatSessionRepository sessionRepository;
    private final AiMetricsService      metricsService;
    private final RestTemplate          restTemplate;
    private final ObjectMapper          objectMapper;

    /** Java 25 virtual-thread executor — each Ollama call runs on its own virtual thread */
    private final ExecutorService virtualThreadExecutor =
        Executors.newVirtualThreadPerTaskExecutor();

    private String systemPromptTemplate;

    public VetAssistantService(
        AiContextService      contextService,
        ChatSessionRepository sessionRepository,
        AiMetricsService      metricsService
    ) {
        this.contextService    = contextService;
        this.sessionRepository = sessionRepository;
        this.metricsService    = metricsService;
        this.restTemplate      = new RestTemplate();
        this.objectMapper      = new ObjectMapper();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @PostConstruct
    void init() {
        this.systemPromptTemplate = loadSystemPrompt();
        log.info("VetAssistantService initialized — model={} prompt={}", chatModel, promptVersion);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Stateless single-turn chat — no session persistence.
     * <p>
     * Use when the caller does not provide a sessionId (anonymous queries,
     * health checks, one-off FAQ answers).
     *
     * @param request validated {@link ChatRequest}
     * @return {@link ChatOutcome} — Success or Failure (sealed)
     */
    public ChatOutcome chatStateless(ChatRequest request) {
        return chat(request, null);
    }

    /**
     * Stateful multi-turn chat with session memory.
     * <p>
     * Loads up to {@value #MAX_HISTORY_PAIRS} prior turns from the DB,
     * appends the current turn, calls Ollama, persists the response.
     *
     * @param request   validated {@link ChatRequest}
     * @param sessionId existing session token, or {@code null} to create a new session
     * @return {@link ChatOutcome} — Success or Failure (sealed)
     */
    public ChatOutcome chat(ChatRequest request, String sessionId) {
        var start = System.currentTimeMillis();

        try {
            // ── 1. Resolve or create session ────────────────────────────────
            var session = resolveSession(sessionId, request.message());

            // ── 2. Build DB context (reuses existing AiContextService) ───────
            var contextResult = contextService.buildContextWithResult(request.message());

            // ── 3. Assemble system prompt with DB context ───────────────────
            // Java 25 text block for readable multi-line string assembly
            var systemPrompt = systemPromptTemplate + contextResult.context();

            // ── 4. Build Ollama message list with trimmed history ────────────
            var history      = loadTrimmedHistory(session);
            var ollamaMessages = new ArrayList<OllamaMessage>();
            ollamaMessages.add(OllamaMessage.system(systemPrompt));
            ollamaMessages.addAll(history);
            ollamaMessages.add(OllamaMessage.user(request.message()));

            // ── 5. Call Ollama via virtual thread (non-blocking) ─────────────
            var ollamaRequest = OllamaChatRequest.of(chatModel, ollamaMessages);
            var reply = callOllamaOnVirtualThread(ollamaRequest);

            // ── 6. Persist turn to DB ────────────────────────────────────────
            persistTurn(session, request.message(), reply);

            // ── 7. Build typed response ──────────────────────────────────────
            var durationMs = System.currentTimeMillis() - start;
            metricsService.recordChatDuration(durationMs, chatModel);

            var response = new ChatResponse(
                reply,
                session.getSessionToken(),
                chatModel,
                promptVersion,
                durationMs,
                false
            );
            return new ChatOutcome.Success(response);

        } catch (ResourceAccessException ex) {
            log.error("Ollama not reachable at {}: {}", ollamaBaseUrl, ex.getMessage());
            metricsService.recordError("OLLAMA_UNAVAILABLE");
            return ChatOutcome.Failure.ollamaUnavailable(ollamaBaseUrl);

        } catch (TimeoutException ex) {
            log.error("Ollama inference timed out after {}s", inferenceTimeoutSeconds);
            metricsService.recordError("INFERENCE_TIMEOUT");
            return ChatOutcome.Failure.internalError(
                "Inference timed out after " + inferenceTimeoutSeconds + "s");

        } catch (Exception ex) {
            log.error("VetAssistantService unexpected error: {}", ex.getMessage(), ex);
            metricsService.recordError("INTERNAL_ERROR");
            return ChatOutcome.Failure.internalError(ex.getMessage());
        }
    }

    /**
     * Health check — verifies Ollama is reachable and the configured model is loaded.
     *
     * @return {@link ChatApiModel.HealthResponse}
     */
    @SuppressWarnings("unchecked")
    public HealthResponse health() {
        try {
            var tagsResponse = restTemplate.getForObject(
                ollamaBaseUrl + OLLAMA_TAGS_PATH,
                Map.class
            );

            var modelNames = new ArrayList<String>();
            if (tagsResponse != null && tagsResponse.containsKey("models")) {
                var models = (List<Map<String, Object>>) tagsResponse.get("models");
                for (var m : models) {
                    if (m.get("name") instanceof String name) {
                        modelNames.add(name);
                    }
                }
            }

            var status = modelNames.stream().anyMatch(n -> n.contains(chatModel))
                ? "UP" : "DEGRADED";

            return new HealthResponse(
                status, chatModel, promptVersion, ollamaBaseUrl,
                modelNames, Instant.now()
            );

        } catch (ResourceAccessException ex) {
            return new HealthResponse(
                "DOWN", chatModel, promptVersion, ollamaBaseUrl,
                List.of(), Instant.now()
            );
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Resolves an existing session or creates a new one.
     * Uses Java 25 var for concise local type inference.
     */
    private ChatSession resolveSession(String sessionId, String firstMessage) {
        if (sessionId != null && !sessionId.isBlank()) {
            return sessionRepository.findBySessionToken(sessionId)
                .orElseGet(() -> createSession(firstMessage));
        }
        return createSession(firstMessage);
    }

    private ChatSession createSession(String firstMessage) {
        var title = firstMessage.substring(0, Math.min(80, firstMessage.length()));
        var session = new ChatSession(title);
        return sessionRepository.save(session);
    }

    /**
     * Loads conversation history trimmed to {@value #MAX_HISTORY_PAIRS} turn pairs.
     * Maps {@link ChatMessage} entities to {@link OllamaMessage} records.
     */
    private List<OllamaMessage> loadTrimmedHistory(ChatSession session) {
        var messages = session.getMessages();
        var start = Math.max(0, messages.size() - (MAX_HISTORY_PAIRS * 2));
        return messages.subList(start, messages.size())
                       .stream()
                       .map(m -> new OllamaMessage(m.getRole(), m.getContent()))
                       .toList();    // Java 25 — Stream.toList() is unmodifiable
    }

    /**
     * Calls Ollama on a Java 25 virtual thread with a configurable timeout.
     * Virtual threads make this non-blocking without callback complexity.
     *
     * @throws TimeoutException if inference exceeds {@code inferenceTimeoutSeconds}
     */
    @SuppressWarnings("unchecked")
    private String callOllamaOnVirtualThread(OllamaChatRequest ollamaRequest)
            throws Exception {

        // Java 25 text block — JSON body for Ollama /api/chat
        var jsonBody = """
            {
              "model":   "%s",
              "messages": %s,
              "stream":  false,
              "options": {
                "temperature": %.1f,
                "num_predict": %d
              }
            }
            """.formatted(
                ollamaRequest.model(),
                objectMapper.writeValueAsString(ollamaRequest.messages()),
                ollamaRequest.options().temperature(),
                ollamaRequest.options().numPredict()
            );

        // Submit inference to a virtual thread
        Future<String> future = virtualThreadExecutor.submit(() -> {
            var response = restTemplate.postForObject(
                ollamaBaseUrl + OLLAMA_CHAT_PATH,
                buildHttpEntity(jsonBody),
                Map.class
            );

            if (response == null) {
                throw new IllegalStateException("Empty response from Ollama");
            }

            // Pattern matching for instanceof (Java 25)
            if (response.get("message") instanceof Map<?,?> messageMap
                    && messageMap.get("content") instanceof String content) {
                return content.strip();
            }
            throw new IllegalStateException("Unexpected Ollama response: " + response);
        });

        try {
            return future.get(inferenceTimeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw ex;
        }
    }

    /**
     * Persists user + assistant turn to the session.
     * Avoids duplicating turns already saved by the existing AiChatController.
     */
    private void persistTurn(ChatSession session, String userMessage, String assistantReply) {
        // Guard: skip if this exact message is already the last saved turn
        var messages = session.getMessages();
        boolean alreadySaved = !messages.isEmpty()
            && "user".equals(messages.getLast().getRole())   // Java 21+ SequencedCollection
            && messages.getLast().getContent().equals(userMessage);

        if (!alreadySaved) {
            session.addMessage(new ChatMessage(session, "user",      userMessage));
            session.addMessage(new ChatMessage(session, "assistant", assistantReply));
            session.setUpdatedAt(LocalDateTime.now());
            sessionRepository.save(session);
        }
    }

    /**
     * Loads the versioned system prompt from the classpath.
     * Path: {@code classpath:prompts/vet-assistant/<version>/system.txt}
     */
    private String loadSystemPrompt() {
        var path = "prompts/vet-assistant/" + promptVersion + "/system.txt";
        try {
            var resource = new ClassPathResource(path);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.warn("Could not load prompt at '{}', using inline fallback: {}", path, ex.getMessage());
            // Java 25 text block fallback
            return """
                You are a helpful veterinary assistant embedded in Spring PetClinic.
                Answer questions about pet health, appointments, and clinic services.
                Always recommend consulting a veterinarian for medical decisions.
                """;
        }
    }

    /**
     * Wraps a JSON string in a Spring HTTP entity with correct Content-Type header.
     */
    private org.springframework.http.HttpEntity<String> buildHttpEntity(String json) {
        var headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return new org.springframework.http.HttpEntity<>(json, headers);
    }
}
