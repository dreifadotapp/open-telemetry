package dreifa.app.opentelemetry

import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter

class InMemorySpanExporter(private val decorated: SpanExporter? = null) : SpanExporter {
    val allSpans = ArrayList<SpanData>()
    override fun export(spans: MutableCollection<SpanData>): CompletableResultCode {
        allSpans.addAll(spans)
        return if (decorated == null) CompletableResultCode.ofSuccess() else decorated.export(spans)
    }

    override fun flush(): CompletableResultCode {
        allSpans.clear()
        return if (decorated == null) CompletableResultCode.ofSuccess() else decorated.flush()
    }

    override fun shutdown(): CompletableResultCode {
        return if (decorated == null) CompletableResultCode.ofSuccess() else decorated.shutdown()
    }
}