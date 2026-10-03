# Decisions

## 2026-10-01

- Used AGP 9.4.1 plus 23 for the Lint artifact version, resulting in Lint 32.4.1.
- Used JDK 17 for `lint-rules` compilation, matching the project requirement and installed runtime.
- Placed the sample app lint configuration inside `android { lint { } }`, which is the supported DSL location for this AGP version.
- Milestone 0 uses a temporary marker rule only to prove registry loading. It must be removed before Milestone 1.

## 2026-10-03

### Build and Lint versions

- **Registry `minApi = 14`.** Lint API 14 is AGP 8.0 (`CURRENT_API` is 16, which is AGP 8.7 and newer). The development apps and the evaluation apps may use an older AGP than this project, so the rules should still load there. Lint older than AGP 8.0 is not supported. Loading on older Lint is not tested yet; if a rule fails there, raise `minApi`. Alternative considered: `minApi = CURRENT_API`, which is safer but refuses to load on AGP 8.0 to 8.6.
- **Kotlin version.** The project uses Kotlin 2.2.10. Lint 32.4.1 depends on `kotlin-stdlib` and `kotlin-reflect` 2.2.10 and bundles a matching compiler, so the versions match and `apiVersion`/`languageVersion` do not need to be set in `lint-rules`.
- **Gradle daemon JVM.** `gradle/gradle-daemon-jvm.properties` (written by Android Studio, not tracked in git) asks for JDK 25 for the Gradle daemon, and Gradle downloads it on the first run. The `lint-rules` module still compiles with a JDK 17 toolchain.

### Test infrastructure

- **Stubs follow the real sources.** Every stub in `ComposeStubs.kt` was checked against the sources of Compose 1.10.4 and Material3 1.4.0 (Compose BOM 2026.02.01) in the Gradle cache. Corrections to the first version: `clickable` is in `androidx.compose.foundation`; `semantics` and `clearAndSetSemantics` are in `androidx.compose.ui.semantics`; `toggleable` and `selectable` are in `androidx.compose.foundation.selection`; `pointerInput` is in `androidx.compose.ui.input.pointer`; `Preview` is in `androidx.compose.ui.tooling.preview`; `Icon` and `Image` take `imageVector`/`painter` first; semantics properties such as `contentDescription` are extension properties; `minimumInteractiveComponentSize` is a `Modifier` extension.
- **Parameters left out of stubs.** A stub may leave out parameters that no rule reads, but only after the last parameter a rule uses, so positional arguments still map to the right parameter.
- **The `Modifier.padding(horizontal, vertical)` stub has no default values.** The real function has `0.dp` defaults. Lint's `JVM_OVERLOADS` test mode adds `@JvmOverloads` to every Kotlin function in the test project, stubs included, and that would create a `padding(Dp)` overload that clashes with `padding(all)`. Real Compose does not use `@JvmOverloads`. Removing the defaults keeps the test mode on, which later rules that visit function declarations (R-05, R-06) need.
- **No `allowCompilationErrors()`.** If a stub or a test file does not compile, calls do not resolve and detectors silently match nothing. Tests must fail in that case.
- **All default Lint test modes stay on.** No mode is disabled.
- **JUnit 3 naming.** `LintDetectorTest` is a JUnit 3 `TestCase`. It ignores `@Test` and only runs methods whose names start with `test`. Detector tests must follow this. Plain unit tests such as `ContrastTest` use JUnit 4 `@Test`.
- **Utility tests use a test-only probe detector.** `UtilityProbeDetector` (test sources only, never registered) visits calls to `probe(label, kind, value)` in test code, runs a `util` helper on the real UAST and reports the result as the message, with a file-only location. This tests the helpers on real Kotlin UAST under every Lint test mode without needing a rule. It replaces the earlier note that utility probes need a registered detector: the real cause was the JUnit 3 naming above.

### Utilities

- **Call matching (`ComposeCalls.isCall`).** A top-level function matches `<package>.<name>`, so the file facade class (`IconKt`) does not matter. A member function matches `<class>.<name>` and a constructor matches the class name. The package form is only used for static (top-level) functions, so a member function named `Icon` in another class never matches `androidx.compose.material3.Icon`.
- **Names come from the resolved declaration.** `ComposeCalls.name` uses the resolved method name, so an import alias (`import ...clickable as click`) still gives `clickable`. Lint's `IMPORT_ALIAS` test mode checks this.
- **Arguments are found through argument mapping.** `argument` and `contentLambda` use `computeArgumentMapping`, so named, positional, reordered and trailing lambda arguments all work. Lint's `REORDER_ARGUMENTS` test mode checks this.
- **Literals are only values written in the source.** `stringLiteralValue` accepts a plain string, or a template made only of literal text. It does not follow constants (`const val`) or variables, so a value that may change at run time is never treated as known. `20.dp` is read as a property access (`20` with the `dp` extension property from `androidx.compose.ui.unit`), not as a call.
- **Modifier chain order.** `ModifierChain.calls` returns calls in source order (innermost receiver first), which is the order Compose applies them. `then(x)` is replaced by the calls of `x`. A local `val` in the same function is followed to its initializer (up to 8 levels). A local `var` is not followed because it may be reassigned. A Kotlin `val` is recognised through `KtProperty.isVar`, because UAST reports Kotlin locals as not final.
- **Semantics assignments.** `semanticsAssignments` returns the property names assigned in `semantics { }` and `clearAndSetSemantics { }` (for example `contentDescription`, `role`) and the calls to functions whose receiver is `SemanticsPropertyReceiver` (for example `heading`, `error`). Assignments inside an `if` in the block count. Nested lambdas, such as the action of a `CustomAccessibilityAction`, are not searched.
- **UI scope.** `isInUiScope` stops at the nearest enclosing function. Lambdas of `remember`, `rememberSaveable`, `derivedStateOf`, `LaunchedEffect`, `DisposableEffect`, `SideEffect`, `produceState`, `launch` and `async` are not UI scope (matched by the resolved function name).
- **Contrast.** `Contrast` uses the WCAG 2.x relative luminance formula (with the 0.03928 threshold from the WCAG definition) and ignores the alpha channel, so transparent colors are treated as opaque. Rules that use contrast must only pass opaque literal colors.

### Sample app and scripts

- Moved the sample app from the template package `com.example.accessibilitychecker` to `org.svu.sedra.a11ylint.sample` (namespace and `applicationId`), as fixed in CLAUDE.md.
- `scripts/check_sample_expectations.py` reads the project's issue IDs from the lint-rules sources by matching `id = "Compose..."`. Every `Issue.create` call must therefore pass the ID as the named argument `id = "..."`. Built-in Lint issues are ignored.
