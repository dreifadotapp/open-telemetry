package dreifa.app.opentelemetry.presenters

import io.opentelemetry.sdk.trace.data.SpanData
import java.lang.StringBuilder

class TimelinePresenter {

    fun present(spans: List<SpanData>): String {

        var epochStart: Long = Long.MAX_VALUE
        var epochEnd: Long = Long.MIN_VALUE

        spans.forEach {
            if (it.startEpochNanos < epochStart) epochStart = it.startEpochNanos
            if (it.endEpochNanos > epochEnd) epochEnd = it.endEpochNanos
        }

        val ticksPerDivision = (epochEnd - epochStart)/80
        val sb = StringBuilder()
        sb.append(
            "Epoch start: $epochStart, Epoch end: $epochEnd, Epoch Period: " +
                    "${EpochNanoPresenter.present(epochEnd - epochStart)}, Ticks: $ticksPerDivision\n"
        )


        sb.append("x........".repeat(8)).append("\n")
        spans.forEach {
            val start = (it.startEpochNanos - epochStart) / ticksPerDivision

            val end = (epochEnd - it.endEpochNanos) / ticksPerDivision
            val width = end - start
            val overlap = width - it.name.length
            sb.append(" ".repeat(start.toInt()))
            sb.append(it.name)
            if (overlap > 0) {
                sb.append(".".repeat(overlap.toInt()))
            }
            sb.append("\n")

        }


        return sb.toString()

    }
}