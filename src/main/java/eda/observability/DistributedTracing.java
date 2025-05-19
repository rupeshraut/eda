package eda.observability;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Distributed Tracing
 * <p>
 * Follows a transaction across multiple services and events.
 */
public class DistributedTracing {
    /**
     * The interface Tracing service.
     */
    public interface TracingService {
        /**
         * Start span span . builder.
         *
         * @param name the name
         * @return the span . builder
         */
        Span.Builder startSpan(String name);

        /**
         * Start span span . builder.
         *
         * @param name          the name
         * @param parentContext the parent context
         * @return the span . builder
         */
        Span.Builder startSpan(String name, DistributedTracing.TraceContext parentContext);

        /**
         * Report span.
         *
         * @param span the span
         */
        void reportSpan(Span span);
    }

    /**
     * The type Trace context.
     */
    public record TraceContext(
            String traceId,
            String spanId,
            String parentSpanId,
            Map<String, String> baggage
    ) {
        /**
         * Create new trace context.
         *
         * @return the trace context
         */
        public static TraceContext createNew() {
            return new TraceContext(
                    generateId(),
                    generateId(),
                    null,
                    new HashMap<>()
            );
        }

        private static String generateId() {
            return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        /**
         * Create child span trace context.
         *
         * @return the trace context
         */
        public TraceContext createChildSpan() {
            return new TraceContext(
                    this.traceId,
                    generateId(),
                    this.spanId,
                    new HashMap<>(this.baggage)
            );
        }
    }

    /**
     * The type Span.
     */
    public record Span(
            String name,
            DistributedTracing.TraceContext context,
            Instant startTime,
            Instant endTime,
            Map<String, String> tags,
            List<SpanEvent> events
    ) {
        /**
         * The type Builder.
         */
        public static class Builder {
            private final Instant startTime = Instant.now();
            private final Map<String, String> tags = new HashMap<>();
            private final List<SpanEvent> events = new ArrayList<>();
            private final String name;
            private final DistributedTracing.TraceContext context;
            private Instant endTime;

            /**
             * Instantiates a new Builder.
             *
             * @param name    the name
             * @param context the context
             */
            public Builder(String name, DistributedTracing.TraceContext context) {
                this.name = name;
                this.context = context;
            }

            /**
             * Add tag builder.
             *
             * @param key   the key
             * @param value the value
             * @return the builder
             */
            public Builder addTag(String key, String value) {
                tags.put(key, value);
                return this;
            }

            /**
             * Add event builder.
             *
             * @param name the name
             * @return the builder
             */
            public Builder addEvent(String name) {
                events.add(new SpanEvent(name, Instant.now(), Map.of()));
                return this;
            }

            /**
             * Add event builder.
             *
             * @param name       the name
             * @param attributes the attributes
             * @return the builder
             */
            public Builder addEvent(String name, Map<String, String> attributes) {
                events.add(new SpanEvent(name, Instant.now(), attributes));
                return this;
            }

            /**
             * End span.
             *
             * @return the span
             */
            public Span end() {
                this.endTime = Instant.now();
                return new Span(name, context, startTime, endTime, tags, events);
            }
        }
    }

    /**
     * The type Span event.
     */
    public record SpanEvent(String name, Instant timestamp, Map<String, String> attributes) {
    }

    /**
     * The type Simple tracing service.
     */
    public static class SimpleTracingService implements TracingService {
        @Override
        public Span.Builder startSpan(String name) {
            return new Span.Builder(name, TraceContext.createNew());
        }

        @Override
        public Span.Builder startSpan(String name, TraceContext parentContext) {
            return new Span.Builder(name, parentContext.createChildSpan());
        }

        @Override
        public void reportSpan(Span span) {
            // In a real system, send to tracing backends like Jaeger, Zipkin, etc.
            System.out.println("TRACE: " + span.name() +
                    " [" + span.context().traceId() + ":" + span.context().spanId() + "] " +
                    "Duration: " + Duration.between(span.startTime(), span.endTime()).toMillis() + "ms");

            for (var event : span.events()) {
                System.out.println("  |- Event: " + event.name() + " at " + event.timestamp());
            }
        }
    }
}