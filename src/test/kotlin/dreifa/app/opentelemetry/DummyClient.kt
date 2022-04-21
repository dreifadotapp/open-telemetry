package dreifa.app.opentelemetry

import io.opentelemetry.api.trace.*
import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.lang.RuntimeException
import java.util.concurrent.Executors

/**
 * Emulate the client side
 */
class DummyClient(
    private val tracer: Tracer,
    private val server1: DummyServer1,
    private val server2: DummyServer2

) {

    fun exec(payload: String) {
        val ctx = createInitialContext()

        ctx.use {

            val span = startSpan()
            try {
                if (payload.contains("client", true) &&
                    payload.contains("error", true)
                ) {
                    throw RuntimeException("Opps!")
                }
                val parentContext = buildParentContext(span)

               // runBlocking {
               //     val job1 = launch(Dispatchers.Default) {
                        server1.exec(parentContext, payload)
               //     }
               //     val job2 = launch(Dispatchers.Default) {
                        server2.exec(parentContext, payload)
               //     }
               // }


                //server2.exec(parentContext, payload)
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


    private fun createInitialContext(): Scope {
        // todo - should be doing something here
        return Context.root().makeCurrent()
    }


    suspend fun differentThread() = withContext(Dispatchers.Default){
        println("Different thread")
    }


}