# CLAUDE.md: Compose Accessibility Lint (Thesis Phase 2)

> **For Sedra (read first, then you can delete this box):**
> 1. In Android Studio, create a new project with the **Empty Activity (Compose)** template, named `compose-a11y-lint`, package `org.svu.sedra.a11ylint.sample`, minimum SDK 24. This gives you the Gradle wrapper and a correct AGP version.
> 2. Rename the generated `app` module folder to `sample-app` (or let Claude Code do it in Milestone 0).
> 3. Put this file in the project root as `CLAUDE.md`, open a terminal in that folder and run `claude`.
> 4. Start with: *"Read CLAUDE.md and do Milestone 0. Stop and report when it is done."*
> 5. Go one milestone at a time and review the report before continuing.

---

## 1. Project context

This repository is the implementation for a master's thesis at the Syrian Virtual University:

**"An AI-Assisted Semantic Accessibility Analysis Framework for Jetpack Compose Applications"**
Student: Sedra Merkhan. Supervisor: Dr. Mazen Mustafa.

The framework finds **semantic accessibility defects in Jetpack Compose source code** while the developer is writing it. It has four layers:

| Layer | Name | What it does | Phase |
|---|---|---|---|
| 1 | Static rule engine | Custom Android Lint rules detect defects in Kotlin UAST and report class, file, line and severity | **Phase 2 (now)** |
| 2 | Semantic context | Extracts a small code slice around each defect for the LLM | Phase 3 |
| 3 | Grounded LLM reasoning | Explains the defect, infers intent, recommends a fix in plain language | Phase 3 |
| 4 | Developer feedback | Shows enriched warnings in Android Studio | Phase 3 |

**Your job in this phase is Layer 1 only: the Lint rules.**

### Hard rules for this phase

- **No LLM code, no network calls, no API keys.** Layer 1 must be fully deterministic and work offline. The same code must always produce the same warnings.
- **No automatic code fixes.** Do not add `LintFix` quick fixes that rewrite code. The thesis scope is detection and explanation only. (A rule may *describe* the fix in its explanation text.)
- **Never invent Compose APIs.** Before writing a detector or a stub, check the real fully qualified name and signature in the androidx sources (for example on cs.android.com or in the Gradle cache). Wrong package names make detectors silently match nothing.
- **Never use the evaluation apps.** See section 9. Only the sample app and the two development apps may be used while building rules.
- **Ask Sedra before** changing the taxonomy, changing a severity, adding a dependency that is not listed here, or dropping a rule.
- **Write things down.** Every heuristic, every known limitation and every design choice goes into the docs files (section 10). The thesis chapters are written from these notes.

---

## 2. Tech stack and versions

Do not hard-code versions from memory. Detect what is installed and align with it.

1. Read the AGP version from the project that Android Studio generated (`gradle/libs.versions.toml` or the root `build.gradle.kts`).
2. **Lint version = AGP version + 23.** Example: AGP `8.7.2` goes with Lint `31.7.2`. Use the same Lint version for `lint-api`, `lint-checks` and `lint-tests`.
3. JDK 17 or newer (`java -version`). Use a Gradle toolchain of 17 for the lint module.
4. **Kotlin version for `lint-rules`:** Lint embeds its own Kotlin compiler. If the rules are compiled with a newer Kotlin than Lint supports, Lint can fail to load them. Keep the Kotlin plugin aligned with the project, and if loading fails, set `apiVersion` and `languageVersion` in `lint-rules` to the Kotlin version Lint bundles. Record what you chose in `docs/DECISIONS.md`.
5. JUnit 4 for tests (what `lint-tests` expects).

Add all versions to `gradle/libs.versions.toml`.

---

## 3. Repository layout

```
compose-a11y-lint/
├── CLAUDE.md
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/libs.versions.toml
├── lint-rules/                         # Kotlin/JVM module (NOT an Android module)
│   ├── build.gradle.kts
│   └── src/
│       ├── main/kotlin/org/svu/sedra/a11ylint/
│       │   ├── ComposeA11yIssueRegistry.kt
│       │   ├── taxonomy/               # TaxonomyEntry, DetectionType, Pour, Priority, Taxonomy.kt
│       │   ├── util/                   # ComposeCalls.kt, ModifierChain.kt, UiScope.kt, Literals.kt, Contrast.kt
│       │   └── detectors/
│       │       ├── perceivable/        # P01..P07
│       │       ├── operable/           # O01..O07
│       │       ├── understandable/     # U01..U07
│       │       └── robust/             # R01..R06
│       └── test/kotlin/org/svu/sedra/a11ylint/
│           ├── stubs/ComposeStubs.kt   # fake Compose APIs for tests
│           ├── testing/A11yLintTest.kt # base class
│           └── detectors/...           # one test class per detector
├── lint-library/                       # Android library that packages the rules as an AAR
│   └── build.gradle.kts                # uses lintPublish(project(":lint-rules"))
├── sample-app/                         # Compose app with deliberately good and bad screens
│   └── src/main/java/org/svu/sedra/a11ylint/sample/defects/p01/...
├── scripts/
│   ├── check_sample_expectations.py    # compares lint output with // EXPECT markers
│   └── measure_lint_time.sh            # build overhead measurement (Plan section 6.5)
└── docs/
    ├── RULES.md                        # rule catalog (one section per rule)
    ├── DECISIONS.md                    # design choices and heuristics, dated
    ├── LIMITATIONS.md                  # what static analysis cannot catch (feeds RQ1)
    ├── PROGRESS.md                     # milestone log
    └── DEV_APP_RESULTS.md              # findings on the development apps
```

**Package names (fixed by Sedra):**
- Lint rules: `org.svu.sedra.a11ylint` (subpackages `taxonomy`, `util`, `detectors.perceivable`, `detectors.operable`, `detectors.understandable`, `detectors.robust`)
- Sample app: `org.svu.sedra.a11ylint.sample`
- Lint library (AAR): namespace `org.svu.sedra.a11ylint.library`; Maven coordinates `org.svu.sedra:a11ylint:<version>`

Use these names everywhere. The `Lint-Registry-v2` manifest entry must match the registry's full class name exactly, otherwise Lint silently loads no rules.

---

## 4. Module setup details

### `lint-rules/build.gradle.kts`

- Plugin: `org.jetbrains.kotlin.jvm` only.
- Dependencies:
  - `compileOnly(lint-api)`, `compileOnly(lint-checks)`
  - `testImplementation(lint-tests)`, `testImplementation(lint-api)`, `testImplementation(lint-checks)`, `testImplementation(junit)`
- Register the issue registry in the jar manifest:
  ```kotlin
  tasks.jar {
      manifest { attributes("Lint-Registry-v2" to "org.svu.sedra.a11ylint.ComposeA11yIssueRegistry") }
  }
  ```

### `ComposeA11yIssueRegistry`

- `issues` = every issue from `Taxonomy`.
- `api = CURRENT_API`; choose a sensible `minApi` for the Lint version in use and note it in DECISIONS.
- `vendor = Vendor(vendorName = "Compose A11y Lint (SVU thesis)", identifier = "compose-a11y-lint", feedbackUrl = "<repo url or placeholder>")`.

### `sample-app`

- Add `lintChecks(project(":lint-rules"))`.
- In the `lint { }` block: `sarifReport = true`, `xmlReport = true`, `abortOnError = false`, `checkDependencies = false`.

### `lint-library`

- Android library module with `lintPublish(project(":lint-rules"))` and `maven-publish`, so it can be published to `mavenLocal()` and used by the development apps without copying code.

---

## 5. Shared infrastructure (build this before the rules)

### 5.1 Taxonomy metadata (`taxonomy/`)

```kotlin
enum class Pour { PERCEIVABLE, OPERABLE, UNDERSTANDABLE, ROBUST }
enum class DetectionType { STATIC, STATIC_LLM }
enum class Priority { CRITICAL, MAJOR, MINOR }

data class TaxonomyEntry(
    val id: String,            // "P-01"
    val issue: Issue,          // the Lint Issue
    val pour: Pour,
    val wcag: List<String>,    // ["1.1.1"]
    val detection: DetectionType,
    val priority: Priority,
)
```

`Taxonomy.kt` holds the list of all 27 entries (section 7). The registry reads from it. Phase 3 will use it to map Lint results back to taxonomy IDs.

### 5.2 Issue conventions

- **Issue ID:** as listed in section 7 (all start with `Compose`, to avoid clashing with built-in IDs such as `ContentDescription`).
- **Category:** `Category.A11Y`.
- **Severity and priority from the taxonomy priority:**
  | Taxonomy priority | Lint severity | Lint priority |
  |---|---|---|
  | Critical | `Severity.ERROR` | 9 |
  | Major | `Severity.WARNING` | 6 |
  | Minor | `Severity.WARNING` | 3 |
- **Scope:** `Implementation(Detector::class.java, Scope.JAVA_FILE_SCOPE)` (this covers Kotlin), except U-06 (see section 7).
- **Message format** (the message shown at the line):
  `"[P-01] Interactive Icon has no contentDescription, so screen readers announce it without a name"`
  The `[ID]` prefix is required. Phase 3 parses it.
- **STATIC_LLM rules report candidates.** Their message starts with `"[ID] Possible ..."` because an intent judgment is still needed. They are still always reported in Phase 2. Never hide a warning based on guesswork.
- **Explanation text:** plain English, 2 to 4 sentences: what is wrong, who it affects, the WCAG 2.2 success criterion, and what a correct version looks like. Example:
  > Icons and images that respond to clicks need a text label. Without one, TalkBack only says "button" and the user cannot tell what it does. This relates to WCAG 2.2 success criterion 1.1.1 (Non-text Content). Give the Icon a contentDescription that describes the action, for example "Delete draft".

### 5.3 Utilities (`util/`)

1. **`ComposeCalls.kt`**
   - `isComposableCall(call)`: resolve the called method and check for the `androidx.compose.runtime.Composable` annotation.
   - `isCall(call, fqName)`: compare the resolved method's containing class or file facade with a fully qualified name, for example `androidx.compose.material3.Icon`.
   - `argument(context, call, name)`: get a named argument using `context.evaluator.computeArgumentMapping(call, method)`, so that both named and positional arguments work.
   - `contentLambda(call)`: the trailing `content` lambda of a composable call, if any.
2. **`ModifierChain.kt`**
   - Given the expression passed as `modifier = ...`, return the ordered list of modifier calls, for example `[size(20.dp), clickable{...}, semantics{...}]`. A chain like `Modifier.a().b()` appears in UAST as nested `UQualifiedReferenceExpression` / `UCallExpression` nodes, so walk receivers and selectors.
   - Also resolve a local `val m = Modifier...` in the same function. Do **not** follow modifiers passed in from other functions in Phase 2; record this in LIMITATIONS.
   - Helpers: `hasModifier(name)`, `semanticsBlocks()`, `semanticsAssignments()` (which properties are set inside `semantics { }` and `clearAndSetSemantics { }`, for example `contentDescription`, `role`, `stateDescription`, `heading()`, `liveRegion`, `paneTitle`, `error()`, `customActions`, `collectionInfo`, `progressBarRangeInfo`).
3. **`UiScope.kt`**
   - `isInUiScope(node)`: true only if the node is inside a function annotated `@Composable` **and not** inside the lambda of `remember`, `rememberSaveable`, `derivedStateOf`, `LaunchedEffect`, `DisposableEffect`, `SideEffect`, `produceState`, or a coroutine builder (`launch`, `async`).
   - Skip functions annotated `@Preview` for U-07 only (previews often use literal text on purpose).
4. **`Literals.kt`**: `isNullLiteral`, `isEmptyStringLiteral`, `stringLiteralValue`, `isStringResourceCall` (`stringResource(...)`, `pluralStringResource(...)`), `dpValue` (reads `20.dp` → `20f`), `spValue`.
5. **`Contrast.kt`**: WCAG relative luminance and contrast ratio for `0xAARRGGBB` colors. Unit-test it with known pairs (black on white = 21:1, `#B0B0B0` on white ≈ 2.1:1).

### 5.4 Test infrastructure

- **Compose stubs (`stubs/ComposeStubs.kt`).** Lint tests cannot see the real Compose libraries. Write small Kotlin stub files with the **real package names and signatures** (checked against androidx sources) for everything the rules touch, for example:
  - `androidx.compose.runtime`: `Composable`, `remember`, `mutableStateOf`, `LaunchedEffect`
  - `androidx.compose.ui`: `Modifier` (interface with companion), `Modifier.semantics`, `Modifier.clearAndSetSemantics`
  - `androidx.compose.ui.semantics`: `SemanticsPropertyReceiver`, `contentDescription`, `stateDescription`, `heading()`, `role`, `Role`, `liveRegion`, `LiveRegionMode`, `paneTitle`, `error()`, `customActions`, `CustomAccessibilityAction`, `collectionInfo`, `CollectionInfo`, `progressBarRangeInfo`, `ProgressBarRangeInfo`
  - `androidx.compose.foundation`: `clickable`, `toggleable`, `selectable`, `Image`, `background`, `Canvas`, `pointerInput`
  - `androidx.compose.foundation.layout`: `Box`, `Row`, `Column`, `size`, `width`, `height`, `padding`
  - `androidx.compose.material3`: `Icon`, `Text`, `Button`, `TextButton`, `IconButton`, `Card`, `Surface`, `TextField`, `OutlinedTextField`, `Switch`, `Checkbox`, `ModalBottomSheet`, `MaterialTheme`, `Typography`, `minimumInteractiveComponentSize`
  - `androidx.compose.ui.unit`: `Dp`, `dp`, `TextUnit`, `sp`; `androidx.compose.ui.graphics.Color`
  - `androidx.compose.ui.res.stringResource`
  Keep stubs minimal (signatures only, bodies `TODO()` or empty). Expose them as `val composeStubs: Array<TestFile>`.
- **Base class `A11yLintTest`** extends `LintDetectorTest`, adds the stubs to every test automatically, and provides helpers `expectWarnings(...)` and `expectClean(...)`.
- **Test modes:** keep Lint's default test modes on (they test parentheses, fully qualified names, etc.). If one fails, fix the detector. Only disable a mode with a comment and a note in DECISIONS.

---

## 6. Definition of done for every rule

A rule is done only when all of these are true:

1. Detector implemented and registered in `Taxonomy.kt`.
2. **At least 4 positive tests** (code that must be flagged) and **at least 4 negative tests** (code that must not be flagged), including the edge cases listed in section 7.
3. Message starts with the `[ID]` prefix, the explanation follows section 5.2, and severity matches the taxonomy.
4. A screen in `sample-app/.../defects/<id>/` with a **bad** and a **good** version. Each bad line is marked with a comment `// EXPECT: <IssueId>`. `scripts/check_sample_expectations.py` passes.
5. A section in `docs/RULES.md`: ID, Issue ID, what it flags, what it ignores, WCAG criterion, example of bad and good code, known limitations.
6. Any heuristic or threshold recorded in `docs/DECISIONS.md`; anything the rule cannot catch statically recorded in `docs/LIMITATIONS.md`.
7. `./gradlew :lint-rules:test` and `./gradlew :sample-app:lintDebug` both pass.
8. One commit per rule: `feat(rule): P-01 ComposeMissingContentDescription`.

---

## 7. The 27 rules

**Priority order of work:** Critical first (Milestone 2), then Major (Milestone 3), then Minor (Milestone 4).
"Flag" = report. "Ignore" = must not report (write negative tests for these).

### 7.1 Overlap policy (one defect, one warning)

Some rules can fire on the same node. Report **only one** issue per node, using this precedence:

- **O-05 over P-01:** a clickable with no readable content at all is O-05; an Icon or Image inside a clickable with a missing label is P-01.
- **O-04 over R-01:** a clickable `Box`/`Row`/`Column` whose content is button-like (a single Text, or an Icon plus a Text) is O-04 ("use Button"); any other clickable custom element without a role is R-01.
- **R-06** is reported on the function declaration and only for custom gestures (see R-06), so it does not duplicate R-01.

Record any further overlaps you find in DECISIONS.

### 7.2 Critical (Milestone 2)

**P-01 · `ComposeMissingContentDescription` · ERROR · STATIC · WCAG 1.1.1**
- Flag: `Icon(...)` or `Image(...)` whose `contentDescription` is `null` or `""` when the Icon or Image is the only possible label of a clickable element: it has `clickable`/`toggleable` in its own modifier chain, or it is the content of an `IconButton` (or a clickable parent) that has no Text and no `contentDescription` in semantics.
- Ignore: Icon with `null` inside a `Button` that also has a `Text` (decorative, the button already has a name); non-interactive images with `null` (decorative is allowed); `contentDescription = stringResource(...)` or any non-empty expression.
- Edge cases: `""` counts as missing; a variable that may be null is **not** flagged (record in LIMITATIONS).

**O-01 · `ComposeSmallTouchTarget` · ERROR · STATIC · WCAG 2.5.8 (Android guideline 48dp)**
- Flag: an element with `clickable`/`toggleable`/`selectable` in its modifier chain and a literal `size`, `width`, `height`, `requiredSize` (etc.) below `48.dp`, when `minimumInteractiveComponentSize()` is not in the chain.
- Ignore: `IconButton` and Material buttons (they enforce the minimum); sizes that are not literals; non-interactive elements.
- Note: modifier order matters in Compose. Record exactly how you treat order in DECISIONS.

**O-03 · `ComposeNestedClickable` · ERROR · STATIC · WCAG 2.4.3, 4.1.2**
- Flag: a clickable element (Modifier.clickable, or `Card(onClick=...)`, `Surface(onClick=...)`) whose content lambda directly contains another clickable element (Modifier.clickable, Button, IconButton, `Card(onClick)`).
- Ignore: siblings; clickables inside a separately declared composable called from the content (Phase 2 does not follow calls, record in LIMITATIONS).
- Note: this rule may produce false positives for common card patterns. Track precision on the development apps and report to Sedra.

**U-01 · `ComposeMissingHeading` · ERROR · STATIC_LLM · WCAG 1.3.1, 2.4.6**
- Flag (candidate): `Text(...)` with `style = MaterialTheme.typography.displayX / headlineX / titleLarge`, or `fontSize >= 20.sp` with `FontWeight.Bold`, and no `heading()` in its semantics.
- Ignore: Text that already has `heading()`; `TopAppBar` titles (verify whether Material already marks them, and note the result); text inside a `Button`.

**U-02 · `ComposeMissingStateDescription` · ERROR · STATIC · WCAG 4.1.2**
- Flag: `Modifier.clickable { ... }` whose lambda flips a Boolean state (`x = !x`, `x.value = !x.value`, `onXChange(!x)`) on a custom element that has no `stateDescription`, `toggleable`, `selectable` or `toggleableState` semantics.
- Ignore: Material `Switch`, `Checkbox`, `RadioButton`, `Modifier.toggleable` (they expose state already); clickables that do not flip a Boolean.

**U-05 · `ComposeTextFieldWithoutLabel` · ERROR · STATIC · WCAG 1.3.1, 3.3.2**
- Flag: `TextField`/`OutlinedTextField` without a `label` argument (placeholder alone does not count, because it disappears when the user types).
- Ignore: fields with `label = { ... }`.

**R-01 · `ComposeClickableWithoutRole` · ERROR · STATIC_LLM · WCAG 4.1.2**
- Flag (candidate): `Modifier.clickable` without a `role` argument and without `role = ...` in a semantics block, on a custom element (not a Material component).
- Ignore: clickables with a role; Material components; the O-04 case (see overlap policy).

### 7.3 Major (Milestone 3)

**P-02 · `ComposeDecorativeImageLabeled` · WARNING · STATIC_LLM · WCAG 1.1.1**
- Flag (candidate): a non-interactive Icon/Image with a non-empty literal `contentDescription` that is equal to, or contained in, a sibling `Text` literal in the same parent (the label is repeated).
- Ignore: interactive images; descriptions that differ from nearby text.

**P-03 · `ComposeLowContrastColors` · WARNING · STATIC · WCAG 1.4.3**
- Flag: a Text whose `color = Color(0x...)` literal and whose background is also a literal (from `Modifier.background(Color(...))` on the Text, or `Surface(color = Color(...))`/`Box(Modifier.background(...))` directly around it) with a contrast ratio below 4.5:1, or below 3:1 for large text (`fontSize >= 18.sp`, or `>= 14.sp` with bold).
- Ignore: theme colors (`MaterialTheme.colorScheme...`); any non-literal color.

**P-04 · `ComposeTextSizeInDp` · WARNING · STATIC · WCAG 1.4.4**
- Flag: `fontSize` (on Text or in `TextStyle`) set from a dp conversion, for example `with(LocalDensity.current) { 16.dp.toSp() }` or `someDp.toSp()`.
- Ignore: `sp` values and typography styles.

**P-06 · `ComposeMissingLiveRegion` · WARNING · STATIC_LLM · WCAG 4.1.3**
- Flag (candidate): a `Text(text = s)` where `s` is a local state created with `mutableStateOf` and assigned inside `LaunchedEffect`, a coroutine (`launch`) or a callback that is not a direct click handler, and the Text has no `liveRegion`.
- Ignore: state assigned only inside click handlers; Text with `liveRegion`.
- Limitation: state coming from a ViewModel is not tracked in Phase 2 (LIMITATIONS).

**O-02 · `ComposeMissingOnClickLabel` · WARNING · STATIC · WCAG 4.1.2**
- Flag: `Modifier.clickable` without `onClickLabel` on containers (`Card`, `Surface`, `Row`, `Column`, `Box`, `ListItem`).
- Ignore: Material buttons; clickables with `onClickLabel`.
- Note: this rule may be noisy. Measure it on the development apps and report before tuning.

**O-04 · `ComposeClickableContainer` · WARNING · STATIC_LLM · WCAG 4.1.2**
- Flag (candidate): clickable `Box`/`Row`/`Column` with no role whose content is button-like (see overlap policy).
- Ignore: containers with a role; containers that hold rich content (lists, several texts and images).

**O-05 · `ComposeEmptyClickable` · WARNING · STATIC · WCAG 4.1.2**
- Flag: a clickable element whose content has no `Text`, no Icon/Image with a label, and no `contentDescription` in semantics.
- Ignore: clickables whose content calls another composable (cannot be seen; LIMITATIONS).

**U-03 · `ComposeMissingSemanticError` · WARNING · STATIC · WCAG 3.3.1**
- First verify in the Material3 source whether `TextField`/`OutlinedTextField` already add an `error(...)` semantic when `isError = true`, and record the answer in DECISIONS.
- Flag: `isError` set to a non-`false` expression with neither `supportingText` nor a custom `error("...")` in semantics, so the user only hears a generic message, or nothing.
- Ignore: fields with a specific error message.

**U-04 · `ComposeVagueButtonLabel` · WARNING · STATIC_LLM · WCAG 2.4.6**
- Flag (candidate): `Button`/`TextButton`/`OutlinedButton` whose only Text literal (case-insensitive, trimmed) is in this list: `ok`, `click`, `click here`, `here`, `tap`, `tap here`, `go`, `submit`, `done`, `more`, `yes`, `no`. Keep the list in one constant and document it.
- Ignore: buttons with descriptive text; buttons with a `contentDescription` override.

**R-02 · `ComposeClearAndSetSemanticsLoss` · WARNING · STATIC · WCAG 4.1.2**
- Flag: `clearAndSetSemantics { }` that is empty, or that does not set `contentDescription` (or text) when its children contain Text or labelled Icons, or that does not set `role`/`stateDescription` when its children were clickable or had state.
- Ignore: blocks that replace all of these.

**R-03 · `ComposeMergeHidesInteractive` · WARNING · STATIC_LLM · WCAG 4.1.2**
- First check how Compose treats interactive children inside `semantics(mergeDescendants = true)` (official docs, and a quick test in the sample app with Layout Inspector). Record the finding in DECISIONS before writing the rule.
- Flag (candidate): explicit `mergeDescendants = true` on a container whose content directly contains interactive children (Button, IconButton, clickable, Checkbox, Switch).
- Ignore: merged containers with no interactive children.

**R-06 · `ComposeComposableWithoutSemantics` · WARNING · STATIC_LLM · WCAG 4.1.2**
- Flag (candidate), on the function declaration: a `@Composable` function that handles input through custom gestures (`pointerInput`, `detectTapGestures`, `draggable`, `swipeable`/`anchoredDraggable`) and sets no semantics at all (no `semantics`, `clickable`, `toggleable`, `role`, `customActions`). Custom gestures give TalkBack no actions, which is the real defect.
- Ignore: functions that use `clickable`/`toggleable` (those are covered by other rules).
- Note: this narrows the taxonomy wording ("contains clickable or toggleable UI"). Record it in DECISIONS so the thesis taxonomy can be updated.

### 7.4 Minor (Milestone 4)

**P-05 · `ComposeTextOverImage` · WARNING · STATIC_LLM · WCAG 1.4.3**
- Flag (candidate): a `Box` that contains an `Image` followed by a `Text` (text drawn on top), with no background or scrim modifier on the Text or a layer between them.
- Ignore: Text with a background.

**P-07 · `ComposeMissingPaneTitle` · WARNING · STATIC · WCAG 1.3.1**
- First verify whether Material3 `ModalBottomSheet`, `ModalNavigationDrawer` and `AlertDialog` already set a pane title. Exclude the ones that do and record the result.
- Flag: custom overlays (for example `Popup` content, or a full-screen `Box` shown conditionally with `AnimatedVisibility`) with no `paneTitle`.

**O-06 · `ComposeDisabledButClickable` · WARNING · STATIC · WCAG 4.1.2**
- Flag: a `Modifier.clickable { }` whose lambda is guarded by a condition (`if (!enabled) return@clickable`, `if (enabled) { ... }`) while `enabled = ...` is not passed to `clickable`. Screen readers then announce the element as enabled.
- Ignore: `clickable(enabled = x)`.

**O-07 · `ComposeMissingCustomActions` · WARNING · STATIC_LLM · WCAG 2.1.1**
- Flag (candidate): a clickable `Card`/`Surface`/`Row` that contains two or more other clickable children (for example favorite and share IconButtons) and has no `customActions`.
- Ignore: containers with `customActions`.

**U-06 · `ComposeInconsistentLabels` · WARNING · STATIC_LLM · WCAG 3.2.4**
- Project-wide rule. Use Lint partial analysis (`context.getPartialResults` / `checkPartialResults`) so it works with incremental Lint.
- Flag (candidate): the same `Icons.X.Y` used with different literal `contentDescription` strings across the project (for example `"Delete"` and `"Remove item"`).
- If partial analysis becomes too complex, implement it per module first and record that in DECISIONS.

**U-07 · `ComposeHardcodedA11yText` · WARNING · STATIC · No direct WCAG criterion (localization)**
- Flag: string literals passed to `contentDescription`, `stateDescription`, `onClickLabel`, `paneTitle`, `error(...)` or `CustomAccessibilityAction(label = ...)`.
- Ignore: `stringResource(...)`; code inside `@Preview` functions; test sources.

**R-04 · `ComposeMissingCollectionInfo` · WARNING · STATIC · WCAG 1.3.1**
- Flag: a `Column`/`Row` (often with `verticalScroll`/`horizontalScroll`) whose content emits items with `forEach`, `forEachIndexed` or a `for` loop, and no `collectionInfo`/`collectionItemInfo` is set.
- Ignore: `LazyColumn`, `LazyRow` and lazy grids (they provide collection info).

**R-05 · `ComposeMissingProgressRange` · WARNING · STATIC · WCAG 4.1.2**
- Flag: a `@Composable` with a `Float` parameter named like `progress`, `percent`, `fraction` or `value`, that draws with `Canvas`/`drawBehind`, and sets no `progressBarRangeInfo`.
- Ignore: Material progress indicators.

---

## 8. Milestones

Stop after each milestone, update `docs/PROGRESS.md`, and give Sedra a short report: what was done, test counts, anything uncertain, and questions.

**Milestone 0: Setup**
1. Check `java -version`, the AGP version, and the Gradle wrapper. Rename `app` to `sample-app` if needed.
2. Create `lint-rules`, `lint-library` and the docs files.
3. Add a trivial rule (for example one that flags a function named `a11yTestMarker`), its test, and the registry.
4. Confirm: `./gradlew :lint-rules:test` passes, `./gradlew :sample-app:lintDebug` reports the trivial rule, and the SARIF file is produced.
5. Initialize git with a `.gitignore` for Android/Gradle. Then remove the trivial rule.

**Milestone 1: Infrastructure**
Taxonomy metadata, all utilities in section 5.3 with their own unit tests (especially `ModifierChain` and `Contrast`), Compose stubs, base test class, and `scripts/check_sample_expectations.py`.

**Milestone 2: Critical rules** P-01, O-01, O-03, U-01, U-02, U-05, R-01.
Then run on the two development apps (section 9) and write `docs/DEV_APP_RESULTS.md`: warnings per rule, false positives you noticed, and proposed fixes. **Ask Sedra before tuning rules based on these results.**

**Milestone 3: Major rules** P-02, P-03, P-04, P-06, O-02, O-04, O-05, U-03, U-04, R-02, R-03, R-06. Development app run again.

**Milestone 4: Minor rules** P-05, P-07, O-06, O-07, U-06, U-07, R-04, R-05. If time is short, Sedra may move some of these to future work.

**Milestone 5: Packaging and reporting**
- Publish `lint-library` to `mavenLocal()` and document how another project adds it.
- Confirm SARIF and XML output, and add a small script that converts the XML report to the JSON format below (used by the evaluation and by Phase 3):
  ```json
  { "taxonomyId": "P-01", "issueId": "ComposeMissingContentDescription",
    "severity": "Error", "file": "path/Screen.kt", "line": 42, "column": 9,
    "message": "...", "detection": "STATIC" }
  ```
- `scripts/measure_lint_time.sh`: run `lintDebug` with `--rerun-tasks` several times with and without the custom rules, and print the mean added time (Plan section 6.5).
- Final pass over `docs/RULES.md`.

---

## 9. Development apps vs evaluation apps (important)

The thesis evaluates precision on 15 to 30 open-source apps. If the rules are tuned on those apps, the results are biased.

- **Allowed while building rules:** `sample-app`, and two development apps from Google's official `android/compose-samples` repository: **Jetnews** and **Jetchat**. Clone them **outside** this repository (for example `../dev-apps/`) and add the rules through `mavenLocal()`.
- **Not allowed:** any other app, including **Now in Android**, which is reserved for the evaluation corpus. If you think another app is needed, ask Sedra first.
- Never commit code from the development apps into this repository.

---

## 10. Documentation the thesis depends on

| File | What goes in it | Used for |
|---|---|---|
| `docs/RULES.md` | Catalog of all rules (section 6, point 5) | Thesis chapter on implementation |
| `docs/DECISIONS.md` | Dated entries: decision, reason, alternatives considered | Methodology chapter, viva questions |
| `docs/LIMITATIONS.md` | Patterns a rule cannot detect statically, with a short example | RQ1 and the error analysis |
| `docs/PROGRESS.md` | One entry per milestone: done, test counts, open questions | Progress reports to the supervisor |
| `docs/DEV_APP_RESULTS.md` | Per-rule warning counts and false positives on Jetnews and Jetchat | Rule tuning record |

Write these in plain, simple English. Do not use long dashes.

---

## 11. Coding conventions

- Kotlin, 4-space indent, no wildcard imports.
- One detector per file, named after the rule: `P01MissingContentDescriptionDetector`.
- Detectors use `getApplicableMethodNames()` / `visitMethodCall()` or a `UElementHandler` where that is clearer. Prefer resolving calls to fully qualified names over matching simple names.
- No reflection tricks and no internal Lint APIs unless necessary. If you use one, note it in DECISIONS.
- Keep detectors small. Put shared logic in `util/`.
- Every public utility has a KDoc comment.

---

## 12. Useful commands

```bash
./gradlew :lint-rules:test                       # all rule tests
./gradlew :lint-rules:test --tests "*P01*"       # one rule
./gradlew :sample-app:lintDebug                  # run rules on the sample app
python3 scripts/check_sample_expectations.py     # compare with // EXPECT markers
./gradlew :lint-library:publishToMavenLocal      # package for the dev apps
```

Reports are written to `sample-app/build/reports/` (`lint-results-debug.sarif`, `.xml`, `.html`).

**If Android Studio keeps showing old results** after a rule change, trust the Gradle run and the unit tests. Restarting the IDE or running *File > Sync Project with Gradle Files* usually clears it.

---

## 13. What comes after this phase (do not build yet)

Phase 3 adds the LLM layers. To make that easy later:
- keep the `[ID]` prefix in every message,
- keep `Taxonomy.kt` as the single source of truth for rule metadata,
- keep the JSON export format stable,
- do not put any LLM, network or slicing code in the Lint rules.
