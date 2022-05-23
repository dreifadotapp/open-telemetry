package dreifa.app.opentelemetry

import dreifa.app.types.CorrelationContext
import dreifa.app.types.CorrelationContexts
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes

object AttributesHelper {
    fun fromCorrelation(correlation: CorrelationContext): Attributes {
        return Attributes.of(AttributeKey.stringKey(correlation.openTelemetryAttrName), correlation.id.id)
    }

    fun fromCorrelations(correlations: CorrelationContexts): Attributes {
        val builder = Attributes.builder()
        correlations.forEach {
            builder.put(it.openTelemetryAttrName, it.id.id)
        }
        return builder.build()
    }

    fun stringAttr(key: String, value: String): Attributes {
        return Attributes.of(AttributeKey.stringKey(key), value)
    }

    fun longAttr(key: String, value: Long): Attributes {
        return Attributes.of(AttributeKey.longKey(key), value)
    }
}