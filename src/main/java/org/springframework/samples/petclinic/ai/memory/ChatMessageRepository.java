package org.springframework.samples.petclinic.ai.memory;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for ChatMessage persistence.
 *
 * @author Krishna Manchikalapudi
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {

	@Query("SELECT m FROM ChatMessage m WHERE m.session.id = :sessionId ORDER BY m.createdAt ASC")
	List<ChatMessage> findBySessionId(@Param("sessionId") Integer sessionId);

}
