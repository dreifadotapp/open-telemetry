package dreifa.app.opentelemetry

import io.opentelemetry.context.Context

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