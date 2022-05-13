package dreifa.app.opentelemetry

import io.opentelemetry.api.internal.ImmutableSpanContext
import io.opentelemetry.api.trace.*
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.context.propagation.TextMapSetter
import io.opentelemetry.sdk.trace.ReadableSpan

// the information passed between layers and into telemetry handlers

data class OpenTelemetryContext(
    val traceId: String,
    val spanId: String,

    /**
     * Optional - really to generate sensible spanKind when the component topology
     *            changes depending on the type of deployment, e.g, for an "all in one"
     *            style service  "INTERNAL" is logical, but in a production mode
     *            "ClIENT" and "SERVER" may make more sense.
     */
    val spanKind: SpanKind? = null
) {
    private constructor(span: Span) : this(span.spanContext.traceId, span.spanContext.spanId, extractSpanKind(span))
    private constructor(spanContext: SpanContext) : this(spanContext.traceId, spanContext.spanId, null)

    companion object {
        private val rootOpenTelemetryContext = OpenTelemetryContext(TraceId.getInvalid(), Span.getInvalid().toString())
        fun root() = rootOpenTelemetryContext
        fun fromSpan(span: Span): OpenTelemetryContext = OpenTelemetryContext(span)
        fun fromSpanContext(spanContext: SpanContext): OpenTelemetryContext = OpenTelemetryContext(spanContext)

        private fun extractSpanKind(span: Span): SpanKind? {
            return if (span is ReadableSpan) {
                span.kind
            } else {
                null
            }
        }
    }

    fun isRoot() = this == root()

    fun isNested() = !isRoot()

    fun dto(): OpenTelemetryContextDTO = OpenTelemetryContextDTO(this)
}

/**
 * Something simple that is sure to serialise easily
 */
data class OpenTelemetryContextDTO(val traceId: String, val spanId: String, val kind: String? = null) {
    constructor(context: OpenTelemetryContext) : this(context.traceId, context.spanId, context.spanKind?.name)

    fun context(): OpenTelemetryContext = OpenTelemetryContext(traceId, spanId, kind?.let {  SpanKind.valueOf(it) })
}


class NoopTextMapGetter : TextMapGetter<OpenTelemetryContext> {
    override fun keys(carrier: OpenTelemetryContext): Iterable<String> = emptyList()
    override fun get(carrier: OpenTelemetryContext?, key: String): String? = null
}

class MyPropagators : ContextPropagators {
    override fun getTextMapPropagator(): TextMapPropagator = MyTextMapPropagator()
}

class MyTextMapPropagator : TextMapPropagator {

    override fun fields(): Collection<String> {
        return emptyList()
    }

    override fun <C : Any?> inject(context: Context, carrier: C?, setter: TextMapSetter<C>) {
        // noop
    }

    override fun <C : Any?> extract(context: Context, carrier: C?, getter: TextMapGetter<C>): Context {
        return if (carrier is OpenTelemetryContext) {
            val ctx = ImmutableSpanContext.create(
                carrier.traceId,
                carrier.spanId,
                TraceFlags.getDefault(),
                TraceState.getDefault(),
                true,
                false
            )

            val propagatedSpan = Span.wrap(ctx)
            context.with(propagatedSpan)
        } else {
            Context.current()
        }
    }
}


class ContextHelper(private val p: OpenTelemetryProvider) {
    private fun createContext(traceId: String, spanId: String): Context {
        val propagator = p.sdk().propagators.textMapPropagator
        val c = OpenTelemetryContext(traceId = traceId, spanId = spanId)
        return propagator.extract(Context.current(), c, NoopTextMapGetter())
    }

    fun createContext(parent: OpenTelemetryContext): Context {
        return createContext(parent.traceId, parent.spanId)
    }

    fun createContext(parent: OpenTelemetryContextDTO): Context {
        return createContext(parent.context())
    }
}