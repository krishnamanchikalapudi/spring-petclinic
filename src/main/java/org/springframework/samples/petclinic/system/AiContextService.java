package org.springframework.samples.petclinic.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.Visit;
import org.springframework.samples.petclinic.vet.Specialty;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.stereotype.Service;

/**
 * @author Krishna Manchikalapudi
 *
 * Queries the PetClinic database and assembles a concise, relevant context block that is
 * injected into each Ollama system prompt, allowing the AI to answer questions about real
 * clinic data.
 *
 * <h3>Context strategy (intent-based)</h3>
 * <ul>
 * <li>Vet / specialty keywords → full vet roster with specialties</li>
 * <li>Owner / client keywords → owner list (up to 30) with pets</li>
 * <li>Pet keywords → pet &amp; type breakdown</li>
 * <li>Visit keywords → recent visits across all pets</li>
 * <li>Count / how-many keywords → aggregate statistics</li>
 * <li>Name detected in query → targeted owner search by last name</li>
 * <li>Everything else → summary stats only (lightweight fallback)</li>
 * </ul>
 */
@Service
public class AiContextService {

	// ── Tunables ──────────────────────────────────────────────────────────────
	/** Max owners fetched for listing queries. */
	private static final int MAX_OWNERS = 30;

	/** Max recent visits included for visit-intent queries. */
	private static final int MAX_VISITS = 15;

	/** Max owners returned for a name-search. */
	private static final int MAX_NAME_SEARCH = 10;

	// ── Intent keyword sets ───────────────────────────────────────────────────
	private static final String[] VET_KEYWORDS = { "vet", "doctor", "dr.", "specialist", "specialty", "specialties",
			"surgeon", "surgery", "staff", "veterinarian" };

	private static final String[] OWNER_KEYWORDS = { "owner", "client", "customer", "person", "people", "who owns",
			"family", "contact", "telephone", "address", "city" };

	private static final String[] PET_KEYWORDS = { "pet", "animal", "cat", "dog", "bird", "hamster", "lizard", "snake",
			"species", "type", "breed" };

	private static final String[] VISIT_KEYWORDS = { "visit", "appointment", "consultation", "treatment", "history",
			"last visit", "recent", "when did", "schedule" };

	private static final String[] COUNT_KEYWORDS = { "how many", "count", "total", "number of", "statistics", "stats",
			"summary", "overview" };

	// ── Private static record (promoted from local to avoid tooling issues) ──
	private record VisitRow(String date, String desc, String petName, String ownerName) {
	}

	// ── Dependencies ──────────────────────────────────────────────────────────
	private final OwnerRepository ownerRepository;

	private final VetRepository vetRepository;

	public AiContextService(OwnerRepository ownerRepository, VetRepository vetRepository) {
		this.ownerRepository = ownerRepository;
		this.vetRepository = vetRepository;
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Public API
	// ─────────────────────────────────────────────────────────────────────────

	/**
	 * Builds a context string tailored to the user's latest query. The returned string is
	 * safe to embed directly into a system prompt.
	 * @param userQuery the latest user message text
	 * @return a newline-delimited context block, or an empty string if an error occurs
	 * while reading the database
	 */
	public String buildContext(String userQuery) {
		try {
			String q = userQuery == null ? "" : userQuery.toLowerCase(Locale.ROOT);
			StringBuilder sb = new StringBuilder();
			sb.append("\n\n--- LIVE CLINIC DATABASE (read-only snapshot) ---\n");

			// 1. Always include summary stats
			sb.append(buildStats());

			// 2. Intent-specific sections
			boolean hasVetIntent = containsAny(q, VET_KEYWORDS);
			boolean hasOwnerIntent = containsAny(q, OWNER_KEYWORDS);
			boolean hasPetIntent = containsAny(q, PET_KEYWORDS);
			boolean hasVisitIntent = containsAny(q, VISIT_KEYWORDS);
			boolean hasCountIntent = containsAny(q, COUNT_KEYWORDS);

			if (hasVetIntent || hasCountIntent) {
				sb.append(buildVetSection());
			}

			if (hasOwnerIntent || hasPetIntent || hasCountIntent) {
				sb.append(buildOwnerSection(MAX_OWNERS, hasPetIntent, false));
			}

			if (hasVisitIntent) {
				sb.append(buildRecentVisitsSection());
			}

			// 3. Name-based targeted search (highest specificity)
			String detectedName = extractPossibleName(userQuery); // original case —
																	// isUpperCase needs
																	// it
			if (detectedName != null) {
				sb.append(buildNameSearchSection(detectedName));
			}

			// 4. Fallback: if no intent matched, show vets so the response is
			// still grounded in real data rather than hallucinated.
			if (!hasVetIntent && !hasOwnerIntent && !hasPetIntent && !hasVisitIntent && !hasCountIntent
					&& detectedName == null) {
				sb.append(buildVetSection());
			}

			sb.append("--- END OF DATABASE SNAPSHOT ---\n");
			return sb.toString();

		}
		catch (Exception ex) {
			// Never let a DB error break the chat entirely
			return "\n[Note: live database context unavailable — " + ex.getMessage() + "]\n";
		}
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Context builders
	// ─────────────────────────────────────────────────────────────────────────

	/** Aggregate statistics line. */
	private String buildStats() {
		long ownerCount = ownerRepository.findByLastNameStartingWith("", PageRequest.of(0, 1)).getTotalElements();

		// VetRepository.findAll() returns Collection<Vet> in the canonical petclinic
		List<Vet> vets = new ArrayList<>(vetRepository.findAll());
		long vetCount = vets.size();

		long petCount = ownerRepository.findByLastNameStartingWith("", PageRequest.of(0, (int) Math.max(ownerCount, 1)))
			.getContent()
			.stream()
			.mapToLong(o -> o.getPets().size())
			.sum();

		return String.format("SUMMARY: %d owner(s), %d pet(s), %d veterinarian(s) on record.%n", ownerCount, petCount,
				vetCount);
	}

	/** Full vet roster with specialties. */
	private String buildVetSection() {
		List<Vet> vets = new ArrayList<>(vetRepository.findAll());
		if (vets.isEmpty()) {
			return "VETERINARIANS: none on record.\n";
		}
		StringBuilder sb = new StringBuilder("VETERINARIANS:\n");
		for (Vet v : vets) {
			List<String> specNames = new ArrayList<>();
			for (Specialty s : v.getSpecialties()) {
				specNames.add(s.getName());
			}
			String specs = specNames.isEmpty() ? "general practice" : String.join(", ", specNames);
			sb.append(String.format("  - %s %s | specialties: %s%n", v.getFirstName(), v.getLastName(), specs));
		}
		return sb.toString();
	}

	/** Owner list with optional pet detail. */
	private String buildOwnerSection(int limit, boolean includePets, boolean includeVisits) {
		Page<Owner> page = ownerRepository.findByLastNameStartingWith("", PageRequest.of(0, limit));
		List<Owner> owners = page.getContent();
		long total = page.getTotalElements();

		if (owners.isEmpty()) {
			return "OWNERS: none on record.\n";
		}

		StringBuilder sb = new StringBuilder();
		sb.append(String.format("OWNERS (%d shown of %d total):%n", owners.size(), total));

		for (Owner o : owners) {
			sb.append(String.format("  - %s %s | %s, %s | tel: %s%n", o.getFirstName(), o.getLastName(), o.getAddress(),
					o.getCity(), o.getTelephone()));

			if (includePets) {
				for (Pet pet : o.getPets()) {
					String typeName = pet.getType() != null ? pet.getType().getName() : "unknown";
					sb.append(String.format("      pet: %s (%s, born %s)%n", pet.getName(), typeName,
							pet.getBirthDate()));

					if (includeVisits) {
						for (Visit vis : pet.getVisits()) {
							sb.append(String.format("        visit %s: %s%n", vis.getDate(), vis.getDescription()));
						}
					}
				}
			}
		}
		return sb.toString();
	}

	/** Recent visits across all owners, newest first, capped at MAX_VISITS. */
	private String buildRecentVisitsSection() {
		Page<Owner> page = ownerRepository.findByLastNameStartingWith("", PageRequest.of(0, MAX_OWNERS));
		List<Owner> owners = page.getContent();

		List<VisitRow> rows = new ArrayList<>();

		for (Owner o : owners) {
			String ownerName = o.getFirstName() + " " + o.getLastName();
			for (Pet pet : o.getPets()) {
				for (Visit v : pet.getVisits()) {
					rows.add(new VisitRow(v.getDate() != null ? v.getDate().toString() : "?", v.getDescription(),
							pet.getName(), ownerName));
				}
			}
		}

		// Sort descending by date string (ISO format sorts lexicographically)
		rows.sort((a, b) -> b.date().compareTo(a.date()));

		if (rows.isEmpty()) {
			return "RECENT VISITS: none on record.\n";
		}

		StringBuilder sb = new StringBuilder(
				String.format("RECENT VISITS (latest %d):%n", Math.min(rows.size(), MAX_VISITS)));
		rows.stream()
			.limit(MAX_VISITS)
			.forEach(r -> sb.append(String.format("  - %s | pet: %s (owner: %s) | %s%n", r.date(), r.petName(),
					r.ownerName(), r.desc())));

		return sb.toString();
	}

	/** Targeted owner search by last name. */
	private String buildNameSearchSection(String lastName) {
		Page<Owner> page = ownerRepository.findByLastNameStartingWith(lastName, PageRequest.of(0, MAX_NAME_SEARCH));
		List<Owner> owners = page.getContent();

		if (owners.isEmpty()) {
			return String.format("SEARCH for \"%s\": no matching owners found.%n", lastName);
		}

		StringBuilder sb = new StringBuilder(
				String.format("SEARCH for \"%s\" (%d result(s)):%n", lastName, owners.size()));
		for (Owner o : owners) {
			sb.append(String.format("  - %s %s | %s, %s | tel: %s%n", o.getFirstName(), o.getLastName(), o.getAddress(),
					o.getCity(), o.getTelephone()));
			for (Pet pet : o.getPets()) {
				String typeName = pet.getType() != null ? pet.getType().getName() : "unknown";
				sb.append(String.format("      pet: %s (%s)%n", pet.getName(), typeName));
				for (Visit v : pet.getVisits()) {
					sb.append(String.format("        visit %s: %s%n", v.getDate(), v.getDescription()));
				}
			}
		}
		return sb.toString();
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Helpers
	// ─────────────────────────────────────────────────────────────────────────

	private boolean containsAny(String text, String[] keywords) {
		for (String k : keywords) {
			if (text.contains(k))
				return true;
		}
		return false;
	}

	/**
	 * Heuristic: if the query contains a capitalised word that looks like a surname (≥3
	 * chars, not a known stop-word), treat it as a search term.
	 *
	 * Examples that match: "find owner Smith" / "is Davis registered?" Examples that
	 * don't: "how many owners?" / "List all vets"
	 */
	private String extractPossibleName(String rawQuery) {
		if (rawQuery == null || rawQuery.isBlank())
			return null;

		// Work on the original-case version
		String[] words = rawQuery.trim().split("\\s+");
		// Stop-words that are capitalised but are NOT names
		java.util.Set<String> stopWords = java.util.Set.of("how", "what", "when", "where", "who", "which", "why", "the",
				"a", "an", "is", "are", "was", "were", "be", "been", "have", "has", "had", "do", "does", "did", "will",
				"would", "can", "could", "should", "may", "might", "shall", "find", "list", "show", "get", "tell",
				"give", "add", "owner", "pet", "vet", "visit", "clinic", "doctor", "i", "me", "my", "we", "our", "you",
				"your", "it", "this", "that", "these", "those", "all", "any", "some", "please", "help", "need", "want",
				"petclinic", "spring", "java");

		for (String word : words) {
			// Strip punctuation
			String clean = word.replaceAll("[^a-zA-Z]", "");
			if (clean.length() >= 3 && Character.isUpperCase(clean.charAt(0))
					&& !stopWords.contains(clean.toLowerCase(Locale.ROOT))) {
				return clean;
			}
		}
		return null;
	}

}
