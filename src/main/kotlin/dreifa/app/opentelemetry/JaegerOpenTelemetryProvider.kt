package dreifa.app.opentelemetry

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import io.opentelemetry.sdk.trace.samplers.Sampler
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import java.util.concurrent.TimeUnit


class JaegerOpenTelemetryProvider(memoryCacheEnabled: Boolean = false) : OpenTelemetryProvider {
    private val endpoint = "http://localhost:14250"
    private val jaegerExporter: JaegerGrpcSpanExporter = JaegerGrpcSpanExporter.builder()
        .setEndpoint(endpoint)
        .setTimeout(30, TimeUnit.SECONDS)
        .build()

    private val inMemory: InMemorySpanExporter = if (memoryCacheEnabled) {
        InMemorySpanExporter(jaegerExporter)
    } else {
        InMemorySpanExporter(null)
    }
    private val exporter: SpanExporter = if (memoryCacheEnabled) inMemory else jaegerExporter
    private val propagators = MyPropagators()

    private val sdk: OpenTelemetrySdk = OpenTelemetrySdk.builder()
        .setTracerProvider(
            SdkTracerProvider.builder()
                .setResource(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "dreifa")))
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .setSampler(Sampler.alwaysOn())
                .build()
        )
        .setPropagators(propagators)
        .build()


    override fun sdk() = sdk

    override fun spans() = inMemory.allSpans
}