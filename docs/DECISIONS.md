# Decisions

## 2026-10-01

- Used AGP 9.4.1 plus 23 for the Lint artifact version, resulting in Lint 32.4.1.
- Used JDK 17 for `lint-rules` compilation, matching the project requirement and installed runtime.
- Placed the sample app lint configuration inside `android { lint { } }`, which is the supported DSL location for this AGP version.
- Milestone 0 uses a temporary marker rule only to prove registry loading. It must be removed before Milestone 1.

## 2026-10-03

### P-02 Decorative image labels

- Compare only direct sibling `Text` literals in the same content lambda.
- Matching is case-insensitive and reports equality or containment in either direction.
- Resource calls and variables remain unknown rather than being guessed.

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

### U-05 ComposeTextFieldWithoutLabel

- **Overloads.** All six Material3 1.4.0 text field overloads (`TextField` and `OutlinedTextField` with `String`, `TextFieldValue` or `TextFieldState`) have a nullable `label` parameter, checked in the sources. The rule reads it with `ComposeCalls.argument`, so the named argument fallback also covers a wrong overload resolution.
- **Missing label.** Not passed, or the literal `null`. Any other expression counts as a label, because its value is unknown.
- **Semantics labels are not accepted.** A `contentDescription` in semantics gives a name to screen readers but no visible label. CLAUDE.md asks for the `label` argument, and WCAG 3.3.2 needs a visible label, so such fields are still reported.
- **Message.** The message names the component and mentions the placeholder when one is set, so the developer sees why the placeholder is not enough.
- **Material 2.** `androidx.compose.material.TextField` and `OutlinedTextField` are included; they have the same `label` parameter.

### R-01 ComposeClickableWithoutRole

- **Scope.** Only `clickable` and `combinedClickable`, as in CLAUDE.md. `toggleable` and `selectable` also take a role, but they already expose state and are not part of this rule.
- **Role found.** A `role` argument that is not the literal `null`, or `role` assigned in a semantics block anywhere in the same chain.
- **Custom element.** Any element except a Material clickable component (and `Card`/`Surface` with `onClick`), because those set their own role. A Material `Card` or `Text` made clickable with a modifier is a custom element.
- **O-04 over R-01.** "Button-like" is implemented once in `Clickables.isButtonLikeContainer`, so O-04 can reuse exactly the same test in Milestone 3: the element is a `Box`, `Row` or `Column`, and the direct children of its content lambda are one Text, or one Icon/Image and one Text, ignoring `Spacer`. A fully qualified call (`androidx.compose.material3.Text(...)`) also counts (found by Lint's `FULLY_QUALIFIED` test mode).
- **Earlier sample screens.** R-01 correctly reported 10 custom clickables without a role in the P-01, O-01 and O-03 sample screens. They were given `role = Role.Button` so that each sample screen shows only its own rule, and every good screen stays clean for all rules.
- **Report location.** The name of the click modifier call.

## 2026-10-04

### P-02 fixed: sibling text lookup must unwrap parentheses and qualified calls

The P-02 unit tests passed in isolation, but failed under Lint's default `PARENTHESIZED` test mode: wrapping sub-expressions in parentheses turns a block statement like `Text("Delete")` into a `UParenthesizedExpression`, so the original `is UCallExpression -> expression` match in `directSiblingText` silently stopped matching anything. Fixed by running `Literals.unwrap(...)` on each block statement first, and also handling the fully-qualified-call case (`UQualifiedReferenceExpression`), the same pattern already used in `Clickables.isButtonLikeContainer`. No test mode is disabled; the detector was made to handle the AST shapes Lint's modes already produce.

Also: the P-02 sample screens (`P02BadScreen.kt`, `P02GoodScreen.kt`) originally called `Icon`/`Image` with only `contentDescription`, which does not compile against the real Material3/Foundation signatures (both overloads require `imageVector` or `painter`, neither has a default). Fixed by passing a `painterResource`, matching the P-01 sample pattern. This is a reminder that `./gradlew :sample-app:lintDebug` must be run for every rule, not just the unit tests, per CLAUDE.md section 5.2's note on compiled-library differences.

### P-03 ComposeLowContrastColors

- **Thresholds.** 4.5:1 for normal text and 3:1 for large text (18sp and above, or 14sp and above when bold), exactly the WCAG 2.2 success criterion 1.4.3 values named in CLAUDE.md. `TextStyles.fontSizeSp`/`isBold` (built for U-01) are reused unchanged.
- **Literal color reading.** New `Literals.colorLiteralValue` recognises only the single-argument `androidx.compose.ui.graphics.Color(0x...)` factory call and reads its literal numeric argument, packed as ARGB into an Int. `Color.Black`/`Color.White` (property access, not a call) and the multi-component `Color(r, g, b, a)` overload are deliberately not recognised: CLAUDE.md's wording is specifically "`color = Color(0x...)` literal". `Contrast.ratio` already ignores the alpha channel (see the shared limitation), so the packed value is used as-is.
- **`Modifier.background(color)`'s compiled parameter name is lost, like O-01's `Dp` parameters.** Unit tests (source stubs) passed on the first run, but the sample app first reported only the `Surface(color = ...)` and large-text cases, not `Modifier.background(Color(...))` written positionally (no `color =`). `Color` is a value class, so the compiled parameter loses its name the same way `Dp` does (see "Compiled libraries behave differently from the test stubs"). Fixed by passing `kotlinNames = listOf("color", "shape")` to `ComposeCalls.argument` when reading a `background` call's color, the same fix `ModifierSizes` already uses for size and padding. `Surface`'s `color` is left name-only (not given `kotlinNames`): CLAUDE.md's wording for that case is specifically the named form `Surface(color = Color(...))`, which also works through `ComposeCalls.argument`'s named-argument-in-source fallback regardless of the compiled name, and `Surface`'s parameter position differs between its two overloads (plain vs `onClick`), so a single positional fallback list would not be reliable there.
- **Background lookup is exactly one level.** The Text's own `Modifier.background(...)`, or else the single composable call whose content lambda directly contains the Text (found the same way `O03NestedClickableDetector.enclosingClickable` walks up to a lambda's parent call, but stopping at the first container instead of searching for a match). Only `Surface`/`Surface` (M2/M3) `color = ...` and `Box`/`Row`/`Column` (`Clickables.layoutContainers`) `Modifier.background(...)` are recognised there. A background two levels up, or coming from a modifier passed in as a function parameter, is not found (see LIMITATIONS.md); going further up was judged more likely to guess wrong than to catch a real defect.
- **Message formatting avoids `String.format`.** A platform with a non-English default locale renders `%.1f` with a comma decimal separator, which would make the message (and any test asserting its exact text) locale-dependent. Used the same manual approach as `O01SmallTouchTargetDetector.format` instead: round to the nearest tenth as an integer, then split into whole and tenths parts.
- **Report location.** The `color` argument of the Text, so the underline is on `Color(0x...)`, consistent with P-01 and P-02 reporting at the value that is actually wrong.

### P-04 ComposeTextSizeInDp

- **What counts as the conversion.** A call resolving to `androidx.compose.ui.unit.FontScaling.toSp` (the member extension `Dp.toSp()`, inherited by `Density`; checked in the `ui-unit` 1.10.4 sources), found either directly as the `fontSize` expression or as the last expression of a `kotlin.with(...)` block's trailing lambda. `with(LocalDensity.current) { ... }` is the pattern CLAUDE.md names; the detector does not special-case `LocalDensity` itself, so any `with(someDensity) { x.dp.toSp() }` is recognised, not only the composition-local form.
- **Where `fontSize` is read from.** The Text's own `fontSize` argument, or else the `fontSize` argument of a `TextStyle(...)`/`.copy(...)` passed as `style` (`TextStyles.styleCall`, made non-private for this). Unlike `TextStyles.fontSizeSp` (which falls back to the style's size only when the Text's own size is not a literal sp value, because it reports an *effective* size), P-04 does not fall back once the Text's own `fontSize` is present: a dp conversion written directly on the Text is the defect, regardless of what the style underneath says.
- **New stubs.** `Density`/`FontScaling` (`androidx.compose.ui.unit`) and `CompositionLocal`/`ProvidableCompositionLocal`/`staticCompositionLocalOf` (`androidx.compose.runtime`) plus `LocalDensity` (`androidx.compose.ui.platform`) were added, checked against the `runtime` and `ui-unit` 1.10.4 sources in the Gradle cache. `current`'s real declaration is `@Composable @ReadOnlyComposable inline val`; the stub keeps only `@Composable`, which is enough for the rules that read it.
- **`kotlin.with`.** Matched by `ComposeCalls.isCall(call, "kotlin.with")`, the same top-level-function matching every other rule uses; `with` takes no value-class parameter, so its JVM name is not mangled.

### P-06 ComposeMissingLiveRegion

- **Finding the `mutableStateOf` declaration must use resolution, not text.** The first attempt checked `variable.sourcePsi?.text?.contains("mutableStateOf")`, the same kind of textual check U-02 uses for comparing flip targets. It failed under Lint's `IMPORT_ALIAS` test mode: the mode rewrites the call site to use the aliased name (`remember { IMPORT_ALIAS_2_MUTABLESTATEOF("Loading") }`), so the literal substring is gone even though the call still resolves to the same function. Fixed by walking the variable's initializer (or delegate) expression with a visitor and checking `ComposeCalls.isCall(call, "androidx.compose.runtime.mutableStateOf")` on every call found, which resolves correctly regardless of the name used at the call site. U-02's textual comparison stays safe because it compares two expressions in the same file to each other (both get aliased the same way), not against a hardcoded name.
- **A delegated property's `uastInitializer` is null.** For `var s by remember { mutableStateOf(initial) }`, `ULocalVariable.uastInitializer` is null; only `val s = remember { mutableStateOf(initial) }` (no delegate) sets it. Found by probing both forms directly. The delegate expression is read from the Kotlin PSI instead (`(variable.sourcePsi as? KtProperty)?.delegate?.expression`), then converted with `toUElement()` so the same call-visitor can walk it.
- **Classifying an assignment by its *nearest* enclosing lambda only.** `scope.launch { status = "Saved" }` inside a `Button(onClick = { ... })` must still be flagged: the update happens asynchronously inside `launch`, not synchronously when the user taps. Walking up and stopping at the first enclosing lambda (rather than continuing to the outer `onClick`) gives the right answer; continuing upward would wrongly treat any assignment reachable from a click handler, at any depth, as safe.
- **What counts as a "direct click handler".** A lambda passed as `onClick`, `onCheckedChange` or `onValueChange` (checked by comparing `sourcePsi` against `ComposeCalls.argument(context, call, name)` for each name, which works for a trailing lambda or a named one). CLAUDE.md only names "a direct click handler"; `onCheckedChange`/`onValueChange` are included because they are the same kind of synchronous, user-triggered callback as `onClick` for a toggle instead of a tap, and the alternative (treating them as "indirect") would make every `Switch`/`Checkbox`/`Modifier.toggleable` label a false positive.
- **No assignment found at all means no report.** If the state's setter is never called in the same function (for example, the value comes from a ViewModel, or is only read), there is no static evidence of an asynchronous change, so reporting would be pure noise. Recorded as a limitation rather than a false negative to chase.
- **Only `mutableStateOf` itself.** `mutableStateListOf`, `mutableIntStateOf`, etc. are not recognised, matching CLAUDE.md's wording exactly rather than guessing at every specialised holder.

### O-02 ComposeMissingOnClickLabel

- **Only `Modifier.clickable`, not `combinedClickable`.** CLAUDE.md names only `clickable`. `combinedClickable` has the same `onClickLabel` parameter, but widening the rule was not asked for; revisit with Sedra if the development app run shows `combinedClickable` is common.
- **Container list taken literally.** Exactly `Card`, `Surface`, `Row`, `Box`, `Column`, `ListItem` (Material3 and Material, matching how every other rule treats the two packages the same). `ElevatedCard`/`OutlinedCard` are not included: CLAUDE.md names only `Card`.
- **Retrofitted earlier sample screens.** Once this rule existed, it also reported on `Modifier.clickable` calls built for other rules (O-01, O-03, P-01, R-01, U-02), on both bad and good screens, the same situation R-01 caused earlier (see the 2026-10-03 R-01 note). Each of those was given an `onClickLabel`, the same way they were earlier given a `role` for R-01, so every sample screen still shows only its own rule.

### O-04 ComposeClickableContainer

- **O-04 and R-01 are one decision, split in two.** Both visit the same node (a `clickable`/`combinedClickable` call with no role, on a non-Material element) and both use `Clickables.isButtonLikeContainer`, R-01 to bail out and O-04 to continue. The partition is total and exclusive: button-like content is O-04, anything else is R-01, so the CLAUDE.md 7.1 requirement of one warning per node holds by construction rather than by precedence checks at report time. The helper was written during R-01 for exactly this.
- **Report location is the click modifier, not the container.** The message asks for a `Button`, so the container call would also be a defensible location, but reporting at the click modifier keeps O-04 and R-01 on the same node, which is what makes "exactly one of the two fires" visible in the output and in the sample EXPECT markers.
- **Scope follows R-01, not the narrower CLAUDE.md wording.** CLAUDE.md 7.3 says "clickable `Box`/`Row`/`Column`", while R-01's scope is `clickable` and `combinedClickable`. O-04 takes both, because any node R-01 hands over must be picked up by O-04; covering only `clickable` would leave a `combinedClickable` button-like container reported by neither rule.
- **Retrofitted earlier sample screens again.** O-04 reported on eight role-less clickables in the O-03, P-01 and U-02 screens (bad and good). Each was given a role, the same retrofit R-01 and O-02 needed. In `U02BadScreen` the roles chosen are the semantically correct ones (`Role.Button`, `Role.Checkbox`, `Role.Switch`), which also makes that screen a sharper U-02 example: the role is set, but the on or off state still is not.

### O-05 ComposeEmptyClickable

- **The P-01 overlap was already decided, and is followed exactly.** CLAUDE.md 7.3 reads "no Text, no Icon/Image *with a label*", which taken alone would make O-05 report an unlabelled icon inside a clickable. The overlap policy (7.1) and the decision recorded when P-01 was built say the opposite: that case is P-01's. So O-05 skips a clickable as soon as its content holds any Icon or Image, and only reports when there is no Icon, no Image and no Text. The recorded decision wins over the looser wording.
- **A clickable Icon, Image or Text is skipped outright.** `Clickables.hasAccessibleName` looks at an element's *content*, not at the element itself, so a labelled `Icon(..., Modifier.clickable { })` would otherwise be reported as empty. Such an element is its own content: labelled is fine, unlabelled is P-01.
- **"Another composable" is detected as `@Composable` returning Unit.** The exclusion CLAUDE.md asks for cannot simply be "any `@Composable` call": `remember` and `stringResource` are composable too and emit nothing. Only a composable that returns Unit emits UI, so that is the test, minus the few whose content the rule reads itself (`Box`, `Row`, `Column`, `Text`, `Icon`, `Image`, `Spacer`, `Canvas`).
- **`onClickLabel` is not a name**, consistent with the P-01 decision: it describes the action ("Activate to open"), not the element. The sample screens rely on this, setting `onClickLabel` and `role` to keep O-02 and R-01 quiet while O-05 still reports.
- **No sample retrofit was needed.** Unlike O-02 and O-04, O-05 reported nothing on the earlier screens: the overlap with P-01 was designed in advance, and every existing clickable already has a Text or a labelled Icon.

### U-03 ComposeMissingSemanticError

**Answer to the question CLAUDE.md asks first: yes, Material already sets an `error(...)` semantic for `isError = true`, but only a generic one.** Checked in the Material3 1.4.0 and Material 1.10.4 sources in the Gradle cache (`internal/TextFieldImpl.kt` in both):

```kotlin
// Developers need to handle invalid input manually. But since we don't provide an error message
// slot API, we can set the default error message in case developers forget about it.
internal fun Modifier.defaultErrorSemantics(
    isError: Boolean,
    defaultErrorMessage: String,
): Modifier = if (isError) semantics { error(defaultErrorMessage) } else this
```

Every `TextField`, `OutlinedTextField` and `SecureTextField` overload in both libraries applies it with `getString(Strings.DefaultErrorMessage)`, which on Android is `R.string.default_error_message` ("Error"). So the defect U-03 reports is not a missing error semantic, it is an error state with no message that says what is actually wrong, which is exactly how CLAUDE.md 7.3 words it ("the user only hears a generic message, or nothing").

- **What counts as a message.** A `supportingText` slot that is not the literal `null`, or `error(...)` set in the field's own semantics. Both are the places a specific message can live; `label` and `placeholder` are not, because they describe the field rather than the problem.
- **What counts as an error state.** An `isError` argument that is passed and is not the literal `false`. A variable or a call (`isError = email.isBlank()`) counts, because its value is unknown and the field is clearly meant to have an error state.
- **Report location.** The `isError` argument, so the underline is on the expression that turns the error state on, consistent with P-01 (reports at `contentDescription`) and P-03 (reports at `color`).
- **Scope.** `TextField` and `OutlinedTextField` in both Material and Material3, the same four names U-05 uses. `SecureTextField` behaves identically in the sources but is not named in CLAUDE.md, so it is left out.

### U-04 ComposeVagueButtonLabel

- **The word list is exactly CLAUDE.md's**, kept in one constant (`VAGUE_LABELS`) so the thesis and the rule cannot drift apart. Compared after `trim()` and `lowercase()`, so `"  DONE  "` matches.
- **The whole label must be vague, not contain a vague word.** The comparison is on the entire trimmed label, so "Read more about shipping" is clean while "more" is reported. Matching substrings would flag most real labels.
- **Exactly one Text.** CLAUDE.md says "whose only Text literal", so a button with two Texts, or with none, is left alone: the readable label is then more than the one literal, and judging it from that literal would be guessing. A non-literal text (`stringResource`, a variable) is unknown for the same reason.
- **`contentDescription` in the button's semantics wins.** It replaces the visible label for screen readers, which is a legitimate way to keep a short visible label with a full spoken one, and is the "contentDescription override" CLAUDE.md asks to ignore.
- **Report location.** The `text` argument of the Text, so the underline is on the label itself rather than on the button.

### R-02 ComposeClearAndSetSemanticsLoss

- **An empty block is always reported, as CLAUDE.md 7.3 asks.** Question for Sedra: an empty `clearAndSetSemantics { }` is also the documented way to hide a purely decorative subtree from screen readers, so this will report that legitimate use as well. The alternative is to report an empty block only when the content had a name or was interactive, which would keep the deliberate "hide this decoration" case clean. The rule follows the literal wording for now, because narrowing it is a change to the taxonomy's intent.
- **What counts as a name to put back:** `contentDescription` or `text`. What counts as a role or state: `role`, `stateDescription`, `toggleableState` or `selected`. Any one of a group is enough, because the block only has to carry the information forward, not mirror the original properties exactly.
- **What the content "had".** A Text with non-empty text, or a labelled Icon or Image, counts as a name. Being clickable (the usual `Clickables.isClickableElement` test, on the element or anything inside it) or holding a `Checkbox`, `TriStateCheckbox`, `Switch` or `RadioButton` counts as role and state. The Material state components are included because CLAUDE.md says "or had state", and those carry state without any click modifier.
- **One report per block, naming what was lost**, rather than one per missing property, so a block that drops both the name and the role gives a single warning that says so.

### Publishing `lint-library` to mavenLocal (two bugs, both fixed)

The module applied `maven-publish` and set `group` and `version`, but `publishToMavenLocal`
produced nothing usable. Two separate problems:

1. **No publication.** `maven-publish` needs a publication and an Android component to publish.
   Added `android { publishing { singleVariant("release") } }` and a `MavenPublication` named
   `release` with `artifactId = "a11ylint"`, wired with `afterEvaluate { from(components["release"]) }`
   because the Android component does not exist until after evaluation.
2. **More than one jar in `lintPublish`.** With the publication in place the build failed with
   "Found more than one jar in the 'lintPublish' configuration". The extra jars were
   `kotlin-stdlib` and its transitive `annotations`, which the Kotlin JVM plugin adds to
   `lint-rules` automatically; `lintPublish` accepts exactly one file. Fixed with
   `lintPublish(project(":lint-rules")) { isTransitive = false }`, which is also correct at
   runtime: Lint runs the rules in its own classloader and already provides the Kotlin stdlib,
   the same reason `lint-api` and `lint-checks` are `compileOnly`.

Verified by unpacking the published artifact rather than trusting the build result:
`~/.m2/repository/org/svu/sedra/a11ylint/0.1.0/a11ylint-0.1.0.aar` contains `lint.jar`, whose
manifest holds `Lint-Registry-v2: org.svu.sedra.a11ylint.ComposeA11yIssueRegistry` and which
carries all 18 detector classes. The coordinates are the ones CLAUDE.md fixes,
`org.svu.sedra:a11ylint:0.1.0`.

## 2026-10-06

### P-05 ComposeTextOverImage

- **`Image` only, not `Icon`.** The first version matched `Clickables.imageCalls`, which also
  holds `Icon`, and the sample app immediately caught it: the O-04 bad screen has a `Box` with an
  `Icon` and a `Text`, an icon-and-label button, which P-05 reported as text over a picture.
  CLAUDE.md 7.4 says `Image`, and an `Icon` is a small tinted symbol rather than a photo, so the
  rule now matches `androidx.compose.foundation.Image` alone. A good example of the sample app
  earning its keep: the unit tests all passed before this was found.
- **Source order is the stacking order.** A `Box` draws its children in the order they are
  written, so only a Text written *after* the Image is on top of it. A Text before the Image is
  underneath and is not reported.
- **One warning per Box.** Only the first Text above the Image is reported. Reporting every Text
  would turn one design decision into several warnings.
- **What counts as protection.** A `Modifier.background(...)` on the Text itself, or on any child
  between the Image and the Text, which is how a scrim is normally written. A scrim drawn with
  `drawBehind` or a gradient `Brush` is not recognised (recorded in LIMITATIONS).

### Two rules tuned after the development app run (agreed with Sedra)

The first run against code we did not write produced 36 findings on JetNews and Jetchat, of
which three were confirmed false positives from two rule defects. Both are now fixed. The
evidence, and the counts before and after, are in `DEV_APP_RESULTS.md`.

**O-05: the clickable element may itself be a composable the rule cannot read.** The rule already
skipped a clickable whose *content* calls an unknown composable, because that composable may
supply the name. It did not apply the same reasoning to the element itself, and a custom
composable has no content lambda to walk, so the walk found nothing and the rule concluded there
was nothing to read. Both of its development app findings were of exactly this shape:

```kotlin
PostCardTop(post = post, modifier = Modifier.clickable { navigateToPost(post.id) })   // renders the title
JetchatIcon(contentDescription = "...", modifier = Modifier.size(64.dp).clickable { }) // has its own label
```

Fixed by applying the existing `emitsUnknownUi` test to the element as well as to its content.
The set of composables the rule can read was renamed `READABLE_COMPOSABLES` and widened to
include the Material clickable components and containers, because their content lambdas *are*
walked: without that, an empty `IconButton` would have been skipped as "unknown" and the rule
would have lost a true positive.

**R-02: an empty block only loses what the content actually provided.** CLAUDE.md asks to flag
every empty `clearAndSetSemantics { }`, and the open question recorded against R-02 asked whether
that was too broad. The development app run answered it with Google's own code:

```kotlin
BookmarkButton(
    isBookmarked = isFavorite,
    onClick = onToggleFavorite,
    // Remove button semantics so action can be handled at row level
    modifier = Modifier.clearAndSetSemantics {}.padding(vertical = 2.dp, horizontal = 6.dp),
)
```

The empty block is deliberate and correct: the row above offers the action as a custom
accessibility action, so the inner button is silenced on purpose. The rule now computes what the
content provided first, and reports an empty block only when there was a name or an interaction
to lose. The message says which, so an empty block reads "is empty, so it hides the name of its
content from screen readers" instead of a generic sentence.

**This narrows the taxonomy wording for R-02**, which said "that is empty" without qualification.
The thesis text should say that an empty block is reported when it hides a name or an
interaction, so that deliberately hiding decoration stays clean.

**Verification.** Both fixes were checked in all three directions: the unit tests (with a new
regression test each, written from the real code above), the sample app expectation check, which
stayed at 64 expected and 64 reported with 0 missing and 0 unexpected, and a re-run of both
development apps, where the three false positives disappeared and every other finding stayed.

### R-06 ComposableWithoutSemantics

- **The narrowing CLAUDE.md asks to record.** The taxonomy wording is "a composable that contains clickable or toggleable UI and sets no semantics". The rule as built reports only **custom gestures** with no semantics, and deliberately ignores `clickable`/`toggleable`, because those already add an action and a role of their own and are covered by R-01, O-02 and U-02. The real defect is the gesture that gives TalkBack nothing at all. **The thesis taxonomy text for R-06 should be updated to say "custom gestures" instead of "clickable or toggleable UI".**
- **`swipeable` and `anchoredDraggable` are not matched.** CLAUDE.md names both, but neither is public API in the versions this project builds against, checked in the sources: `swipeable` exists only in Material 2 and its whole API is annotated `@Deprecated(SwipeableDeprecation)`, and `anchoredDraggable` is `androidx.compose.material3.internal.anchoredDraggable`, that is internal, with `draggableAnchors` and `AnchoredDraggableState` as the public surface. Rather than invent a fully qualified name that matches nothing, they are left out and recorded here. `draggable2D` and the whole `detect*Gestures` family are included instead, which covers the same kind of defect with names that exist.
- **Reported on the function declaration**, as CLAUDE.md asks, rather than on the gesture modifier: the fix is a property of the component as a whole, and one report per composable avoids several warnings for a function with more than one gesture.
- **Per function, no call following.** A gesture in one composable and semantics applied by its caller are not connected (recorded in LIMITATIONS), which matches how every other rule in this project stops at the enclosing function.

### R-03: the premise does not hold in Compose 1.10.4 (the check CLAUDE.md asks for first)

CLAUDE.md 7.3 asks to verify how Compose treats interactive children inside
`semantics(mergeDescendants = true)` before writing R-03. **The answer is that it does not hide
them, so the defect R-03 describes does not exist in Compose 1.10.4.** Three places in the
sources agree (Layout Inspector was not available in this environment, so this is a source
reading rather than a device check):

1. `ui/semantics/SemanticsNode.kt`, `mergeConfig`:

   ```kotlin
   // Don't merge children that themselves merge all their descendants (because that
   // indicates they're independently screen-reader-focusable).
   if (!child.isMergingSemanticsOfDescendants) {
       mergedConfig.mergeChild(child.unmergedConfig)
       child.mergeConfig(unmergedChildren, mergedConfig)
   }
   ```

2. The KDoc of `Modifier.semantics` and of `SemanticsModifierNode`: "descendant nodes (except
   those themselves marked [mergeDescendants]) will disappear from the tree".

3. `foundation/Clickable.kt`, `AbstractClickableNode`, the base of `clickable`,
   `combinedClickable`, `toggleable` and `selectable`:

   ```kotlin
   final override val shouldMergeDescendantSemantics: Boolean
       get() = true
   ```

Put together: every interactive child is a merging root of its own, and merging deliberately
stops at merging roots, so a `Button`, `IconButton`, `Checkbox`, `Switch` or any element with a
click or toggle modifier stays independently focusable inside a merged container. Material
buttons are built on the same clickable modifiers, so they behave the same way.

A rule written to CLAUDE.md's wording would therefore report a non-defect on every merged
container that happens to hold a button, which would hurt the precision figure the thesis
measures. The modifier that genuinely does remove interactive children is
`clearAndSetSemantics`, which R-02 already covers.

**Decision (Sedra, 2026-10-05): R-03 is dropped and recorded as not applicable.** No detector is
written and no taxonomy entry is registered, so the implemented taxonomy is 26 rules plus R-03
documented as void. The alternatives considered were re-aiming R-03 at over-merging (a merged
container whose many Texts become one long announcement), which would have needed new taxonomy
wording, and implementing it as written and filtering the false positives in Phase 3, which
would have cost precision in the evaluation.

For the thesis this is a result rather than a gap: the taxonomy was drafted from the WCAG
mapping and from how merging is commonly described, and checking it against the framework
showed that this particular defect cannot occur in Compose. If a future Compose version stops
treating clickable nodes as merging roots, the rule becomes meaningful again, and the check to
repeat is `AbstractClickableNode.shouldMergeDescendantSemantics`.

### `ComposeCalls.argument` now reads the source name first (found while building U-03)

U-03's unit tests all passed, but on the sample app the `Modifier.semantics { error("...") }` field was still reported. A probe put into the message showed why: for the compiled Material3 `TextField`, `argument(context, call, "modifier")` returned the **`onValueChange`** argument.

This is the wrong-overload problem recorded for `Card(onClick = ...)` above, in a worse form. Lint resolves a `TextField(value = ..., onValueChange = ...)` call to the `TextFieldState` overload, whose parameters are shifted by one (`state, modifier, enabled, ...` instead of `value, onValueChange, modifier, ...`). Asking for the parameter named `modifier` therefore finds a real parameter at the wrong index, and `computeArgumentMapping` hands back the argument written in that position. The existing fallback did not help, because it only ran when the mapping returned **null**, and here it returned a wrong but non-null value.

**Fix:** `argument` now tries `namedArgumentInSource` *before* the mapping. When the source writes `name = ...`, the Kotlin compiler has already matched that name against the overload that really applies, so the source name is the more trustworthy of the two. Positional arguments are unchanged and still go through the mapping (with the `kotlinNames` fallback for value class parameters).

This is a shared utility every rule uses, so it was checked with the full unit suite and the full sample app expectation check, which stayed at 0 missing and 0 unexpected. It also silently fixes any rule that reads a named argument from a mis-resolved overload; U-05 was reading `label` from the same shifted mapping and only happened to reach the right verdict.
