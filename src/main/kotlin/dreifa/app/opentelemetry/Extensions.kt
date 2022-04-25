package dreifa.app.opentelemetry

/**
 * Keep all extensions functions in one file for clarity
 */

import dreifa.app.opentelemetry.analysers.SimpleSpanAnalyser
import dreifa.app.opentelemetry.analysers.SimpleSpansAnalyser
import io.opentelemetry.sdk.trace.data.SpanData

fun List<SpanData>.analyser(): SimpleSpansAnalyser = SimpleSpansAnalyser(this)

fun SpanData.analyser(): SimpleSpanAnalyser = SimpleSpanAnalyser(this)