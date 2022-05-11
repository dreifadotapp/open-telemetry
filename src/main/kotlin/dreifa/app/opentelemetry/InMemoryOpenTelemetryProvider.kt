package dreifa.app.opentelemetry

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.samplers.Sampler
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes

class InMemoryOpenTelemetryProvider() : OpenTelemetryProvider {
    private val inMemory = InMemorySpanExporter(null)

    private val sdk: OpenTelemetrySdk = OpenTelemetrySdk.builder()
        .setTracerProvider(
            SdkTracerProvider.builder()
                .setResource(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "dreifa")))
                .addSpanProcessor(SimpleSpanProcessor.create(inMemory))
                .setSampler(Sampler.alwaysOn())
                .build()
        )
        .setPropagators(MyPropagators())
        .build()


    override fun sdk() = sdk

    override fun spans() = inMemory.allSpans
}