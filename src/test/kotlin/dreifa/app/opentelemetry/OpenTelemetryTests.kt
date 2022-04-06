package dreifa.app.opentelemetry

import dreifa.app.opentelemetry.presenters.TimelinePresenter
import dreifa.app.registry.Registry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import org.junit.jupiter.api.Test

class OpenTelemetryTests
{
    //private val registry = Registry()
    private val provider = ZipKinOpenTelemetryProvider()

    init {
        //registry.store(taskFactory).store(logChannelFactory)
    }

    @Test
    fun `should log something!`() {

        //provider.provider().getTracer("fgdhsjsd").spanBuilder("wibble").setSpanKind(SpanKind.CLIENT).startSpan().end()


        var tracer: Tracer = provider.provider().getTracer("dreifa.app.tasks.Tracer")

        val outerSpan: Span = tracer.spanBuilder("Client").setSpanKind(SpanKind.CLIENT).setAttribute("wibble.name","foobar").startSpan()

        val taskSpan: Span = tracer.spanBuilder("TaskName")
            .setSpanKind(SpanKind.SERVER)
            //.addLink(outerSpan.spanContext)
            .setParent(Context.current().with(outerSpan))
            .startSpan()

        taskSpan.setAttribute("Attr 1", "first attribute value")
        taskSpan.setAttribute("Attr 2", "second attribute value")
        taskSpan.addEvent("something happened!! ")
        Thread.sleep(2)
        taskSpan.end()
        Thread.sleep(1)
        outerSpan.end()

        provider.spans().forEach {
            println("A span")
            //println(it)
            println(it.parentSpanContext)
            println(it.name)
            println(it.startEpochNanos)
            println(it.endEpochNanos - it.startEpochNanos)
            println(it.kind)

        }

        val presenter = TimelinePresenter()
        println(presenter.present( provider.spans()))

        // give it time to make it to zipkin
        Thread.sleep(50)

    }

}