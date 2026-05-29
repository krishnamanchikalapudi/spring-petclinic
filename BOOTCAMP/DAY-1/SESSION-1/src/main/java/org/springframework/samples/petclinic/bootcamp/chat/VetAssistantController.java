package org.springframework.samples.petclinic.bootcamp.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.bootcamp.chat.ChatApiModel.*;
import org.springframework.web.bind.annotation.*;

/**
 * Session 1 — VetAssistantController
 * <p>
 * Typed REST controller that exposes the new {@link VetAssistantService} API.
 * Builds incrementally on top of the existing {@code AiChatController}
 * (which continues to work unchanged at {@code /api/ai/chat}).
 *
 * <p>New endpoints added in Session 1:
 * <pre>
 *   POST /api/ai/vet/chat?sessionId={token}   — typed stateful / stateless chat
 *   GET  /api/ai/vet/health                   — model health check
 * </pre>
 *
 * <h3>Java 25 feature — Pattern Matching for switch</h3>
 * <p>The {@code switch} on {@link ChatOutcome} is exhaustive because the interface is
 * sealed. The compiler enforces that every subtype (Success, Failure) is handled.
 * No {@code default} branch needed.
 *
 * @author Anil Kumar Veldurthi
 * @author Krishna Manchikalapudi
 * @since Session 1
 */
@RestController
@RequestMapping("/api/ai/vet")
public class VetAssistantController {

    private static final Logger log = LoggerFactory.getLogger(VetAssistantController.class);

    private final VetAssistantService vetAssistantService;

    public VetAssistantController(VetAssistantService vetAssistantService) {
        this.vetAssistantService = vetAssistantService;
    }

    // ── Chat ──────────────────────────────────────────────────────────────────

    /**
     * Stateful (or stateless) chat endpoint.
     *
     * <p>If {@code sessionId} is omitted, a new session is created automatically.
     * Pass the returned {@code sessionId} back on subsequent calls to maintain
     * multi-turn conversation context.
     *
     * <p>Request body:
     * <pre>{@code {"message": "Is ibuprofen safe for dogs?"}}</pre>
     *
     * <p>Success response (200):
     * <pre>{@code
     * {
     *   "response":      "No, ibuprofen is toxic to dogs...",
     *   "sessionId":     "abc-123-def",
     *   "modelVersion":  "qwen3.5:0.8b",
     *   "promptVersion": "v1",
     *   "durationMs":    2341,
     *   "cached":        false
     * }
     * }</pre>
     *
     * <p>Error response (503 / 400 / 500):
     * <pre>{@code {"error": "MODEL_UNAVAILABLE", "detail": "Ollama is not reachable..."}}</pre>
     *
     * @param request   the chat request (validated in record constructor)
     * @param sessionId optional session token for multi-turn context
     * @return typed {@link ResponseEntity}
     */
    @PostMapping("/chat")
    public ResponseEntity<?> chat(
        @RequestBody   ChatRequest request,
        @RequestParam(required = false) String sessionId
    ) {
        log.debug("POST /api/ai/vet/chat sessionId={} message='{}'",
            sessionId, request.message().substring(0, Math.min(80, request.message().length())));

        var outcome = vetAssistantService.chat(request, sessionId);

        // ── Java 25 — pattern matching switch on sealed ChatOutcome ──────────
        // Exhaustive: compiler enforces both Success and Failure are handled.
        return switch (outcome) {
            case ChatOutcome.Success success ->
                ResponseEntity.ok(success.response());

            case ChatOutcome.Failure failure ->
                ResponseEntity.status(failure.httpStatus()).body(failure.error());
        };
    }

    // ── Health ────────────────────────────────────────────────────────────────

    /**
     * Model health check — verifies Ollama is reachable and the configured model
     * ({@code qwen3.5:0.8b}) is loaded.
     *
     * <p>Response (200 when UP):
     * <pre>{@code
     * {
     *   "status":          "UP",
     *   "modelVersion":    "qwen3.5:0.8b",
     *   "promptVersion":   "v1",
     *   "ollamaBaseUrl":   "http://localhost:11434",
     *   "availableModels": ["qwen3.5:0.8b", "nomic-embed-text:latest"],
     *   "checkedAt":       "2026-05-28T10:00:00Z"
     * }
     * }</pre>
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        var healthResponse = vetAssistantService.health();
        var httpStatus = switch (healthResponse.status()) {
            case "UP"       -> 200;
            case "DEGRADED" -> 207;
            default         -> 503;
        };
        return ResponseEntity.status(httpStatus).body(healthResponse);
    }

    // ── Exception handler ─────────────────────────────────────────────────────

    /**
     * Handles validation failures from the {@link ChatRequest} record constructor.
     * Returns 400 with a typed {@link ErrorResponse}.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(IllegalArgumentException ex) {
        log.warn("Chat request validation failed: {}", ex.getMessage());
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("VALIDATION_ERROR", ex.getMessage()));
    }
}
