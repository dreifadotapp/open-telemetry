package dreifa.app.opentelemetry

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.TraceId

// the information passed between layers and into telemetry handlers

data class OpenTelemetryContext(
    val traceId: String, val spanId: String,

    /**
     * Optional - this is really to generate sensible spanKind when the component topology
     *            changes depending on the type of deployment, e.g, for an "all in one"
     *            style service  "INTERNAL" is logical, but in a production mode
     *            "ClIENT" and "SERVER" may make more sense.
     */
    val spanKind: SpanKind? = null
) {
    private constructor(span: Span) : this(span.spanContext.traceId, span.spanContext.spanId, null)
    private constructor(span: Span, kind: SpanKind) : this(span.spanContext.traceId, span.spanContext.spanId, kind)

    private constructor(spanContext: SpanContext) : this(spanContext.traceId, spanContext.spanId, null)
    private constructor(spanContext: SpanContext, kind: SpanKind) : this(spanContext.traceId, spanContext.spanId, kind)


    companion object {
        private val rootOpenTelemetryContext = OpenTelemetryContext(TraceId.getInvalid(), Span.getInvalid().toString())

        private val noOpenTelemetryContext =
            OpenTelemetryContext("zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz", "zzzzzzzzzzzzzzzz")

        fun root() = rootOpenTelemetryContext
        fun none() = noOpenTelemetryContext

        fun fromSpan(span: Span?, default: OpenTelemetryContext) =
            if (span != null) OpenTelemetryContext(span) else default

        fun fromSpan(span: Span) = OpenTelemetryContext(span)

        fun fromSpan(span: Span, kind: SpanKind) = OpenTelemetryContext(span, kind)

        fun fromSpanContext(spanContext: SpanContext) = OpenTelemetryContext(spanContext)

        fun fromSpanContext(spanContext: SpanContext, kind: SpanKind) = OpenTelemetryContext(spanContext, kind)


    }

    fun isRoot() = this == root()

    fun isNone() = this == none()

    fun isNested() = !isRoot() && !isNone()

    fun dto(): OpenTelemetryContextDTO = OpenTelemetryContextDTO(this)
}

/**
 * Something simple that is sure to serialise easily
 */
data class OpenTelemetryContextDTO(val traceId: String, val spanId: String, val kind: String? = null) {
    constructor(context: OpenTelemetryContext) : this(context.traceId, context.spanId, context.spanKind?.name)

    fun context(): OpenTelemetryContext = OpenTelemetryContext(traceId, spanId, kind?.let { SpanKind.valueOf(it) })
}
