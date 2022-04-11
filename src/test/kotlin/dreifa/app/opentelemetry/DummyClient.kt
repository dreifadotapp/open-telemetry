package dreifa.app.opentelemetry

import io.opentelemetry.api.internal.ImmutableSpanContext
import io.opentelemetry.api.trace.*
import java.lang.RuntimeException
import java.util.*

/**
 * Emulate the client side
 */
class DummyClient(
    private val tracer: Tracer,
    private val server: DummyServer
) {

    fun exec(traceId: UUID, payload: String) {
        val span = startSpan()
        val ctx = setTraceId(span.spanContext, traceId)
        try {
            if (payload.contains("client", true) &&
                payload.contains("error", true)
            ) {
                throw RuntimeException("Opps!")
            }
            server.exec(traceId, payload)
            completeSpan(span)
        } catch (ex: Exception) {
            completeSpan(span, ex)
        }
    }

    private fun startSpan(): Span {

        return tracer.spanBuilder("Client")
            .setSpanKind(SpanKind.CLIENT)
            //.setParent(Context.current().with(ImplicitContextKeyed {  }))
            .startSpan()
            .setAttribute("client.attr", "foo")

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

    private fun setTraceId(spanContext: SpanContext, traceId: UUID): SpanContext {
        return ImmutableSpanContext.create(
            traceId.toString().replace("-", ""),
            spanContext.spanId,
            spanContext.traceFlags,
            spanContext.traceState,
            false,
            false
        )
    }


}