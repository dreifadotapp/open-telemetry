package dreifa.app.opentelemetry

import io.opentelemetry.api.internal.ImmutableSpanContext
import io.opentelemetry.api.trace.*
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.context.propagation.TextMapSetter
import io.opentelemetry.sdk.trace.ReadableSpan


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

