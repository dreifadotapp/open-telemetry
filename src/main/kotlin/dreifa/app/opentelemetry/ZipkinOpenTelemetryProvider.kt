package dreifa.app.opentelemetry

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import io.opentelemetry.sdk.trace.samplers.Sampler
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes

class ZipkinOpenTelemetryProvider(
    memoryCacheEnabled: Boolean = false,
    serviceName: String = "app.dreifa"
) : OpenTelemetryProvider {
    private val endpoint = String.format("http://%s:%s/api/v2/spans", "localhost", 9411)
    private val zipkinExporter = ZipkinSpanExporter.builder().setEndpoint(endpoint).build()

    private val inMemory: InMemorySpanExporter = if (memoryCacheEnabled) {
        InMemorySpanExporter(zipkinExporter)
    } else {
        InMemorySpanExporter(null)
    }
    private val exporter: SpanExporter = if (memoryCacheEnabled) inMemory else zipkinExporter
    private val propagators = MyPropagators()

    private val sdk: OpenTelemetrySdk = OpenTelemetrySdk.builder()
        .setTracerProvider(
            SdkTracerProvider.builder()
                .setResource(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, serviceName)))
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .setSampler(Sampler.alwaysOn())
                .build()
        )
        .setPropagators(propagators)
        .build()


    override fun sdk() = sdk

    override fun spans(): ArrayList<SpanData> = inMemory.allSpans
}
