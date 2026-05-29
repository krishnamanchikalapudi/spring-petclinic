package org.springframework.samples.petclinic.bootcamp.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Session 1 — AI Metrics Service
 * <p>
 * Records Micrometer metrics for AI inference operations.
 * Surfaces at {@code /actuator/prometheus} for Grafana (Session 3 / Chapter 20).
 *
 * <h3>Metrics exposed</h3>
 * <ul>
 *   <li>{@code ai.vet.chat.duration} — histogram of inference latency (ms)</li>
 *   <li>{@code ai.vet.chat.errors}   — counter by error type</li>
 *   <li>{@code ai.vet.chat.total}    — total call counter</li>
 * </ul>
 *
 * @author Anil Kumar Veldurthi
 * @author Krishna Manchikalapudi
 * @since Session 1
 */
@Service
public class AiMetricsService {

    private final MeterRegistry meterRegistry;

    public AiMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Records inference latency for a completed chat call.
     *
     * @param durationMs  wall-clock time from request start to response received
     * @param modelVersion model tag (e.g. {@code qwen3.5:0.8b})
     */
    public void recordChatDuration(long durationMs, String modelVersion) {
        Timer.builder("ai.vet.chat.duration")
             .description("VetAssistant inference latency")
             .tag("model", modelVersion)
             .register(meterRegistry)
             .record(durationMs, TimeUnit.MILLISECONDS);

        Counter.builder("ai.vet.chat.total")
               .description("Total VetAssistant chat calls")
               .tag("model", modelVersion)
               .register(meterRegistry)
               .increment();
    }

    /**
     * Records an error event with a machine-readable error type tag.
     *
     * @param errorType one of: {@code OLLAMA_UNAVAILABLE}, {@code INFERENCE_TIMEOUT},
     *                  {@code INTERNAL_ERROR}
     */
    public void recordError(String errorType) {
        Counter.builder("ai.vet.chat.errors")
               .description("VetAssistant chat errors by type")
               .tag("errorType", errorType)
               .register(meterRegistry)
               .increment();
    }
}
