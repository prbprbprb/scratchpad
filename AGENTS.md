# AGENTS.md

Compact guidance for OpenCode sessions in ScratchPad.

ScratchPad is an Android playground app: a Compose shell that lets you pick a small "sketch" experiment, run it, and watch its output in a scrolling log pane.

## Project shape

- Two Gradle modules: `:app` (Android app, `org.prbentley.scratchpad`) and `:ksp` (plain JVM KSP processor, `org.prbentley.scratchpad.ksp`).
- Root project name is `ScratchPad`.
- `app` depends on the processor via `ksp(project(":ksp"))`.
- To understand sketch discovery or the KSP processor you must read across both modules; no single file tells the whole story.

## Commands

Run from repo root with `./gradlew`:

- Build debug APK: `./gradlew assembleDebug`
- Run JVM unit tests: `./gradlew :app:testDebugUnitTest`
- Run one test: `./gradlew :app:testDebugUnitTest --tests "org.prbentley.scratchpad.ExampleUnitTest"`
- Run instrumented tests (needs device/emulator): `./gradlew :app:connectedDebugAndroidTest`
- Lint: `./gradlew :app:lint`
- Build KSP module alone: `./gradlew :ksp:build`
- Install to device: `./gradlew :app:installDebug`
- Check formatting: `./gradlew spotlessCheck`
- Apply formatting: `./gradlew spotlessApply`

CI order (`.github/workflows/ci.yml`): `spotlessCheck` → `assembleDebug testDebugUnitTest`.

## Tooling

- JDK 21 everywhere: it runs Gradle / Android Gradle Plugin and is the compilation toolchain (`jvmToolchain(21)`, Java/Kotlin targets 21 in both modules). The Foojay resolver in `settings.gradle.kts` auto-provisions it on machines that lack it.
- `minSdk` is deliberately 24 (the lowest the current Compose/AndroidX libraries allow) while `compileSdk`/`targetSdk` track the latest: the point is running the same sketches on old and new emulators to compare API behaviour. Never raise `minSdk` to silence lint; guard the call instead. Core library desugaring is enabled so `java.time` works below API 26 (`BaseSketch.sleep()` depends on it). Adaptive launcher icons live in `mipmap-anydpi-v26/`; older devices use the `.webp` mipmaps.
- Code style is enforced by Spotless with ktlint `android_studio` / AOSP style. `@Composable` functions are allowed PascalCase. Every `*.kt`, `*.java`, and `*.gradle.kts` file must carry the Apache 2.0 license header from `config/spotless/apache-2.0-header.txt`.
- A tracked pre-push hook (`.githooks/pre-push`) runs the same checks as CI and blocks the push on failure. Enable it once per clone with `git config core.hooksPath .githooks`; bypass with `git push --no-verify`. It builds the working tree, not the commits being pushed, so uncommitted changes affect the result.

## Sketch architecture

- A “sketch” is a self-contained experiment class in `app/src/main/java/org/prbentley/scratchpad/sketch/` that extends `BaseSketch` and is annotated `@Sketch` (`org.prbentley.scratchpad.Sketch`). Sketches can be Kotlin or Java (see `HelloJava.java`).
- Exactly one sketch may also be annotated `@Default` to be selected at startup.
- Sketches are discovered at compile time by `ksp/.../SketchFinder.kt`, which generates `org.prbentley.scratchpad.SketchProvider` in `app/build/generated/ksp/...` — it does **not** exist in `app/src/`.
- The registry is an aggregating KSP output (`Dependencies(true, ...)`), so incremental builds regenerate it in full: adding, renaming or removing a sketch needs no clean build. Keep it aggregating; as an isolating output, adding a new sketch file regenerates the registry from that file alone.
- `SketchModel` instantiates sketches by reflection via `getDeclaredConstructor().newInstance()` and wires `scratchpad`, `logger`, and `after`/`onFinished`.

## Writing a sketch

- Override `go()` for the main work. It runs off the main thread (`Dispatchers.IO`).
- For long-running loops, check `shouldContinue` cooperatively and break out; `stop()` sets it to `false` and cancels the job.
- Output with `out(...)` / `verbose(...)`; `Logger` fans out to both Logcat and a Compose `ComposableLogSink` observed by the UI.
- `toast(...)` posts a `Toast` using `scratchpad.applicationContext`.
- Expose UI controls by overriding `widgets: List<Widget>` (SliderWidget, TextWidget, BooleanWidget, ButtonWidget). Each widget holds its own Compose state, which `go()` reads directly; there is no per-sketch view model.
- `BaseSketch.Ui()` renders `widgets` by default; override it for a custom layout.
- Guard any platform API above 24 with a `Build.VERSION.SDK_INT` check that reports the missing API via `out(...)` on older devices, or mark helpers called only behind such a check with `@RequiresApi`. `./gradlew :app:lintDebug` must report no `NewApi` errors. Lint does not check library APIs; for those, rely on the library's own availability checks (e.g. `AppFunctionManager.getInstance()` returning `null`).
- See `Fibonacci.kt` for the cancellation/widget pattern and `HelloJava.java` for a Java sketch.

## App shell

- `MainActivity` → `MainScreen` (Compose) → `MainViewModel` wires the sketch dropdown, Go/Stop button, verbose/timestamp toggles, the current sketch's `Ui()`, and the log pane.
- Switching sketches in the dropdown stops the running sketch and instantiates a fresh instance via `SketchModel.createSketch`.
