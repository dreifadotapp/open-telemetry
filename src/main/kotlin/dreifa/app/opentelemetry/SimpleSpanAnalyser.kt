package dreifa.app.opentelemetry

import io.opentelemetry.sdk.trace.data.SpanData

class SimpleSpanAnalyser(private val span: SpanData) {

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
}