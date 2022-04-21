package dreifa.app.opentelemetry

import io.opentelemetry.api.internal.ImmutableSpanContext
import io.opentelemetry.api.trace.*
import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import java.lang.RuntimeException
import java.util.*

class DummyServer(
    private val tracer: Tracer,
    private val provider: OpenTelemetryProvider,
    private val server: DummyServer? = null
    ) {

    val wibble = Wibble(provider)

    fun exec(parent: ParentContext, payload : String) {
        val ctx = setParentContext(parent)

        ctx.use {

            val span = startSpan(parent)
            //span.spanContext.
            try {
                if (payload.contains("server", true) &&
                    payload.contains("error", true)
                ) {
                    throw RuntimeException("Opps!")
                }
                //server.exec(buildParentContext(span), payload)
                completeSpan(span)
            } catch (ex: Exception) {
                completeSpan(span, ex)
            }
        }
    }


    private fun startSpan(parent : ParentContext): Span {



        //val z = Span.wrap(ctx)

        val ctx = wibble.createContext(parent.traceId,parent.spanId)

        //val x = SpanContext.createFromRemoteParent()

        //Context.current().with { ctx }


        val s = return tracer.spanBuilder("Server")
            .setSpanKind(SpanKind.SERVER)
            //
            .setParent(ctx)
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

    private fun setParentContext(parent: ParentContext) : Scope {
        // todo - should be doing something here
        return Context.root().makeCurrent()
    }

}