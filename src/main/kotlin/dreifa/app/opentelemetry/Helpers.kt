package dreifa.app.opentelemetry

import dreifa.app.registry.Registry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

data class SpanDetails(
    val name: String,
    val kind: SpanKind = SpanKind.INTERNAL,
    val attributes: Attributes = Attributes.empty()
) {
   // constructor(name : String, kind : SpanKind = SpanKind.INTERNAL, correlation : CorrelationContexts = CorrelationContexts.empty()) : super(name, kind, correlation)
}

enum class ExceptionStrategy { recordAndThrow, throwOnly }

object Helpers {

    fun <T> runWithTelemetry(
        coroutineContext: CoroutineContext = kotlin.coroutines.EmptyCoroutineContext,
        tracer: Tracer? = null,
        provider: OpenTelemetryProvider? = null,
        telemetryContext: OpenTelemetryContext,
        spanDetails: SpanDetails,
        exceptionStrategy: ExceptionStrategy = ExceptionStrategy.recordAndThrow,
        block: () -> T
    ): T {
        return if (tracer != null && provider != null) {
            runBlocking(coroutineContext) {
                val helper = ContextHelper(provider)
                withContext(helper.createContext(telemetryContext).asContextElement()) {
                    val span = tracer!!.spanBuilder(spanDetails.name).setSpanKind(spanDetails.kind)
                        .setAllAttributes(spanDetails.attributes).startSpan()
                    try {
                        val result = block.invoke()
                        span.setStatus(StatusCode.OK).end()
                        result
                    } catch (ex: Exception) {
                        if (exceptionStrategy == ExceptionStrategy.recordAndThrow) {
                            span.recordException(ex)
                        }
                        span.setStatus(StatusCode.ERROR).end()
                        throw ex
                    }
                }
            }
        } else {
            block.invoke()
        }
    }

    fun <T> runWithTelemetry(
        coroutineContext: CoroutineContext = kotlin.coroutines.EmptyCoroutineContext,
        registry: Registry,
        telemetryContext: OpenTelemetryContext,
        spanDetails: SpanDetails,
        exceptionStrategy: ExceptionStrategy = ExceptionStrategy.recordAndThrow,
        block: () -> T
    ): T {
        return runWithTelemetry(
            coroutineContext,
            registry.getOrNull(Tracer::class.java),
            registry.getOrNull(OpenTelemetryProvider::class.java),
            telemetryContext,
            spanDetails,
            exceptionStrategy,
            block
        )
    }
}