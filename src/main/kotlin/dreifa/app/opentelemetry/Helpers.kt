package dreifa.app.opentelemetry

import dreifa.app.registry.Registry
import dreifa.app.types.CorrelationContext
import dreifa.app.types.CorrelationContexts
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

data class SpanDetails(
    val name: String, val kind: SpanKind, val attributes: Attributes = Attributes.empty()
) {

    constructor(
        name: String, kind: SpanKind
    ) : this(name, kind, Attributes.empty())

    constructor(
        name: String, kind: SpanKind, correlations: CorrelationContexts = CorrelationContexts.empty()
    ) : this(name, kind, AttributesHelper.fromCorrelations(correlations))

    constructor(
        name: String, kind: SpanKind, correlation: CorrelationContext
    ) : this(name, kind, AttributesHelper.fromCorrelation(correlation))
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
        block: (span: Span?) -> T
    ): T {
        return if (tracer != null && provider != null) {
            runBlocking(coroutineContext) {
                val helper = ContextHelper(provider)
                withContext(helper.createContext(telemetryContext).asContextElement()) {
                    val span = tracer.spanBuilder(spanDetails.name).setSpanKind(spanDetails.kind)
                        .setAllAttributes(spanDetails.attributes).startSpan()
                    try {
                        val result = block.invoke(span)
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
            block.invoke(null)
        }
    }

    fun <T> runWithCurrentTelemetry(
        coroutineContext: CoroutineContext = kotlin.coroutines.EmptyCoroutineContext,
        tracer: Tracer? = null,
        provider: OpenTelemetryProvider? = null,
        spanDetails: SpanDetails,
        exceptionStrategy: ExceptionStrategy = ExceptionStrategy.recordAndThrow,
        block: (span : Span?) -> T
    ): T {
        return if (tracer != null && provider != null) {
            runBlocking(coroutineContext) {
                withContext(Context.current().asContextElement()) {
                    val span = tracer.spanBuilder(spanDetails.name).setSpanKind(spanDetails.kind)
                        .setAllAttributes(spanDetails.attributes).startSpan()
                    try {
                        val result = block.invoke(span)
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
            block.invoke(null)
        }
    }

    fun <T> runWithTelemetry(
        coroutineContext: CoroutineContext = kotlin.coroutines.EmptyCoroutineContext,
        registry: Registry,
        telemetryContext: OpenTelemetryContext,
        spanDetails: SpanDetails,
        exceptionStrategy: ExceptionStrategy = ExceptionStrategy.recordAndThrow,
        block: (span : Span?) -> T
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