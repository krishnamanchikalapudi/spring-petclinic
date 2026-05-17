package org.springframework.samples.petclinic.ai.system;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.ai.memory.ChatMessage;
import org.springframework.samples.petclinic.ai.memory.ChatSession;
import org.springframework.samples.petclinic.ai.memory.ChatSessionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * @author Krishna Manchikalapudi
 *
 * Backend proxy controller that routes AI chat requests to a locally-running
 */
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

	private static final String OLLAMA_CHAT_PATH = "/api/chat";

	private static final String OLLAMA_TAGS_PATH = "/api/tags";

	private static final int MAX_HISTORY_PAIRS = 10;

	private static final String BASE_SYSTEM_PROMPT = "You are a helpful AI assistant embedded in Spring PetClinic.\n\n"
			+ "Your capabilities:\n" + "1. Answer questions about real clinic data using the live DB snapshot below.\n"
			+ "2. Guide users through the application (navigation, forms, features).\n\n" + "Navigation:\n"
			+ "  Home: /  |  Find owners: /owners/find  |  Add owner: /owners/new  |  Vets: /vets.html\n\n" + "Rules:\n"
			+ "- Answer ONLY from the DB snapshot — never invent data not listed there.\n"
			+ "- Be concise and friendly.  Plain text; no markdown tables.\n"
			+ "- If data is absent from the snapshot, say so honestly.\n"
			+ "- If the question is unrelated to the clinic, politely redirect.\n";

	private static final String FALLBACK_SYSTEM_PROMPT = "You are a helpful AI assistant embedded in Spring PetClinic.\n\n"
			+ "Note: The live clinic database is currently unavailable, so you cannot access real clinic data.\n\n"
			+ "Your capabilities:\n" + "1. Guide users through the application (navigation, forms, features).\n"
			+ "2. Provide general information about pet clinics and veterinary care.\n\n" + "Navigation:\n"
			+ "  Home: /  |  Find owners: /owners/find  |  Add owner: /owners/new  |  Vets: /vets.html\n\n" + "Rules:\n"
			+ "- Be concise and friendly.  Plain text; no markdown tables.\n"
			+ "- Let the user know the database is currently unavailable if they ask about specific clinic data.\n"
			+ "- Provide helpful general guidance about the application.\n";

	@Value("${ollama.base-url:http://localhost:11434}")
	private String ollamaBaseUrl;

	@Value("${ollama.model:qwen3.5:0.8b}")
	private String ollamaModel;

	private final AiContextService contextService;

	private final ChatSessionRepository sessionRepository;

	private final RestTemplate restTemplate = new RestTemplate();

	public AiChatController(AiContextService contextService, ChatSessionRepository sessionRepository) {
		this.contextService = contextService;
		this.sessionRepository = sessionRepository;
	}

	@PostMapping("/chat")
	public ResponseEntity<?> chat(@RequestBody ChatRequest request) {
		if (request.messages() == null || request.messages().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "messages array is required"));
		}

		// Load or create session
		ChatSession session = null;
		if (request.sessionToken() != null && !request.sessionToken().isEmpty()) {
			session = sessionRepository.findBySessionToken(request.sessionToken()).orElse(null);
		}
		if (session == null) {
			// Create new session with auto-generated title based on first user message
			String firstUserMsg = request.messages()
				.stream()
				.filter(m -> "user".equals(m.role()))
				.map(Message::content)
				.findFirst()
				.orElse("New Chat");
			String title = firstUserMsg.substring(0, Math.min(100, firstUserMsg.length()));
			session = new ChatSession(title);
			session = sessionRepository.save(session);
		}

		List<Message> history = new ArrayList<>(request.messages());
		if (history.size() > MAX_HISTORY_PAIRS * 2) {
			history = history.subList(history.size() - MAX_HISTORY_PAIRS * 2, history.size());
		}

		String latestUserMsg = history.stream()
			.filter(m -> "user".equals(m.role()))
			.reduce((a, b) -> b)
			.map(Message::content)
			.orElse("");

		// Build context and check if real data was found
		AiContextService.ContextResult contextResult = contextService.buildContextWithResult(latestUserMsg);
		String dbContext = contextResult.context();
		boolean dataFound = contextResult.dataFound();

		// Use fallback prompt if no data was found, otherwise use base prompt with
		// context
		String systemPrompt = dataFound ? BASE_SYSTEM_PROMPT + dbContext : FALLBACK_SYSTEM_PROMPT;

		List<Map<String, String>> ollamaMessages = new ArrayList<>();
		ollamaMessages.add(Map.of("role", "system", "content", systemPrompt));
		for (Message m : history) {
			ollamaMessages.add(Map.of("role", m.role(), "content", m.content()));
		}

		Map<String, Object> ollamaBody = Map.of("model", ollamaModel, "messages", ollamaMessages, "stream", false);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(ollamaBody, headers);

		try {
			@SuppressWarnings("unchecked")
			ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(ollamaBaseUrl + OLLAMA_CHAT_PATH,
					entity, (Class<Map<String, Object>>) (Class<?>) Map.class);

			Map<String, Object> body = response.getBody();
			if (body == null)
				return serverError("Empty response from Ollama");

			@SuppressWarnings("unchecked")
			Map<String, String> messageObj = (Map<String, String>) body.get("message");
			if (messageObj == null)
				return serverError("Unexpected Ollama response format");

			String reply = messageObj.getOrDefault("content", "").strip();

			// Save messages to database
			Message lastClientMsg = history.stream()
				.filter(m -> "user".equals(m.role()))
				.reduce((a, b) -> b)
				.orElse(null);

			if (lastClientMsg != null && session.getMessages()
				.stream()
				.noneMatch(m -> "user".equals(m.getRole()) && m.getContent().equals(lastClientMsg.content()))) {
				session.addMessage(new ChatMessage(session, "user", lastClientMsg.content()));
				session.addMessage(new ChatMessage(session, "assistant", reply));
				session.setUpdatedAt(LocalDateTime.now());
				sessionRepository.save(session);
			}

			return ResponseEntity.ok(Map.of("reply", reply, "sessionToken", session.getSessionToken()));

		}
		catch (ResourceAccessException ex) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(Map.of("error", "Ollama is not reachable at " + ollamaBaseUrl + ". Please run: ollama serve"));
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

	/**
	 * List all recent chat sessions (limited to 20 most recent)
	 */
	@GetMapping("/sessions")
	public ResponseEntity<?> listSessions(@RequestParam(defaultValue = "20") int limit) {
		try {
			List<ChatSession> sessions = sessionRepository.findRecentSessions(Math.min(limit, 50));
			List<Map<String, Object>> result = sessions.stream()
				.map(s -> Map.of("id", (Object) s.getId(), "sessionToken", s.getSessionToken(), "title", s.getTitle(),
						"messageCount", s.getMessageCount(), "summary", s.getSummary(), "createdAt",
						s.getCreatedAt().toString(), "updatedAt", s.getUpdatedAt().toString()))
				.toList();
			return ResponseEntity.ok(result);
		}
		catch (Exception ex) {
			return serverError(ex.getMessage());
		}
	}

	/**
	 * Get a specific chat session with all messages
	 */
	@GetMapping("/sessions/{sessionToken}")
	public ResponseEntity<?> getSession(@PathVariable String sessionToken) {
		try {
			Optional<ChatSession> session = sessionRepository.findBySessionToken(sessionToken);
			if (session.isEmpty()) {
				return ResponseEntity.notFound().build();
			}

			ChatSession s = session.get();
			List<Map<String, String>> messages = s.getMessages()
				.stream()
				.map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
				.toList();

			return ResponseEntity
				.ok(Map.of("id", s.getId(), "sessionToken", s.getSessionToken(), "title", s.getTitle(), "messages",
						messages, "createdAt", s.getCreatedAt().toString(), "updatedAt", s.getUpdatedAt().toString()));
		}
		catch (Exception ex) {
			return serverError(ex.getMessage());
		}
	}

	/**
	 * Create a new chat session
	 */
	@PostMapping("/sessions")
	public ResponseEntity<?> createSession(@RequestBody Map<String, String> body) {
		try {
			String title = body.getOrDefault("title", "New Chat");
			ChatSession session = new ChatSession(title);
			session = sessionRepository.save(session);

			return ResponseEntity.ok(Map.of("id", session.getId(), "sessionToken", session.getSessionToken(), "title",
					session.getTitle()));
		}
		catch (Exception ex) {
			return serverError(ex.getMessage());
		}
	}

	/**
	 * Delete a chat session
	 */
	@PostMapping("/sessions/{sessionToken}/delete")
	public ResponseEntity<?> deleteSession(@PathVariable String sessionToken) {
		try {
			Optional<ChatSession> session = sessionRepository.findBySessionToken(sessionToken);
			if (session.isEmpty()) {
				return ResponseEntity.notFound().build();
			}

			sessionRepository.delete(session.get());
			return ResponseEntity.ok(Map.of("message", "Session deleted"));
		}
		catch (Exception ex) {
			return serverError(ex.getMessage());
		}
	}

	private ResponseEntity<Map<String, Object>> serverError(String message) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(Map.of("error", "AI service error: " + message));
	}

	public record ChatRequest(List<Message> messages, String sessionToken) {
	}

	public record Message(String role, String content) {
	}

}
