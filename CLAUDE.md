# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

ScratchPad (Gradle root project name `Scratch2`) is an Android playground app. It provides a
Compose UI shell with a dropdown to pick a "sketch" (a small self-contained piece of Kotlin or
Java code), run it, and view its output in a scrolling log pane. It exists to quickly try out
language features, APIs, or algorithms on-device without spinning up a new project each time.

## Build / test / lint commands

Run from the repo root using the Gradle wrapper:

- Build debug APK: `./gradlew assembleDebug`
- Run JVM unit tests (app module): `./gradlew :app:testDebugUnitTest`
- Run a single unit test: `./gradlew :app:testDebugUnitTest --tests "org.prbentley.scratchpad.ExampleUnitTest"`
- Run instrumented tests (needs a connected device/emulator): `./gradlew :app:connectedDebugAndroidTest`
- Lint: `./gradlew :app:lint`
- Build the KSP annotation processor module on its own: `./gradlew :ksp:build`
- Install to a connected device: `./gradlew :app:installDebug`

There are two Gradle modules: `app` (the Android application, `org.prbentley.scratchpad`) and
`ksp` (a plain JVM module implementing a KSP `SymbolProcessor`, `org.prbentley.scratchpad.ksp`).
`app` depends on `ksp` via `ksp(project(":ksp"))`.

## Architecture: how sketches are discovered and run

The central mechanic of this app is a compile-time registry of "sketches," built by the KSP
module and consumed by the app module. To understand any change here you need to read across
both modules — no single file tells the whole story:

1. **Authoring a sketch** — a class in `app/src/main/java/org/prbentley/scratchpad/sketch/`
   extends `BaseSketch` (`app/.../BaseSketch.kt`) and is annotated `@Sketch`
   (`app/.../Sketch.kt`). Exactly one sketch may additionally be annotated `@Default`
   (`app/.../Default.kt`) to make it the one selected at app startup. Sketches can be Kotlin or
   Java (see `HelloJava.java`).
2. **Discovery at compile time** — `ksp/.../SketchFinder.kt` runs as a KSP `SymbolProcessor`
   during the `app` module's compilation. It scans for all classes annotated `@Sketch` and
   `@Default`, then code-generates `org.prbentley.scratchpad.SketchProvider` (an object with a
   `sketches: List<Class<out BaseSketch>>` and `defaultSketchName: String?`) using a tiny
   string-templating helper, `ValueMapper.kt`. This generated file does not exist in source —
   it only appears in `app/build/generated/ksp/...` after a build, so don't expect to grep for
   it in `app/src`.
3. **Runtime lookup** — `SketchModel.kt` reads `SketchProvider.sketches`/`defaultSketchName`,
   builds a name-to-class map, and instantiates a sketch via reflection
   (`getDeclaredConstructor().newInstance()`) in `createSketch()`, wiring up `scratchpad`,
   `logger`, and an `onFinished` callback on the new instance.
4. **Adding a new sketch** therefore requires no manual registration: just create the class
   with `@Sketch` in the `sketch` package and rebuild — KSP regenerates `SketchProvider` and it
   appears in the dropdown automatically. Because discovery is annotation-based, a clean/rebuild
   is sometimes needed after adding or renaming a sketch for the generated file to catch up.

### Execution model

- `BaseSketch.run()` is invoked off the main thread (`MainViewModel` launches it on
  `Dispatchers.IO`) and calls the abstract `go()`, catching exceptions and always calling
  `after()` and resetting `isRunning` in a `finally` block.
- Long-running sketches should periodically check the `shouldContinue` (an atomicfu `atomic`
  Boolean) flag inside loops to support cooperative cancellation — see `Fibonacci.kt`. Calling
  `stop()` sets `shouldContinue = false` and cancels the coroutine `job`.
- Sketches write output via `out(...)`/`verbose(...)` on `BaseSketch`, which forward to the
  injected `Logger` (`Logger.kt`). `Logger` fans out to both Logcat and a Compose-backed
  `ComposableLogSink` that the UI observes as `SnapshotStateList`, so log lines appear live in
  the scrollable log pane. Verbosity and timestamp display are toggled from the UI and mutate
  the shared `ComposableLogSink` state directly.
- `toast(...)` is available for quick on-screen feedback (uses `scratchpad.applicationContext`).

### Widgets (per-sketch UI controls)

A sketch can expose interactive controls above its log output by overriding `widgets: List<Widget>`
with instances of `SliderWidget`, `TextWidget`, `BooleanWidget`, or `ButtonWidget` (all in
`app/src/main/java/org/prbentley/scratchpad/`). Each `Widget` is a small `@Composable` wrapper
holding its own Compose state (e.g. `SliderWidget.value` via `mutableStateOf`), which the
sketch's `go()` reads directly when it runs — there's no separate view-model/state layer per
sketch. `BaseSketch.Ui()` renders `widgets` by default; override it for custom layouts.

### App shell

`MainActivity` → `MainScreen` (Compose) → `MainViewModel` (`AndroidViewModel`) wires everything
together: the sketch dropdown (`SketchSelector`), the Go/Stop button, verbose/timestamp
checkboxes, the current sketch's `Ui()`, and the log pane. Switching sketches in the dropdown
stops the currently running one and instantiates a fresh instance via `SketchModel.createSketch`.
