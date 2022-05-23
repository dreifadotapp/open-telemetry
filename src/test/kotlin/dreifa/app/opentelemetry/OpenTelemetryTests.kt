package dreifa.app.opentelemetry

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dreifa.app.opentelemetry.analysers.SimpleSpansAnalyser
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.Tracer
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenTelemetryTests {
    private lateinit var provider: OpenTelemetryProvider

    @BeforeEach
    fun `fresh provider`() {
        provider = JaegerOpenTelemetryProvider(true)
    }

    @AfterAll
    fun `wait to flush telemetry`() {
        // give it time to flush to the collector
        Thread.sleep(100)
    }

    @Test
    fun `should trace client to server`() {
        val clientTracer = buildTracer("client")
        val serverTracer = buildTracer("tracer")

        val server1 = DummyServer1(serverTracer, provider)
        val server2 = DummyServer2(serverTracer, provider)
        val client = DummyClient(clientTracer, server1, server2)

        runBlocking {
            client.exec("foobar")
        }

        val spansAnalyser = provider.spans().analyser()

        assertThat(spansAnalyser.traceIds().size, equalTo(1))
        assertThat(spansAnalyser.spanIds().size, equalTo(3))

        val rootSpan = spansAnalyser.rootSpan().analyser()
        assertThat(rootSpan.name, equalTo("DummyClient"))
        assertThat(rootSpan.kind, equalTo(SpanKind.CLIENT))

        val serverSpans = spansAnalyser.children(rootSpan)
        assertThat(serverSpans.spanIds().size, equalTo(2))
        assert(!serverSpans.overlapping())

        // check sorting and indexing of SpansAnalyser - by default is chronological
        assertThat(spansAnalyser.firstSpan().name, equalTo("DummyClient"))
        assertThat(spansAnalyser[0].name, equalTo("DummyClient"))
        assertThat(spansAnalyser.secondSpan().name, equalTo("DummyServer1"))
        assertThat(spansAnalyser[1].name, equalTo("DummyServer1"))
        assertThat(spansAnalyser.lastSpan().name, equalTo("DummyServer2"))
        assertThat(spansAnalyser[2].name, equalTo("DummyServer2"))

        // check filtering by attr
        assertThat(spansAnalyser.withAttribute("server.attr").size, equalTo(3))
        assertThat(spansAnalyser.withAttribute("server.attr", SimpleSpansAnalyser.MatchingRule.AnySpanInTraceId).size, equalTo(3))
        assertThat(spansAnalyser.withAttribute("server.attr", SimpleSpansAnalyser.MatchingRule.SingleSpan).size, equalTo(2))
        assert(spansAnalyser.withAttribute("missing.attr").isEmpty())

        // check filtering by attr / value
        assertThat(spansAnalyser.withAttributeValue("server.attr", "server1").size, equalTo(3))
        assertThat(spansAnalyser.withAttributeValue("server.attr", "server2").size, equalTo(3))
        assertThat(spansAnalyser.withAttributeValue("client.attr", "client").size, equalTo(3))
        assertThat(spansAnalyser.withAttributeValue("client.attr", "client", SimpleSpansAnalyser.MatchingRule.AnySpanInTraceId).size, equalTo(3))
        assertThat(spansAnalyser.withAttributeValue("client.attr", "client", SimpleSpansAnalyser.MatchingRule.SingleSpan).size, equalTo(1))
        assert(spansAnalyser.withAttributeValue("client.attr", "missing value", SimpleSpansAnalyser.MatchingRule.SingleSpan).isEmpty())
        assert(spansAnalyser.withAttributeValue("missing.attr", "client", SimpleSpansAnalyser.MatchingRule.SingleSpan).isEmpty())
    }


    private fun buildTracer(scope: String): Tracer {
        return provider.sdk().getTracer(scope)
    }
}
