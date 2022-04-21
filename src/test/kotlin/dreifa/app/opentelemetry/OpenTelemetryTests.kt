package dreifa.app.opentelemetry

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dreifa.app.opentelemetry.presenters.TimelinePresenter
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenTelemetryTests {
    //private val registry = Registry()
    private lateinit var provider: ZipKinOpenTelemetryProvider

    init {
        //registry.store(taskFactory).store(logChannelFactory)
    }

    @BeforeEach
    fun `fresh provider`() {
        provider = ZipKinOpenTelemetryProvider()
    }

    @AfterAll
    fun `wait for zipkin 2`() {
        // give it time to flush to zipkin
        Thread.sleep(50)
    }


    @Test
    fun `should log something!`() {
        //provider.provider().getTracer("fgdhsjsd").spanBuilder("wibble").setSpanKind(SpanKind.CLIENT).startSpan().end()

        var tracer: Tracer = provider.provider().getTracer("dreifa.app.tasks.Tracer")

        val propogator = provider.provider().propagators.textMapPropagator
        println(propogator)
        val getter = Y()
        val c = Carrier(traceId ="00000000000000000000000000000001", spanId = "0000000000000001" )

        val xx: Context =  propogator
            .extract(Context.current(), c, getter)

        println(xx)
        xx.makeCurrent()

        val outerSpan: Span = tracer.spanBuilder("Client")
            .setSpanKind(SpanKind.CLIENT)
            //.setParent(Context.current().with(ImplicitContextKeyed {  }))
            .startSpan()


        val y = Y()
        val carrier = Carrier("000086bcaa3febcb0129ac0d6322edff", outerSpan.spanContext.spanId)

        val x = X()
        propogator.inject(Context.current(),carrier,x)

        val extractedContext: Context = propogator
            .extract(Context.current(), carrier, y)

        //val extracted =  extractedContext.makeCurrent()
        val current = Context.current()
        val currentWithSpan = Context.current().with(outerSpan)
        //println(extracted)

        try {
            outerSpan.makeCurrent()
            outerSpan.setAttribute(SemanticAttributes.HTTP_METHOD, "GET")
            propogator.inject(Context.current()/*.with(outerSpan)*/, carrier, X())


        } catch (e: Exception) {

        }

        val taskSpan: Span = tracer.spanBuilder("TaskName")
            .setSpanKind(SpanKind.SERVER)
            //.addLink(outerSpan.spanContext)
            // .setParent(Context.current().with(outerSpan))
            .setParent(extractedContext)
            .startSpan()

        taskSpan.setAttribute("attr.1", "foo")
        taskSpan.setAttribute("attr.2", "bar")
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
        println(presenter.present(provider.spans()))

        // give it time to make it to zipkin
        //Thread.sleep(50)

        val spansAnalyser = SimpleSpansAnalyser(provider.spans())
       // assertThat(spansAnalyser.traceIds().size, equalTo(1))
        assertThat(spansAnalyser.filterHasAttribute("attr.1").size, equalTo(1))

    }

    @Test
    fun `should trace client to server`() {
        val clientTracer = buildTracer("client")
        val serverTracer = buildTracer("tracer")

        val ctx = Context.current()
        println(ctx.javaClass.name)
        val s = clientTracer.spanBuilder("wibble").startSpan()

        val ctx2 = s.spanContext
        println(ctx2.javaClass.name)

        val server = DummyServer(serverTracer, provider)
        val client = DummyClient(clientTracer, provider, server)

        val traceId = UUID.randomUUID().toString().replace("-", "")

        client.exec(ParentContext.root, "foobar")

        val spansAnalyser = SimpleSpansAnalyser(provider.spans())

        spansAnalyser.traceIds().forEach {println(it)}
        //assertThat(spansAnalyser.traceIds(), equalTo(setOf(traceId)))

    }

    private fun buildTracer(scope: String): Tracer {
        return provider.provider().getTracer(scope)
    }
}
