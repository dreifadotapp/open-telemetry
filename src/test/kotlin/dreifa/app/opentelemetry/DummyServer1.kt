package dreifa.app.opentelemetry

import io.opentelemetry.api.trace.*
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext
import java.lang.RuntimeException
import kotlin.random.Random

class DummyServer1(
    private val tracer: Tracer,
    provider: OpenTelemetryProvider
) {

    private val helper = ContextHelper(provider)

    suspend fun exec(parent: OpenTelemetryContext, payload: String) {
        withContext(setParentContext(parent).asContextElement()) {

            val span = startSpan()
            Thread.sleep(Random.nextLong(10))
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
        return tracer.spanBuilder("DummyServer1")
            .setSpanKind(SpanKind.SERVER)
            .startSpan()
            .setAttribute("server.attr", "server1")
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

    private fun setParentContext(parent: OpenTelemetryContext): Context {
        return helper.createContext(parent)
    }
}