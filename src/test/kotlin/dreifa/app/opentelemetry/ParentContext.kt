package dreifa.app.opentelemetry

class ParentContext(val traceId: String, val spanId: String) {
    companion object {
        val root = ParentContext("", "")
    }
}