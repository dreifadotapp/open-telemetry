package dreifa.app.opentelemetry.presenters

object EpochNanoPresenter {
    fun present(epoch: Long): String {
        if ((epoch / 1_000_1000_000) > 1) {
            return "${(epoch / 1_000_000_000)}s"
        }
        if ((epoch / 1_000_000) > 1) {
            return "${(epoch / 1_000_000)}ms"
        }
        if ((epoch / 1_000) > 1) {
            return "${(epoch / 1000)}μs"
        }
        return "${epoch}ns"
    }
}