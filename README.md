# Open Telemetry

[![Circle CI](https://circleci.com/gh/dreifadotapp/open-telemetry.svg?style=shield)](https://circleci.com/gh/dreifadotapp/open-telemetry)
[![Licence Status](https://img.shields.io/github/license/dreifadotapp/open-telemetry)](https://github.com/dreifadotapp/open-telemetry/blob/master/licence.txt)

## What it does?

Some helpers and support classes for [Open Telemetry](https://opentelemetry.io/)

## Running Tests

Most unit tests are setup to automatically send metrics to a Jaeger
collector, if running - this makes it easier to analyse the output. To start Jaeger locally, run

```bash
docker run --rm -it --name jaeger\
  -p 16686:16686 \
  -p 14250:14250 -d \
  jaegertracing/all-in-one:latest
```

Then open the [Jaeger UI](http://localhost:16686/search)

## The 'OpenTelemetryProvider' interface

This interface provides a convenient DI friendly way of configuring the Open Telemetry Java SDK. Most of the higher
level services in [Dreifa dot App](https://dreifa.app) use this to initialise Open Telemetry

Some simple implementations are provided for testing purposes. Production code would likely
provide a custom implementation.

### With Zipkin

Use `ZipkinOpenTelemetryProvider`

Zipkin can be started locally with

```bash
docker run --rm -it --name zipkin \
  -p 9411:9411 -d \
  openzipkin/zipkin:latest
```

Then open the [Zipkin UI](http://localhost:9411/zipkin/).

### With Jaeger

Use `JaegerOpenTelemetryProvider`

Jaeger can be started locally with

```bash
docker run --rm -it --name jaeger\
  -p 16686:16686 \
  -p 14250:14250 -d \
  jaegertracing/all-in-one:latest
```

Then open the [Jaeger UI](http://localhost:16686/search)

Some useful links for Jaeger

* https://opentelemetry.io/docs/instrumentation/js/exporters/
* https://www.jaegertracing.io/docs/1.33/getting-started/
* https://github.com/open-telemetry/opentelemetry-java-docs/tree/main/jaeger

### InMemory only

This is enough to capture output and assert the results in testcase.

Use `InMemoryOpenTelemetryProvider`

## Dependencies

As with everything in [Dreifa dot App](https://dreifa.app), this library has minimal dependencies:

* Kotlin 1.5
* Java 11
* The object [Registry](https://github.com/dreifadotapp/registry#readme)
* The [Open Telemetry Java Libs](https://opentelemetry.io/docs/instrumentation/java/)

## Adding as a dependency

Maven jars are deployed using [JitPack](https://jitpack.io/).
See [releases](https://github.com/dreifadotapp/open-telemetry/releases) for version details.

```groovy
//add jitpack repo
maven { url "https://jitpack.io" }

// add dependency 
implementation "com.github.dreifadotapp:open-telemetry:<release>"
```

_JitPack build status is at https://jitpack.io/com/github/dreifadotapp/open-telemetry/$releaseTag/build.log_
