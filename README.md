# Open Telemetry

[![Circle CI](https://circleci.com/gh/dreifadotapp/open-telemetry.svg?style=shield)](https://circleci.com/gh/dreifadotapp/open-telemetry)
[![Licence Status](https://img.shields.io/github/license/dreifadotapp/open-telemetry)](https://github.com/dreifadotapp/open-telemetry/blob/master/licence.txt)

## What it does?

Some helpers and support classes for [Open Telemetry](https://opentelemetry.io/)

## Dependencies

As with everything in [Dreifa dot App](https://dreifa.app), this library has minimal dependencies:

* Kotlin 1.4
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

