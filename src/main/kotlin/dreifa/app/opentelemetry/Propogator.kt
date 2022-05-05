package dreifa.app.opentelemetry

import io.opentelemetry.api.internal.ImmutableSpanContext
import io.opentelemetry.api.trace.*
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.context.propagation.TextMapSetter

// the information passed between layers

data class OpenTelemetryContext(val traceId: String, val spanId: String) {
    private constructor(span: Span) : this(span.spanContext.traceId, span.spanContext.spanId)
    private constructor(spanContext: SpanContext) : this(spanContext.traceId, spanContext.spanId)

    companion object {
        private val rootOpenTelemetryContext = OpenTelemetryContext(TraceId.getInvalid(), Span.getInvalid().toString())
        fun root() = rootOpenTelemetryContext
        fun fromSpan(span: Span): OpenTelemetryContext = OpenTelemetryContext(span)
        fun fromSpanContext(spanContext: SpanContext): OpenTelemetryContext = OpenTelemetryContext(spanContext)
    }

    fun isRoot() = this == root()

    fun isNested() = !isRoot()

    fun dto () : OpenTelemetryContextDTO = OpenTelemetryContextDTO(this)
}

/**
 * Something simple that is sure to serialise easily
 */
data class OpenTelemetryContextDTO(val traceId: String, val spanId: String) {
    constructor(context: OpenTelemetryContext) : this(context.traceId, context.spanId)

    fun context(): OpenTelemetryContext = OpenTelemetryContext(traceId, spanId)
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