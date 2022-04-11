package dreifa.app.opentelemetry

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.trace.data.SpanData

class SimpleSpansAnalyser(spans: List<SpanData>) : Iterable<SpanData> {
    // deep copy to be sure there are no changes
    private val spans: List<SpanData> = ArrayList(spans)

    fun filterTraceId(traceId: String): SimpleSpansAnalyser {
        return SimpleSpansAnalyser(spans.filter { it.traceId == traceId })
    }

    fun filterHasAttribute(key: String): SimpleSpansAnalyser {
        return SimpleSpansAnalyser(spans.filter { SimpleSpanAnalyser(it).hasAttribute(key) })
    }

    fun filterHasAttribute(key: AttributeKey<Any>): SimpleSpansAnalyser {
        return filterHasAttribute(key.key)
    }

    fun filterHasAttributeValue(key: String, value: Any): SimpleSpansAnalyser {
        return SimpleSpansAnalyser(spans.filter { SimpleSpanAnalyser(it).hasAttributeValue(key, value) })
    }

    fun filterHasAttributeValue(key: AttributeKey<Any>, value: Any): SimpleSpansAnalyser {
        return filterHasAttributeValue(key.key, value)
    }

    fun traceIds(): Set<String> {
        return spans.map { it.traceId }.toSet()
    }

    val size: Int = spans.size

    override fun iterator(): Iterator<SpanData> = spans.iterator()

}


