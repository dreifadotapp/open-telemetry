package dreifa.app.opentelemetry

import io.opentelemetry.api.trace.*
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.lang.RuntimeException

/**
 * Emulate the client side
 */
class DummyClient(
    private val tracer: Tracer,
    private val server1: DummyServer1,
    private val server2: DummyServer2
) {

    suspend fun exec(payload: String) {
        withContext(Context.current().asContextElement()) {

            val span = startSpan()
            try {
                if (payload.contains("client", true) &&
                    payload.contains("error", true)
                ) {
                    throw RuntimeException("Opps!")
                }
                val parentContext = buildParentContext(span)

                runBlocking {
                    server1.exec(parentContext, payload)
                    server2.exec(parentContext, payload)
                }

                completeSpan(span)
            } catch (ex: Exception) {
                completeSpan(span, ex)
            }
        }
    }

    private fun buildParentContext(span: Span): ParentContext {
        return ParentContext(span.spanContext.traceId, span.spanContext.spanId)
    }

    private fun startSpan(): Span {
        return tracer.spanBuilder("DummyClient")
            .setSpanKind(SpanKind.CLIENT)
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

}