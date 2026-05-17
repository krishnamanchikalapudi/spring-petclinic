
package org.springframework.samples.petclinic.ai.memory;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for ChatSession persistence.
 *
 * @author Krishna Manchikalapudi
 *
 * Repository for ChatSession persistence. - `findBySessionToken()` - Retrieve session by
 * UUID - `findAllRecent()` - Paginated retrieval of recent sessions -
 * `findRecentSessions(limit)` - Get N most recent sessions
 */
public interface ChatSessionRepository extends JpaRepository<ChatSession, Integer> {

	Optional<ChatSession> findBySessionToken(String sessionToken);

	@Query("SELECT s FROM ChatSession s ORDER BY s.updatedAt DESC")
	Page<ChatSession> findAllRecent(Pageable pageable);

	@Query("SELECT s FROM ChatSession s ORDER BY s.updatedAt DESC LIMIT :limit")
	List<ChatSession> findRecentSessions(@Param("limit") int limit);

}
