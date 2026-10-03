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

### Compiled libraries behave differently from the test stubs (found while building P-01)

The P-01 unit tests passed, but the first run on the sample app reported nothing. Two differences between the source stubs and the real compiled Compose libraries were the cause. Both are now handled in `ComposeCalls`:

- **JVM name mangling.** Kotlin changes the JVM name of a function that takes a value class parameter. The compiled Material3 `Icon` takes `tint: Color` (a value class), so Lint sees it as `Icon-ww6aTOc`. `clickable` (`role: Role?`) and semantics setters such as `role` are mangled the same way. `ComposeCalls.declaredName` removes everything from the first `-`, which is safe because a Kotlin identifier cannot contain `-`. All resolved names go through it.
- **Wrong overload for `Card(onClick = ...)`.** Against the compiled Material3 1.4.0 library, UAST resolves `Card(onClick = onAction) { ... }` to the non-clickable `Card(modifier, shape, ...)` overload, which has no `onClick` parameter, so argument mapping cannot find `onClick`. `ComposeCalls.argument` now falls back to an argument written by name in the Kotlin source when the mapping finds nothing.
- **Lost parameter names (found while building O-01).** In the compiled library, a parameter whose type is a value class loses its name: `Modifier.size(size: Dp)` appears as `size-3ABfNKs($this$size..., p: float)`. `ComposeCalls.argument` takes an optional list of the Kotlin parameter names of the resolved overload. When the compiled parameter at that position has a synthetic name (`p`, `p0`, ...), it is matched by position, still through Lint's argument mapping, so named and reordered arguments keep working. `ModifierSizes` supplies the names for the size and padding overloads, chosen by the number of parameters. Rules that read other value class parameters (for example `fontSize: TextUnit` in U-01) must do the same.
- **Consequence for testing.** Unit tests with source stubs cannot show these problems. The sample app run (`lintDebug` plus `check_sample_expectations.py`) is the check against the real libraries, so every rule must pass it before it is done.

### Issue creation

- `taxonomy/A11yIssues.create` builds every issue: category `A11Y`, and severity and Lint priority derived from the taxonomy priority (Critical: error, 9; Major: warning, 6; Minor: warning, 3). `TaxonomyTest` checks that every taxonomy entry matches its issue and that the registry contains every taxonomy issue.

### P-01 ComposeMissingContentDescription

- **What counts as the clickable parent.** The detector walks up from the Icon or Image through the enclosing lambdas until it reaches the enclosing function. The first call whose content lambda contains the icon and that is a clickable element (Material button, icon button, FAB, `Card`/`Surface` with `onClick` or `onCheckedChange`, or any composable with a click or toggle modifier) is the parent. Layout calls such as `Row` and `Box` in between are passed through.
- **What counts as another name.** A `Text` (Material3, Material or `BasicText`) whose text is not `""`, another `Icon`/`Image` whose `contentDescription` is not `null` or `""` (a variable counts), or `contentDescription`/`text` set in semantics on the parent or on any element inside it. The whole content of the parent is searched, including nested layouts.
- **Own clickable modifier.** An Icon with a click or toggle modifier is always reported when unlabelled, because it is its own clickable element. `onClickLabel` does not count as a name: it describes the action ("Activate to ..."), not the element.
- **Report location.** The rule reports at the `contentDescription` argument, so the underline is on `null` or `""`. Phase 3 can slice the code around this line.
- **Several unlabelled icons in one clickable.** Each one is reported, because each is a possible label.
- **Overlap with O-05.** CLAUDE.md says "a clickable with no readable content at all is O-05; an Icon or Image inside a clickable with a missing label is P-01". So when a clickable contains an unlabelled Icon or Image, P-01 reports it, and O-05 must skip that clickable. O-05 will only report clickables that contain no Icon or Image at all and no Text.
- **Test mode note.** Lint's `REORDER_ARGUMENTS` test mode cannot rewrite a call that has a trailing comma after the last named argument and is followed by a trailing lambda (it produces invalid code). One negative test therefore writes that call without the trailing comma. The detector is not affected.
- **Sample app icons.** The sample screens use `painterResource(R.drawable.ic_launcher_foreground)` instead of `Icons.Filled.*`, because Material3 1.4.0 does not depend on the material icons library and adding it would be a new dependency.

### O-01 ComposeSmallTouchTarget

- **Threshold.** 48dp, the Android and Material guideline that CLAUDE.md names, not the 24 CSS pixels of WCAG 2.5.8. An element is reported when its width or its height is below 48dp. One known small axis is enough; the other axis may be unknown.
- **Modifier order.** Compose applies modifiers from the outside in, so the clickable area is decided by the size modifiers around the click modifier:
  - A size before the click modifier (`Modifier.size(20.dp).clickable { }`) is the clickable size. Padding between that size and the click modifier is inside the size but outside the clickable area, so it is subtracted (`size(48.dp).padding(8.dp).clickable { }` gives 32dp).
  - When there is no size before it, the first size after it (`Modifier.clickable { }.size(20.dp)`) is the content size, and padding between the click modifier and that size is inside the clickable area, so it is added (`clickable { }.padding(14.dp).size(20.dp)` gives 48dp).
  - Padding before an outer size, or after an inner size, does not change the clickable area and is ignored.
  - The search on each side stops at the first modifier that sets the axis to something unknown (`fillMaxWidth`, `widthIn`, `sizeIn`, `defaultMinSize`, `wrapContentSize`, `aspectRatio`, `weight`, or a non-literal size or padding). An unknown axis is never reported.
- **`minimumInteractiveComponentSize()`.** Its presence anywhere in the chain suppresses the warning. In Compose it only helps when it comes before the size, but treating any position as a fix avoids false positives and the developer clearly addressed the problem. Recorded as a limitation.
- **Material components.** A chain passed directly as an argument to a Material button, icon button, FAB, or `Card`/`Surface` with `onClick` is ignored, because these components add `minimumInteractiveComponentSize()` themselves (checked in the Material3 1.4.0 sources: `IconButton`, `Button` and the clickable `Surface` all apply it).
- **One report per chain.** Only the first click or toggle modifier in a chain is checked, so `Modifier.size(20.dp).clickable { }.toggleable(...)` gives one warning.
- **Report location.** The size call that decided the first small axis, without its receiver (`getCallLocation(includeReceiver = false)`), so the underline is on `size(20.dp)` and not on the whole chain.

### O-03 ComposeNestedClickable

- **Direction of the search.** The detector visits every clickable element and looks upwards for the nearest clickable element whose content lambda contains it. Searching upwards gives one report per inner element and handles any depth of layout nesting.
- **Reported element.** The inner element is reported, not the outer one: the inner element is the one that is hard to reach. A card with two icon buttons gives two warnings.
- **Same definition of "clickable" as P-01** (`Clickables.isClickableElement`), so the rules stay consistent.
- **Selection controls are not included.** CLAUDE.md lists `Modifier.clickable`, `Button`, `IconButton` and `Card(onClick)`. A `Checkbox` or `Switch` with a callback inside a clickable `Row` is a closely related defect (the usual fix is `Modifier.toggleable` on the row and `onCheckedChange = null` on the checkbox), but it is not in the spec and adding it could raise the false positive rate. Left out for now. Question for Sedra.
- **Overlap with O-07.** O-07 (Minor, Milestone 4) will report a clickable container that has two or more clickable children and no `customActions`. That warning is on the container, while O-03 warns on each child, so they are different nodes. If this gives too many warnings for one card, the precedence will be decided with Sedra when O-07 is built.
- **Element without a modifier argument.** A clickable modifier in a local `val` that is passed to a container (`Box(clickableModifier)`) is followed through `ModifierChain`, so the container is still recognised as clickable.

### U-01 ComposeMissingHeading

- **Material3 top app bars do not mark their title as a heading.** Checked in the Material3 1.4.0 sources (`AppBar.kt`): the title slot gets `isTraversalGroup` and is hidden from accessibility while the bar collapses, but `heading()` is never set anywhere in Material3. CLAUDE.md asks to ignore `TopAppBar` titles, so the rule ignores Text inside any top app bar, but this means a real heading is not reported there. Question for Sedra: keep ignoring them, or report them (the title is usually the main heading of the screen)?
- **Heading styles.** The Material 3 styles from CLAUDE.md (`display*`, `headline*`, `titleLarge`). Material 2 `h1` to `h6` are added because they are the Material 2 equivalents. `titleMedium` and `titleSmall` are not included: Material uses them for list item titles and card titles, which are usually not headings.
- **Bold.** "Bold" means a weight of 700 or more, the CSS and WCAG meaning. `SemiBold` (600) does not count.
- **20sp threshold.** As in CLAUDE.md. Kept as the constant `MIN_HEADING_SP`.
- **Where the style is read.** The `style` argument must be a direct `MaterialTheme.typography.<style>` expression (optionally with `.copy(...)`), checked by resolving the property to the `androidx.compose.material3` or `androidx.compose.material` package. Size and weight are read from the Text arguments first and then from a `TextStyle(...)` or `.copy(...)` passed as `style`.
- **Ignored containers.** Text inside a Material clickable component (the same list as P-01 and O-03) or a top app bar, anywhere above it in the same function.
- **Report location.** The name of the `Text` call.
- **Getter names.** A property resolved through its getter gives a lowercase name (`FontWeight.Bold` resolves to `getBold`, which gives `bold`), so font weight names are compared without case.

### U-02 ComposeMissingStateDescription

- **What counts as a flip.** Three patterns, chosen from the CLAUDE.md wording: an assignment whose right side is the negation of its left side (`x = !x`, `x.value = !x.value`), compared by source text without whitespace and parentheses; and a call to a function whose name matches `on...Change` or `on...Changed` with a negated argument (`onCheckedChange(!checked)`). The whole click lambda is searched, including `if` branches.
- **What counts as exposed state.** A `toggleable`, `triStateToggleable` or `selectable` modifier in the same chain, or `stateDescription`, `toggleableState` or `selected` in its semantics. The whole chain is checked, so the state modifier or semantics may come before or after the click modifier.
- **Material components.** Only modifier-based clicks are checked, as in CLAUDE.md. A Material `IconButton` whose `onClick` flips a Boolean has the same problem (the fix is `IconToggleButton`), but it is not reported. A chain passed to a Material clickable component is also skipped.
- **Report location.** The name of the `clickable` call, because the click handler is where the toggle happens.
- **Shared code.** The code that finds the whole modifier chain around a modifier call (`outermostExpression`) and the element it is passed to (`receivingElement`) moved from O-01 into `ModifierChain`, so O-01 and U-02 use the same logic.
