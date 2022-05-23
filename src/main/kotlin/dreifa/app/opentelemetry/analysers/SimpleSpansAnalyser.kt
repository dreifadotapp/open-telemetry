package dreifa.app.opentelemetry.analysers

import dreifa.app.types.CorrelationContext
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.SpanId
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.data.StatusData

class SimpleSpansAnalyser(spans: List<SpanData>) : Iterable<SpanData> {

    enum class MatchingRule {
        SingleSpan,         // only apply the rule to the Span being checked
        AnySpanInTraceId,   // checks all spans within the traceId, regardless of the position in the hierarchy
    }

    // deep copy to be sure there are no changes
    // and that spans are in chronological order
    private val spans: List<SpanData> = ArrayList(spans.sortedBy { it.startEpochNanos })

    fun filterTraceId(traceId: String): SimpleSpansAnalyser {
        return SimpleSpansAnalyser(spans.filter { it.traceId == traceId })
    }

    fun withAttribute(key: String, rule: MatchingRule = MatchingRule.AnySpanInTraceId): SimpleSpansAnalyser {
        return when (rule) {
            MatchingRule.SingleSpan -> {
                SimpleSpansAnalyser(spans.filter { SimpleSpanAnalyser(it).hasAttribute(key) })
            }
            MatchingRule.AnySpanInTraceId -> {
                val filtered = ArrayList<SpanData>()
                traceIds().forEach { traceId ->
                    val spansForTraceId = filterTraceId(traceId)
                    if (spansForTraceId.withAttribute(key, MatchingRule.SingleSpan).isNotEmpty()) {
                        filtered.addAll(spansForTraceId.spans)
                    }
                }
                SimpleSpansAnalyser(filtered)
            }
        }
    }

    @Deprecated(message = "use withAttribute")
    fun filterHasAttribute(key: String, rule: MatchingRule = MatchingRule.AnySpanInTraceId) = withAttribute(key, rule)


    @Deprecated(message = "use withAttribute")
    fun filterHasAttribute(
        key: AttributeKey<Any>,
        rule: MatchingRule = MatchingRule.AnySpanInTraceId
    ): SimpleSpansAnalyser {
        return withAttribute(key.key, rule)
    }

    fun withAttribute(
        key: AttributeKey<Any>,
        rule: MatchingRule = MatchingRule.AnySpanInTraceId
    ): SimpleSpansAnalyser {
        return withAttribute(key.key, rule)
    }

    fun withAttributeValue(
        key: String,
        value: Any,
        rule: MatchingRule = MatchingRule.AnySpanInTraceId
    ): SimpleSpansAnalyser {
        return when (rule) {
            MatchingRule.SingleSpan -> {
                SimpleSpansAnalyser(spans.filter { SimpleSpanAnalyser(it).hasAttributeValue(key, value) })
            }
            MatchingRule.AnySpanInTraceId -> {
                val filtered = ArrayList<SpanData>()
                traceIds().forEach { traceId ->
                    val spansForTraceId = filterTraceId(traceId)
                    if (spansForTraceId.withAttributeValue(key, value, MatchingRule.SingleSpan).isNotEmpty()) {
                        filtered.addAll(spansForTraceId.spans)
                    }
                }
                SimpleSpansAnalyser(filtered)
            }
        }
    }

    @Deprecated(message = "use withAttributeValue")
    fun filterHasAttributeValue(
        key: String,
        value: Any,
        rule: MatchingRule = MatchingRule.AnySpanInTraceId
    ) = withAttributeValue(key, value, rule)


    @Deprecated(message = "use withAttributeValue")
    fun filterHasAttributeValue(key: AttributeKey<Any>, value: Any) = withAttributeValue(key, value)

    fun withAttributeValue(key: AttributeKey<Any>, value: Any): SimpleSpansAnalyser {
        return withAttributeValue(key.key, value)
    }

    @Deprecated(message = "use withCorrelation")
    fun filterHasAttributeValue(correlation: CorrelationContext) = withCorrelation(correlation)

    fun withCorrelation(correlation: CorrelationContext): SimpleSpansAnalyser {
        return withAttributeValue(correlation.openTelemetryAttrName, correlation.id.id)
    }


    fun traceIds(): Set<String> = spans.map { it.traceId }.toSet()

    fun spanIds(): Set<String> = spans.map { it.spanId }.toSet()

    fun spanNames(): Set<String> = spans.map { it.name }.toSet()

    fun spanKinds(): Set<SpanKind> = spans.map { it.kind }.toSet()


    fun rootSpan(): SpanData = spans.single { it.parentSpanContext.spanId == SpanId.getInvalid() }

    fun withName(name: String): SimpleSpansAnalyser {
        val filtered = spans.filter { it.name == name }
        return SimpleSpansAnalyser(filtered)
    }

    fun withKind(kind: SpanKind): SimpleSpansAnalyser {
        val filtered = spans.filter { it.kind == kind }
        return SimpleSpansAnalyser(filtered)
    }

    fun withStatue(status: StatusData): SimpleSpansAnalyser {
        val filtered = spans.filter { it.status == status }
        return SimpleSpansAnalyser(filtered)
    }

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

    fun spans(): List<SpanData> = spans

    val size: Int = spans.size

    fun isEmpty(): Boolean = spans.isEmpty()

    fun isNotEmpty(): Boolean = spans.isNotEmpty()

    override fun iterator(): Iterator<SpanData> = spans.iterator()

    operator fun get(index: Int) = spans[index]

}


