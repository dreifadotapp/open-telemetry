package dreifa.app.opentelemetry

import io.opentelemetry.api.trace.*
import io.opentelemetry.context.Scope
import java.lang.RuntimeException

class DummyServer2(
    private val tracer: Tracer,
    provider: OpenTelemetryProvider
) {

    private val helper = ContextHelper(provider)

    fun exec(parent: ParentContext, payload: String) {
        setParentContext(parent).use {
            val span = startSpan()
            try {
                if (payload.contains("server", true) &&
                    payload.contains("error", true)
                ) {
                    throw RuntimeException("Opps!")
                }
                completeSpan(span)
            } catch (ex: Exception) {
                completeSpan(span, ex)
            }
        }
    }

    private fun startSpan(): Span {
        return tracer.spanBuilder("DummyServer2")
            .setSpanKind(SpanKind.SERVER)
            .startSpan()
            .setAttribute("server.attr", "foo")

    }

    private fun completeSpan(span: Span) {
        span.setStatus(StatusCode.OK)
        span.end()
    }

    private fun completeSpan(span: Span, ex: Exception) {
        span.recordException(ex)
        span.setStatus(StatusCode.ERROR)
        span.end()
    }

    private fun setParentContext(parent: ParentContext): Scope {
        return helper.createContext(parent).makeCurrent()
    }
}