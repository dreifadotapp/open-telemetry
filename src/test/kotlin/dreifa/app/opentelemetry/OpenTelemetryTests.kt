package dreifa.app.opentelemetry

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.Tracer
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenTelemetryTests {
    private lateinit var provider: ZipKinOpenTelemetryProvider

    @BeforeEach
    fun `fresh provider`() {
        provider = ZipKinOpenTelemetryProvider()
    }

    @AfterAll
    fun `wait for zipkin`() {
        // give it time to flush to zipkin
        Thread.sleep(50)
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
    }

    private fun buildTracer(scope: String): Tracer {
        return provider.provider().getTracer(scope)
    }
}
