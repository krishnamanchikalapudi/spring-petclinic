package org.springframework.samples.petclinic.ai.memory;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * @author Krishna Manchikalapudi
 *
 * Represents a single message in a chat session. Represents individual messages Stores
 * role ("user" or "assistant"), content, and timestamp Links to parent ChatSession with
 * cascade delete
 */
@Entity
@Table(name = "ai_chat_messages")
public class ChatMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne
	@JoinColumn(name = "session_id", nullable = false)
	private ChatSession session;

	@Column(nullable = false)
	private String role; // "user", "assistant", "system"

	@Lob
	@Column(nullable = false)
	private String content;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	public ChatMessage() {
	}

	public ChatMessage(ChatSession session, String role, String content) {
		this.session = session;
		this.role = role;
		this.content = content;
	}

	// ── Getters & Setters ──
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public ChatSession getSession() {
		return session;
	}

	public void setSession(ChatSession session) {
		this.session = session;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

}
