package org.springframework.samples.petclinic.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * @author Krishna Manchikalapudi Backend proxy controller that routes AI chat requests to
 * a locally-running
 */
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

	// Ollama API paths
	private static final String OLLAMA_CHAT_PATH = "/api/chat";

	private static final String OLLAMA_TAGS_PATH = "/api/tags";

	// Conversation limits
	/** Maximum user+assistant message pairs kept in context. */
	private static final int MAX_HISTORY_PAIRS = 10;

	// PetClinic system prompt
	private static final String SYSTEM_PROMPT = "You are a helpful AI assistant embedded in Spring PetClinic, a web application "
			+ "for managing a veterinary clinic. Your job is to help users navigate and use " + "the application.\n\n" +

			"Key features you can assist with:\n"
			+ "- Owner management: adding, editing, and searching for owners (people who own pets)\n"
			+ "- Pet management: adding pets to owners; supported types are cat, dog, bird, "
			+ "  hamster, lizard, snake\n" + "- Visit scheduling: booking vet visits with a date and description\n"
			+ "- Veterinarian information: browsing available vets and their specialties\n\n" +

			"Application navigation:\n" + "- Home:         /\n" + "- Find owners:  /owners/find\n"
			+ "- Add owner:    /owners/new\n" + "- Veterinarians: /vets.html\n\n" +

			"Guidelines:\n" + "- Keep answers concise, friendly, and specific to PetClinic.\n"
			+ "- Use plain text; avoid markdown tables or code blocks.\n"
			+ "- If a question is unrelated to the clinic, politely redirect the user.\n"
			+ "- Never invent features that do not exist in the application.";

	// Configurable properties
	@Value("${ollama.base-url:http://localhost:11434}")
	private String ollamaBaseUrl;

	@Value("${ollama.model:llama3.2:1b}")
	private String ollamaModel;

	private final RestTemplate restTemplate = new RestTemplate();

	// ──────────────────────
	// POST /api/ai/chat
	// ──────────────────────

	/**
	 * Accepts a conversation and returns the AI reply.
	 *
	 * <p>
	 * Request body: <pre>{@code
	 * {
	 *   "messages": [
	 *     {"role": "user",      "content": "How do I add a new owner?"},
	 *     {"role": "assistant", "content": "Go to /owners/new …"},
	 *     {"role": "user",      "content": "And how do I add a pet?"}
	 *   ]
	 * }
	 * }</pre>
	 *
	 * <p>
	 * Response body: <pre>{@code
	 * { "reply": "To add a pet, open the owner's detail page …" }
	 * }</pre>
	 */
	@PostMapping("/chat")
	public ResponseEntity<?> chat(@RequestBody ChatRequest request) {

		// Validate input ─
		if (request.messages() == null || request.messages().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "messages array is required"));
		}

		// Trim history to last MAX_HISTORY_PAIRS pairs ──────────────────────
		List<Message> history = request.messages();
		if (history.size() > MAX_HISTORY_PAIRS * 2) {
			history = history.subList(history.size() - MAX_HISTORY_PAIRS * 2, history.size());
		}

		// Build Ollama request (system message prepended) ───────────────────
		List<Map<String, String>> ollamaMessages = new ArrayList<>();
		ollamaMessages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
		for (Message m : history) {
			ollamaMessages.add(Map.of("role", m.role(), "content", m.content()));
		}

		Map<String, Object> ollamaBody = Map.of("model", ollamaModel, "messages", ollamaMessages, "stream", false);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(ollamaBody, headers);

		// Call Ollama ────
		try {
			@SuppressWarnings("unchecked")
			ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(ollamaBaseUrl + OLLAMA_CHAT_PATH,
					entity, (Class<Map<String, Object>>) (Class<?>) Map.class);

			// Ollama response shape: { "message": { "role": "assistant", "content": "..."
			// } }
			Map<String, Object> body = response.getBody();
			if (body == null) {
				return serverError("Empty response from Ollama");
			}

			@SuppressWarnings("unchecked")
			Map<String, String> messageObj = (Map<String, String>) body.get("message");
			if (messageObj == null) {
				return serverError("Unexpected Ollama response format");
			}

			String reply = messageObj.getOrDefault("content", "").strip();
			return ResponseEntity.ok(Map.of("reply", reply));

		}
		catch (ResourceAccessException ex) {
			// Ollama is not running or not reachable
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(Map.of("error",
						"Ollama is not reachable at " + ollamaBaseUrl + ". " + "Please run: ollama serve"));

		}
		catch (HttpClientErrorException ex) {
			String msg = ex.getResponseBodyAsString();
			return ResponseEntity.status(ex.getStatusCode())
				.body(Map.of("error", msg.isBlank() ? ex.getMessage() : msg));

		}
		catch (Exception ex) {
			return serverError(ex.getMessage());
		}
	}

	// ──────────────────────
	// GET /api/ai/health
	// ──────────────────────

	/**
	 * Checks whether Ollama is running and lists the locally available models.
	 *
	 * <p>
	 * Response body (success): <pre>{@code
	 * {
	 *   "status":  "ok",
	 *   "baseUrl": "http://localhost:11434",
	 *   "model":   "llama3.2",
	 *   "models":  ["llama3.2", "mistral", "phi3"]
	 * }
	 * }</pre>
	 */
	@GetMapping("/health")
	public ResponseEntity<?> health() {
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> tagsResponse = restTemplate.getForObject(ollamaBaseUrl + OLLAMA_TAGS_PATH,
					(Class<Map<String, Object>>) (Class<?>) Map.class);

			List<String> modelNames = new ArrayList<>();
			if (tagsResponse != null && tagsResponse.containsKey("models")) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> models = (List<Map<String, Object>>) tagsResponse.get("models");
				for (Map<String, Object> m : models) {
					Object name = m.get("name");
					if (name != null)
						modelNames.add(name.toString());
				}
			}

			return ResponseEntity
				.ok(Map.of("status", "ok", "baseUrl", ollamaBaseUrl, "model", ollamaModel, "models", modelNames));

		}
		catch (ResourceAccessException ex) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(Map.of("status", "unavailable", "baseUrl", ollamaBaseUrl, "error",
						"Ollama is not running. Start it with: ollama serve"));
		}
		catch (Exception ex) {
			return serverError(ex.getMessage());
		}
	}

	// ──────────────────────
	// Private helpers
	// ──────────────────────

	private ResponseEntity<Map<String, Object>> serverError(String message) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(Map.of("error", "AI service error: " + message));
	}

	// ──────────────────────
	// Records
	// ──────────────────────

	/** Incoming chat request from the browser. */
	public record ChatRequest(List<Message> messages) {
	}

	/** A single conversation turn. */
	public record Message(String role, String content) {
	}

}
