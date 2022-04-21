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
        println("todo")
        val k = ContextKey.named<String>("opentelemetry-trace-span-key")
        //val x = ImplicitContextKeyed()
        //context.with {  }
        //context.with()
        // getter.
        val c = (carrier as Carrier)
        val ctx = ImmutableSpanContext.create(
            c.traceId,
            c.spanId,
            TraceFlags.getDefault(),
            TraceState.getDefault(),
            true,
            false
        )

        val myspan = Span.wrap(ctx)

        context.with(myspan)

        //val updated = context.with(k, myspan as String)
        val updated = context.with(myspan)
        return updated
    }

}

//class Z : ImplicitContextKeyed {
//    override fun storeInContext(context: Context): Context {
//        TODO("Not yet implemented")
//    }
//
//}

class Wibble(private val p : OpenTelemetryProvider) {

    fun setScope(traceId : String, spanId: String) : Scope {
        val propagator = p.provider().propagators.textMapPropagator
        val getter = Y()
        val c = Carrier(traceId = traceId, spanId = spanId )

        val xx: Context =  propagator.extract(Context.current(), c, getter)
        return xx.makeCurrent()
    }

    fun createContext(traceId : String, spanId: String) : Context {
        val propagator = p.provider().propagators.textMapPropagator
        val getter = Y()
        val c = Carrier(traceId = traceId, spanId = spanId )

        val xx: Context =  propagator.extract(Context.current(), c, getter)
        return xx
    }


    fun xx () {

        val propagator = p.provider().propagators.textMapPropagator


    }

}