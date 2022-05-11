package dreifa.app.opentelemetry


import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.data.SpanData


/**
 * A common interface for DI.
 */
interface OpenTelemetryProvider {
    fun sdk(): OpenTelemetrySdk

    fun spans() : ArrayList<SpanData> = ArrayList()
}





