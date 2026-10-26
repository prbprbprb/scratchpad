# Scratch Pad

A playground Android app for quick experiments, and for trying out platform
APIs across Android versions. Each experiment is a small *sketch* class: pick
one from the dropdown, tap **Go**, and watch its output in a scrolling log
pane. Sketches can add simple controls (sliders, checkboxes, text fields,
buttons) without writing any UI code.

Sketches are discovered at compile time by a small KSP processor, so adding
one is just a matter of writing the class.

## Requirements

- JDK 21
- Android SDK with platform 37 (Android Studio will offer to install it)
- A device or emulator running Android 7.0 (API 24) or later

## Android versions

The app's `minSdk` is deliberately low while `compileSdk` and `targetSdk`
track the latest release, so one build runs on everything from Android 7.0 to
the newest version. That makes it cheap to find out how an API behaves, or
whether it exists, on a given release: create emulators for the versions you
care about and run the same sketch on each.

API 24 is the lowest that the current Compose and AndroidX libraries
support. Core library desugaring is enabled, so `java.time` and other newer
Java APIs work on every version.

Because the code compiles against the newest SDK, nothing stops a sketch
calling an API the device doesn't have. Guarding those calls is up to the
sketch; see [Writing a sketch](#writing-a-sketch).

## Building and running

Open the project in Android Studio and run the `app` configuration, or
install from the command line:

```bash
./gradlew installDebug
```

| Task                                   | Command                                |
|----------------------------------------|----------------------------------------|
| Build a debug APK                      | `./gradlew assembleDebug`              |
| Run unit tests                         | `./gradlew testDebugUnitTest`          |
| Run instrumented tests (device needed) | `./gradlew connectedDebugAndroidTest`  |
| Check for unguarded newer-API calls    | `./gradlew lintDebug`                  |
| Check formatting                       | `./gradlew spotlessCheck`              |
| Fix formatting and license headers     | `./gradlew spotlessApply`              |

## Writing a sketch

Add a class to `app/src/main/java/org/prbentley/scratchpad/sketch/` that
extends `BaseSketch` and is annotated `@Sketch`:

```kotlin
package org.prbentley.scratchpad.sketch

import java.time.Duration
import org.prbentley.scratchpad.BaseSketch
import org.prbentley.scratchpad.Sketch
import org.prbentley.scratchpad.SliderWidget
import org.prbentley.scratchpad.Widget

@Sketch
class Countdown : BaseSketch() {
    private val start = SliderWidget("Count down from", 1..10, 3)

    override val widgets = listOf<Widget>(start)

    override fun go() {
        for (i in start.value downTo 1) {
            if (!shouldContinue) {
                return
            }
            out("$i...")
            sleep(Duration.ofSeconds(1))
        }
        out("Lift off!")
    }
}
```

- `go()` does the work. It runs on a background thread, not the main thread.
- `out(...)` writes a line to the log pane (and to Logcat); `verbose(...)`
  writes a line that only shows when **Verbose** is ticked. `toast(...)`
  shows a toast.
- **Stop** sets `shouldContinue` to `false`, so check it in long-running
  loops and return early.
- Controls go in `widgets`, and `go()` reads their current values directly:
  `SliderWidget` (`.value`), `BooleanWidget` (`.checked`), `TextWidget`
  (`.text`, optionally read-only), and `ButtonWidget`, which runs a lambda
  when pressed. Override `Ui()` instead if you want a custom Compose layout.
- Annotate one sketch with `@Default` as well to select it at startup.
- Sketches can be written in Java too; see `HelloJava.java`.

### Guarding newer APIs

The app runs on Android 7.0 and up, so check the device's API level before
calling anything newer than API 24, and report what happens on older
versions instead:

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    out("SoC model: ${Build.SOC_MODEL}")
} else {
    out("Build.SOC_MODEL needs Android 12 (API 31)")
}
```

For a helper that is only ever called behind such a check, annotate it with
`@RequiresApi` instead. `./gradlew lintDebug` (lint's `NewApi` check) flags
any newer-API call that isn't guarded. The `AppFunctions` sketch shows both
patterns.

## Included sketches

| Sketch                     | What it does                                                                        |
|----------------------------|-------------------------------------------------------------------------------------|
| `HelloJava`, `HelloKotlin` | Print Java system properties and security providers, in Java and Kotlin             |
| `Fibonacci`                | A deliberately slow recursive Fibonacci; shows sliders and stopping a sketch        |
| `Benchmark`                | Times sorting lists of several sizes                                                |
| `Https`                    | Connects to a host you enter over HTTPS and prints the response headers             |
| `Text`                     | Text fields, including a read-only one updated from code                            |
| `Button`                   | A button that shows a toast                                                         |
| `AppFunctions`             | Lists installed apps that export Android AppFunctions, and the functions they export |

## How it works

The project has two Gradle modules. `:ksp` is a KSP symbol processor that
finds every `@Sketch` class at compile time and generates a
`SketchProvider` registry (in `app/build/generated/ksp/`, not in the source
tree). `:app` is the Compose app: it reads the registry, instantiates the
selected sketch by reflection, runs `go()` on a background coroutine, and
shows the sketch's widgets and log output.

## Development

- **Code style:** AOSP style, enforced by Spotless with ktlint. Every
  Kotlin, Java and Gradle Kotlin script file carries the Apache 2.0 license
  header; `./gradlew spotlessApply` fixes formatting and adds missing
  headers.
- **Pre-push hook:** `.githooks/pre-push` runs the same checks as CI before
  each push. Enable it once per clone:

  ```bash
  git config core.hooksPath .githooks
  ```

- **CI:** GitHub Actions checks formatting, builds, runs the unit tests and
  runs lint on JDK 21. Lint errors fail the build, including calls to APIs
  newer than `minSdk` without a version check.
- [`AGENTS.md`](AGENTS.md) has more detail for coding agents.

## License

Copyright 2026 Pete Bentley. Licensed under the Apache License, Version 2.0;
see [LICENSE](LICENSE).
