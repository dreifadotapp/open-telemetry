package dreifa.app.opentelemetry.analysers

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.SpanId
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

    fun traceIds(): Set<String> = spans.map { it.traceId }.toSet()

    fun spanIds(): Set<String> = spans.map { it.spanId }.toSet()

    fun rootSpan(): SpanData = spans.single { it.parentSpanContext.spanId == SpanId.getInvalid() }

    fun children(parent: SpanData): SimpleSpansAnalyser {
        val filtered = spans.filter {
            it.parentSpanContext.spanId == parent.spanId &&
                    it.parentSpanContext.traceId == parent.traceId
        }
        return SimpleSpansAnalyser(filtered)
    }

    fun children(parent: SimpleSpanAnalyser): SimpleSpansAnalyser {
        return children(parent.span)
    }

    fun overlapping(): Boolean {
        if (spans.size >= 2) {
            val timeOrdered = spans.sortedBy { it.startEpochNanos }
            var endEpoch = timeOrdered[0].endEpochNanos
            for (i in 1..spans.size - 1) {
                if (timeOrdered[i].startEpochNanos <= endEpoch) {
                    return true
                } else {
                    endEpoch = timeOrdered[i].endEpochNanos
                }
            }
        }

        return false
    }

    fun firstSpan(): SpanData = spans[0]

    fun secondSpan(): SpanData = spans[1]

    fun lastSpan(): SpanData = spans.last()

    val size: Int = spans.size

    override fun iterator(): Iterator<SpanData> = spans.iterator()

}


