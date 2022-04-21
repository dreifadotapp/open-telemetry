package dreifa.app.opentelemetry

import io.opentelemetry.api.internal.ImmutableSpanContext
import io.opentelemetry.api.trace.*
import io.opentelemetry.context.Context
import io.opentelemetry.context.ContextKey
import io.opentelemetry.context.ImplicitContextKeyed
import io.opentelemetry.context.Scope
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.context.propagation.TextMapSetter


// information to transfer between layers
data class Carrier(var traceId: String? = null, var spanId: String? = null)

class X : TextMapSetter<Carrier> {
    override fun set(carrier: Carrier?, key: String, value: String) {
        println("TextMapSetter with carrier: $carrier")
    }

}

class Y : TextMapGetter<Carrier> {
    override fun keys(carrier: Carrier): Iterable<String> {
        return listOf("opentelemetry-trace-span-key")
    }

    override fun get(carrier: Carrier?, key: String): String? {
        if ((key == "opentelemetry-trace-span-key") && carrier != null) {
            return carrier.traceId
        }
        return null
    }
}

class NoopTextMapGetter : TextMapGetter<Carrier> {
    override fun keys(carrier: Carrier): Iterable<String> = emptyList()
    override fun get(carrier: Carrier?, key: String): String? = null
}

class MyPropagators : ContextPropagators {
    override fun getTextMapPropagator(): TextMapPropagator = MyTextMapPropagator()
}

class MyTextMapPropagator : TextMapPropagator {

    override fun fields(): Collection<String> {
        println("todo")
        return emptyList()
    }

    override fun <C : Any?> inject(context: Context, carrier: C?, setter: TextMapSetter<C>) {
        println(setter)
        println("todo")

        val k = ContextKey.named<Any>("opentelemetry-trace-span-key")
        println(k.javaClass.name)
        val x = context.get(k)
        println(x)
    }

    override fun <C : Any?> extract(context: Context, carrier: C?, getter: TextMapGetter<C>): Context {
        return if (carrier is Carrier) {
            val ctx = ImmutableSpanContext.create(
                carrier.traceId,
                carrier.spanId,
                TraceFlags.getDefault(),
                TraceState.getDefault(),
                true,
                false
            )

            val propagatedSpan = Span.wrap(ctx)
            context.with(propagatedSpan)
        } else {
            Context.current()
        }
    }

}

//class Z : ImplicitContextKeyed {
//    override fun storeInContext(context: Context): Context {
//        TODO("Not yet implemented")
//    }
//
//}

class ParentContext(val traceId: String, val spanId: String) {
    companion object {
        val root = ParentContext("", "")
    }
}

class ContextHelper(private val p: OpenTelemetryProvider) {
    fun createContext(traceId: String, spanId: String): Context {
        val propagator = p.provider().propagators.textMapPropagator
        val c = Carrier(traceId = traceId, spanId = spanId)
        return propagator.extract(Context.current(), c, NoopTextMapGetter())
    }

    fun createContext(parent : ParentContext) : Context {
        return createContext(parent.traceId, parent.spanId)
    }
}