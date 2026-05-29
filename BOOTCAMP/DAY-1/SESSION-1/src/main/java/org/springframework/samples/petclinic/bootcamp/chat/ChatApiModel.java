package org.springframework.samples.petclinic.bootcamp.chat;

import java.time.Instant;
import java.util.List;

/**
 * Session 1 — Typed Chat API Model
 * <p>
 * Java 25 records and sealed interface replace the raw {@code Map<String,Object>}
 * responses used in the existing {@code AiChatController}. This file defines every
 * request/response type for the {@code VetAssistantController}.
 *
 * <h3>Java 25 features used</h3>
 * <ul>
 *   <li>Records — immutable, compact value carriers</li>
 *   <li>Sealed interface — exhaustive ChatOutcome hierarchy</li>
 *   <li>Pattern matching for switch — dispatch on ChatOutcome subtypes</li>
 * </ul>
 *
 * @author Anil Kumar Veldurthi
 * @author Krishna Manchikalapudi
 * @since Session 1
 */
public final class ChatApiModel {

    private ChatApiModel() {}   // utility class — no instantiation

    // ── Inbound ──────────────────────────────────────────────────────────────

    /**
     * Single chat turn sent by the front-end.
     *
     * <pre>{@code
     * POST /api/ai/vet/chat
     * {"message": "Is ibuprofen safe for dogs?"}
     * }</pre>
     */
    public record ChatRequest(String message) {
        public ChatRequest {
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("message must not be blank");
            }
        }
    }

    /**
     * One turn in a conversation — maps directly to the Ollama API message format.
     */
    public record OllamaMessage(String role, String content) {
        public static OllamaMessage system(String content)    { return new OllamaMessage("system",    content); }
        public static OllamaMessage user(String content)      { return new OllamaMessage("user",      content); }
        public static OllamaMessage assistant(String content) { return new OllamaMessage("assistant", content); }
    }

    /**
     * Request body sent to the Ollama /api/chat endpoint.
     * {@code stream} is always false — we read the full response synchronously.
     */
    public record OllamaChatRequest(
        String model,
        List<OllamaMessage> messages,
        boolean stream,
        OllamaOptions options
    ) {
        public static OllamaChatRequest of(String model, List<OllamaMessage> messages) {
            return new OllamaChatRequest(model, messages, false,
                new OllamaOptions(0.3, 800));
        }
    }

    /**
     * Per-request inference options forwarded to Ollama.
     *
     * @param temperature lower = more deterministic (0.1–0.3 for clinical use)
     * @param numPredict  max tokens to generate
     */
    public record OllamaOptions(double temperature, int numPredict) {}

    // ── Outbound ─────────────────────────────────────────────────────────────

    /**
     * Successful chat response returned to the caller.
     *
     * <pre>{@code
     * {
     *   "response":     "No, ibuprofen is toxic to dogs...",
     *   "sessionId":    "abc-123",
     *   "modelVersion": "qwen3.5:0.8b",
     *   "promptVersion":"v1",
     *   "durationMs":   2341,
     *   "cached":       false
     * }
     * }</pre>
     */
    public record ChatResponse(
        String response,
        String sessionId,
        String modelVersion,
        String promptVersion,
        long   durationMs,
        boolean cached
    ) {}

    /**
     * Error response with machine-readable code.
     *
     * <pre>{@code
     * {"error":"MODEL_UNAVAILABLE","detail":"Ollama not reachable at http://localhost:11434"}
     * }</pre>
     */
    public record ErrorResponse(String error, String detail) {}

    // ── Sealed outcome hierarchy (Java 25) ───────────────────────────────────

    /**
     * Sealed interface — every path through VetAssistantService returns exactly one
     * of these two subtypes. The controller uses pattern-matching switch to dispatch.
     *
     * <pre>{@code
     * ChatOutcome outcome = service.chat(request, sessionId);
     * return switch (outcome) {
     *     case ChatOutcome.Success s -> ResponseEntity.ok(s.response());
     *     case ChatOutcome.Failure f -> ResponseEntity.status(f.httpStatus()).body(f.error());
     * };
     * }</pre>
     */
    public sealed interface ChatOutcome permits ChatOutcome.Success, ChatOutcome.Failure {

        /** The inference completed and a typed response is available. */
        record Success(ChatResponse response) implements ChatOutcome {}

        /** The inference failed — includes an HTTP status code for the controller. */
        record Failure(int httpStatus, ErrorResponse error) implements ChatOutcome {

            /** 503 — Ollama not reachable */
            static Failure ollamaUnavailable(String baseUrl) {
                return new Failure(503, new ErrorResponse(
                    "MODEL_UNAVAILABLE",
                    "Ollama is not reachable at " + baseUrl + ". Run: ollama serve"
                ));
            }

            /** 400 — bad request from the caller */
            static Failure badRequest(String detail) {
                return new Failure(400, new ErrorResponse("BAD_REQUEST", detail));
            }

            /** 500 — unexpected internal error */
            static Failure internalError(String detail) {
                return new Failure(500, new ErrorResponse("INTERNAL_ERROR", detail));
            }
        }
    }

    // ── Health ────────────────────────────────────────────────────────────────

    /**
     * Response for GET /api/ai/vet/health.
     */
    public record HealthResponse(
        String  status,
        String  modelVersion,
        String  promptVersion,
        String  ollamaBaseUrl,
        List<String> availableModels,
        Instant checkedAt
    ) {}
}
