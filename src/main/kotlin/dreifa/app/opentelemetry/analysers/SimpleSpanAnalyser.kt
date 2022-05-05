package dreifa.app.opentelemetry.analysers

import dreifa.app.types.CorrelationContext
import io.opentelemetry.sdk.trace.data.SpanData

class SimpleSpanAnalyser(val span: SpanData) {

    fun hasAttribute(key: String): Boolean {
        var found = false
        span.attributes.forEach { attributeKey, _ ->
            if (!found && (attributeKey.key == key)) found = true
        }
        return found
    }

    fun hasAttributeValue(key: String, value: Any): Boolean {
        var found = false
        span.attributes.forEach { attributeKey, attributeValue ->
            if (!found && (attributeKey.key == key) && (attributeValue == value)) found = true
        }
        return found
    }

    fun hasAttributeValue(correlation: CorrelationContext): Boolean {
        return hasAttributeValue(correlation.openTelemetryAttrName, correlation.id.id)
    }

    val name = span.name

    val kind = span.kind

    val spanId = span.spanId

    val traceId = span.traceId

}

