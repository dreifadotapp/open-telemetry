package dreifa.app.opentelemetry

import io.opentelemetry.api.internal.ImmutableSpanContext
import io.opentelemetry.api.trace.*
import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import java.lang.RuntimeException

/**
 * Emulate the client side
 */
class DummyClient(
    private val tracer: Tracer,
    private val provider: OpenTelemetryProvider,
    private val server: DummyServer
) {

    val wibble = Wibble(provider)

    fun exec(parent : ParentContext, payload: String) {
        val ctx = createInitialContext(parent)

        ctx.use {

            val span = startSpan()
            //span.spanContext.
            try {
                if (payload.contains("client", true) &&
                    payload.contains("error", true)
                ) {
                    throw RuntimeException("Opps!")
                }
                server.exec(buildParentContext(span), payload)
                completeSpan(span)
            } catch (ex: Exception) {
                completeSpan(span, ex)
            }
        }
    }

    private fun buildParentContext(span : Span) : ParentContext {
        return ParentContext(span.spanContext.traceId, span.spanContext.spanId)
    }

    private fun startSpan(): Span {

        val s = return tracer.spanBuilder("Client")
            .setSpanKind(SpanKind.CLIENT)
            //.setParent(ctx)
            //.setNoParent()
            .startSpan()
            .setAttribute("client.attr", "foo")


        return s
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

    private fun setTraceId(spanContext: SpanContext, traceId: String): SpanContext {
        return ImmutableSpanContext.create(
            traceId,
            spanContext.spanId,
            spanContext.traceFlags,
            spanContext.traceState,
            false,
            false
        )
    }

    private fun createInitialContext(ctx : ParentContext) : Scope {
        // todo - should be doing something here
       return Context.root().makeCurrent()
    }


}