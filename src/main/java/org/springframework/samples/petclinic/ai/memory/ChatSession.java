package org.springframework.samples.petclinic.ai.memory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * Represents a chat conversation session with persistent memory across page reloads.
 *
 * @author Krishna Manchikalapudi
 *
 * Represents a conversation session Auto-generates session tokens (UUID) Tracks creation
 * and update timestamps Provides utilities: `getSummary()`, `getMessageCount()`,
 * `addMessage()`
 *
 */
@Entity
@Table(name = "ai_chat_sessions")
public class ChatSession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(nullable = false, unique = true, length = 36)
	private String sessionToken = UUID.randomUUID().toString();

	@Column(nullable = false)
	private String title;

	@OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("createdAt ASC")
	private List<ChatMessage> messages = new ArrayList<>();

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(nullable = false)
	private LocalDateTime updatedAt = LocalDateTime.now();

	public ChatSession() {
	}

	public ChatSession(String title) {
		this.title = title;
	}

	// ── Getters & Setters ──
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getSessionToken() {
		return sessionToken;
	}

	public void setSessionToken(String sessionToken) {
		this.sessionToken = sessionToken;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<ChatMessage> getMessages() {
		return messages;
	}

	public void setMessages(List<ChatMessage> messages) {
		this.messages = messages;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public void addMessage(ChatMessage msg) {
		messages.add(msg);
		msg.setSession(this);
		this.updatedAt = LocalDateTime.now();
	}

	public int getMessageCount() {
		return messages.size();
	}

	public String getSummary() {
		if (messages.isEmpty()) {
			return "No messages";
		}
		ChatMessage first = messages.stream()
			.filter(m -> "user".equals(m.getRole()))
			.findFirst()
			.orElse(messages.get(0));
		String preview = first.getContent();
		if (preview.length() > 60) {
			preview = preview.substring(0, 57) + "…";
		}
		return preview;
	}

}
