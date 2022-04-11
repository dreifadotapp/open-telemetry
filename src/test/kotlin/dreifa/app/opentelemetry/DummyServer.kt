package dreifa.app.opentelemetry

import io.opentelemetry.api.trace.Tracer
import java.util.*

class DummyServer(private val tracer: Tracer) {

    fun exec(traceId: UUID, payload : String) {

    }
}