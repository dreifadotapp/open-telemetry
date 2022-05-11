package dreifa.app.opentelemetry


import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.data.SpanData

/**
 * A common interface for DI.
 */
interface OpenTelemetryProvider {

    /**
     * returns a correctly configured OpenTelemetrySdk
     */
    fun sdk(): OpenTelemetrySdk


    /**
     * returns a raw, unfiltered copy of the telemetry, if available
     * This feature is really only intended for use in test cases
     */
    fun spans(): ArrayList<SpanData> = ArrayList()
}





