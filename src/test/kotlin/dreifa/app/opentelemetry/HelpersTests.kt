package dreifa.app.opentelemetry

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dreifa.app.opentelemetry.analysers.SimpleSpansAnalyser
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.trace.data.StatusData
import org.junit.jupiter.api.*

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
        Helpers.runWithTelemetry(
            provider = provider,
            tracer = tracer,
            telemetryContext = OpenTelemetryContext.root(),
            spanDetails = SpanDetails(
                "span1",
                SpanKind.INTERNAL,
                Attributes.of(AttributeKey.stringKey("testid"), "should-create-telemetry")
            ),
            block = {}
        )

        val spansAnalyser = provider.spans().analyser()
            .filterHasAttributeValue(
                "testid",
                "should-create-telemetry",
                SimpleSpansAnalyser.MatchingRule.AnySpanInTraceId
            )

        assertThat(spansAnalyser.traceIds().size, equalTo(1))
        assertThat(spansAnalyser.spanIds().size, equalTo(1))
        val span = spansAnalyser.firstSpan()
        assertThat(span.name, equalTo("span1"))
        assertThat(span.status, equalTo(StatusData.ok()))
        assertThat(span.totalRecordedEvents, equalTo(0))
        assertThat(span.kind, equalTo(SpanKind.INTERNAL))
        assertThat(span.attributes.size(), equalTo(1))
    }

    @Test
    fun `should record exception in telemetry`() {
        try {
            Helpers.runWithTelemetry(
                provider = provider,
                tracer = tracer,
                telemetryContext = OpenTelemetryContext.root(),
                spanDetails = SpanDetails(
                    "span2",
                    SpanKind.SERVER,
                    Attributes.of(AttributeKey.stringKey("testid"), "should-record-exception-in-telemetry")
                ),
                block = { throw RuntimeException("opps") }
            )
            fail("should have thrown an execption")
        } catch (ex: RuntimeException) {
            assertThat(ex.message, equalTo("opps"))
        }

        val spansAnalyser = provider.spans().analyser()
            .filterHasAttributeValue(
                "testid",
                "should-record-exception-in-telemetry",
                SimpleSpansAnalyser.MatchingRule.AnySpanInTraceId
            )

        assertThat(spansAnalyser.traceIds().size, equalTo(1))
        assertThat(spansAnalyser.spanIds().size, equalTo(1))
        val span = spansAnalyser.firstSpan()
        assertThat(span.name, equalTo("span2"))
        assertThat(span.status, equalTo(StatusData.error()))
        assertThat(span.totalRecordedEvents, equalTo(1))
        assertThat(span.kind, equalTo(SpanKind.SERVER))
        assertThat(span.attributes.size(), equalTo(1))
    }

}