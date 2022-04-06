package dreifa.app.opentelemetry

import dreifa.app.opentelemetry.presenters.TimelinePresenter
import dreifa.app.registry.Registry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.Tracer
import org.junit.jupiter.api.Test

class OpenTelemetryTests
{
    private val registry = Registry()
    private val provider = InMemoryOpenTelemetryProvider()

    init {
        //registry.store(taskFactory).store(logChannelFactory)
    }

    @Test
    fun `should log something!`() {
//        val outOut = System.err
//        val x = ByteArrayOutputStream()
//        System.setErr(PrintStream( x))


        provider.provider().getTracer("fgdhsjsd").spanBuilder("Client").setSpanKind(SpanKind.CLIENT).startSpan().end()

        var tracer: Tracer = provider.provider().getTracer("dreifa.app.tasks.Tracer")

        val outerSpan: Span = tracer.spanBuilder("Client").setSpanKind(SpanKind.CLIENT).startSpan()

        val taskSpan: Span = tracer.spanBuilder("TaskName")
            .setSpanKind(SpanKind.SERVER)
            .addLink(outerSpan.spanContext)
            .startSpan()

        taskSpan.setAttribute("Attr 1", "first attribute value")
        taskSpan.setAttribute("Att2 2", "second attribute value")
        taskSpan.addEvent("something happened!! ")
        //Thread.sleep(1)

        taskSpan.end()
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


    }

}