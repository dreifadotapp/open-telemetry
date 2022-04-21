package dreifa.app.opentelemetry

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.samplers.Sampler
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import java.lang.String


interface OpenTelemetryProvider {
    fun provider(): OpenTelemetrySdk
}

class InMemoryOpenTelemetryProvider() : OpenTelemetryProvider {
    private val inMemory = InMemorySpanExporter()

    private val sdk: OpenTelemetrySdk = OpenTelemetrySdk.builder()
        .setTracerProvider(
            SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(inMemory))
                .build()
        )
        .build()

    override fun provider() = sdk

    fun spans() = inMemory.allSpans
}

class ZipKinOpenTelemetryProvider() : OpenTelemetryProvider {
    private val endpoint = String.format("http://%s:%s/api/v2/spans", "localhost", 9411)
    private val zipkinExporter = ZipkinSpanExporter.builder().setEndpoint(endpoint).build()
    private val inMemory = InMemorySpanExporter(zipkinExporter)
    private val propagators = MyPropagators()
    private val sdk: OpenTelemetrySdk = OpenTelemetrySdk.builder()
        .setTracerProvider(
            SdkTracerProvider.builder()
                .setResource(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "dreifa")))
                .addSpanProcessor(SimpleSpanProcessor.create(inMemory))
                .setSampler(Sampler.alwaysOn())
                .build()
        )
        .setPropagators(propagators)
        .build()


    override fun provider() = sdk

    fun spans() = inMemory.allSpans
}


