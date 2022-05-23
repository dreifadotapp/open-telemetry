package dreifa.app.opentelemetry

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.trace.data.StatusData
import org.junit.jupiter.api.*
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HelpersTests {
    private lateinit var provider: OpenTelemetryProvider
    private lateinit var tracer: Tracer

    @BeforeEach
    fun `fresh provider`() {
        provider = JaegerOpenTelemetryProvider(true)
        tracer = provider.sdk().getTracer("dreifa.app.opentelemetry.HelpersTests")
    }


    @AfterAll
    fun `wait to flush telemetry`() {
        // give it time to flush to the collector
        Thread.sleep(100)
    }

    @Test
    fun `should create telemetry`() {
        val testId = "should-create-telemetry"
        Helpers.runWithTelemetry(provider = provider,
            tracer = tracer,
            telemetryContext = OpenTelemetryContext.root(),
            spanDetails = SpanDetails(
                "testspan", SpanKind.INTERNAL, Attributes.of(AttributeKey.stringKey("testid"), testId)
            ),
            block = {})

        val analyser = provider.spans().analyser().withAttributeValue("testid", testId)

        assertThat(analyser.traceIds().size, equalTo(1))
        assertThat(analyser.spanIds().size, equalTo(1))
        val span = analyser.firstSpan()
        assertThat(span.name, equalTo("testspan"))
        assertThat(span.status, equalTo(StatusData.ok()))
        assertThat(span.totalRecordedEvents, equalTo(0))
        assertThat(span.kind, equalTo(SpanKind.INTERNAL))
        assertThat(span.attributes.size(), equalTo(1))
    }

    @Test
    fun `should record exception in telemetry`() {
        val testId = "should-record-exception-in-telemetry"
        try {
            Helpers.runWithTelemetry(provider = provider,
                tracer = tracer,
                telemetryContext = OpenTelemetryContext.root(),
                spanDetails = SpanDetails(
                    "testspan", SpanKind.SERVER, Attributes.of(AttributeKey.stringKey("testid"), testId)
                ),
                block = { throw RuntimeException("opps") })
            fail("should have thrown an exception")
        } catch (ex: RuntimeException) {
            assertThat(ex.message, equalTo("opps"))
        }

        val analyser = provider.spans().analyser().withAttributeValue("testid", testId)

        assertThat(analyser.traceIds().size, equalTo(1))
        assertThat(analyser.spanIds().size, equalTo(1))
        val span = analyser.firstSpan()
        assertThat(span.name, equalTo("testspan"))
        assertThat(span.status, equalTo(StatusData.error()))
        assertThat(span.totalRecordedEvents, equalTo(1))
        assertThat(span.kind, equalTo(SpanKind.SERVER))
        assertThat(span.attributes.size(), equalTo(1))
    }

    @Test
    fun `should create telemetry with propagated context`() {
        val testId = "should-create-telemetry-with-propagated-context"
        val outer = tracer.spanBuilder("outerspan").setSpanKind(SpanKind.CLIENT).startSpan()
        Helpers.runWithTelemetry(provider = provider,
            tracer = tracer,
            telemetryContext = OpenTelemetryContext.fromSpan(outer),
            spanDetails = SpanDetails(
                "testspan", SpanKind.SERVER, Attributes.of(AttributeKey.stringKey("testid"), testId)
            ),
            block = {})
        outer.end()

        val analyser = provider.spans().analyser().withAttributeValue("testid", testId)

        assertThat(analyser.traceIds().size, equalTo(1))
        assertThat(analyser.spanIds().size, equalTo(2))
        val span = analyser.secondSpan()
        assertThat(span.name, equalTo("testspan"))
        assertThat(span.status, equalTo(StatusData.ok()))
        assertThat(span.totalRecordedEvents, equalTo(0))
        assertThat(span.kind, equalTo(SpanKind.SERVER))
        assertThat(span.attributes.size(), equalTo(1))
    }

    @Test
    fun `should create telemetry in current context`() {
        val testId = "should-create-telemetry-in-current-context"
        val random = Random(System.currentTimeMillis())

        // simulate a client to create a new trace
        val outer = tracer.spanBuilder("clientspan")
            .setSpanKind(SpanKind.CLIENT)
            .setAttribute("testid", testId)
            .startSpan()

        // simulate a server that has propagated the context from the client
        Helpers.runWithTelemetry(provider = provider,
            tracer = tracer,
            telemetryContext = OpenTelemetryContext.fromSpan(outer),
            spanDetails = SpanDetails("serverspan", SpanKind.SERVER),
            block = {
                Thread.sleep(random.nextLong(10))

                // simulate calls inside the client that pick up the existing context, so
                // they share the same parent
                Helpers.runWithCurrentTelemetry(provider = provider,
                    tracer = tracer,
                    spanDetails = SpanDetails("testspan", SpanKind.INTERNAL),
                    block = { Thread.sleep(random.nextLong(10)) })

                Helpers.runWithCurrentTelemetry(provider = provider,
                    tracer = tracer,
                    spanDetails = SpanDetails("testspan", SpanKind.INTERNAL),
                    block = { Thread.sleep(random.nextLong(10)) })

                Thread.sleep(random.nextLong(10))
            }
        )
        outer.end()

        val analyser = provider.spans().analyser().withAttributeValue("testid", testId)
        assertThat(analyser.traceIds().size, equalTo(1))
        assertThat(analyser.spanIds().size, equalTo(4))
        val actualOuter = analyser.firstSpan()
        assertThat(actualOuter.kind, equalTo(SpanKind.CLIENT))
        assertThat(actualOuter.name, equalTo("clientspan"))

        val serverSpans = analyser.children(actualOuter)
        assertThat(serverSpans.spanIds().size, equalTo(3))
        val serverSpan = serverSpans.withName("serverspan").single()
        assertThat(serverSpan.kind, equalTo(SpanKind.SERVER))
        val testSpans = serverSpans.withName("testspan")
        assertThat(testSpans.size, equalTo(2))
        assertThat(testSpans.spanKinds(), equalTo(setOf(SpanKind.INTERNAL)))
    }

}