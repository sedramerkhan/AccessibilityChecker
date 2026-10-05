# Progress

## Milestone 0: Setup

- Project renamed to `compose-a11y-lint`.
- Generated `app` module renamed to `sample-app`.
- Added `lint-rules` and `lint-library` module scaffolding.
- Added a temporary `a11yTestMarker` lint rule and sample probe.
- JDK 17 detected. AGP 9.4.1 and Gradle 9.6.0 detected.
- Lint dependency version selected as AGP plus 23: 32.4.1.
- The temporary marker rule and sample probe were removed after the lint unit test passed.
- The registry now reads issues from the taxonomy metadata.

## Milestone 1: Infrastructure

Status: done (2026-10-03).

- Taxonomy enums and entry model (`Pour`, `DetectionType`, `Priority`, `TaxonomyEntry`). The entry list stays empty until the first rule.
- Utilities with KDoc: `Contrast`, `Literals`, `ComposeCalls`, `ModifierChain`, `UiScope`.
- Compose stubs rewritten against the real Compose 1.10.4 and Material3 1.4.0 sources. They now compile, so `allowCompilationErrors()` was removed.
- Shared `A11yLintTest` base with `expectWarnings` and `expectClean`.
- Test-only `UtilityProbeDetector` that runs the utilities on real UAST under all Lint test modes. The earlier "probe is not dispatched" problem was caused by JUnit 3 naming (`LintDetectorTest` only runs methods named `test...`).
- Utility bugs found by the new tests and fixed: `isCall` matched any function in a file with the same name; `dpValue`/`spValue` never matched `20.dp`; modifier calls could be listed twice; semantics property assignments (`contentDescription = ...`, `role = ...`) were not found; local `val` modifiers were not followed.
- `scripts/check_sample_expectations.py` compares the Lint XML report with the `// EXPECT:` markers.
- Sample app moved to the package `org.svu.sedra.a11ylint.sample`.
- Registry `minApi` set to 14 (AGP 8.0).

Tests: 23 passing, 0 failing (`ModifierChainTest` 7, `LiteralsTest` 5, `ComposeCallsTest` 4, `UiScopeTest` 2, `ContrastTest` 5).
`./gradlew :sample-app:lintDebug` passes and writes the SARIF, XML, HTML and text reports. The expectations script passes (there are no rules or markers yet).

Open questions for Sedra:
- `minApi = 14` lets the rules load on AGP 8.0 and newer, but this is untested on older AGP. Is that acceptable?
- The Gradle daemon now uses JDK 25, because Android Studio wrote `gradle-daemon-jvm.properties`. The rules still compile with JDK 17. Keep it this way?

## Milestone 2: Critical rules

In progress: all 7 critical rules are done. The development app run (Jetnews and Jetchat) is next.

| Rule | Status | Positive tests | Negative tests | Sample EXPECT lines |
|---|---|---|---|---|
| P-01 `ComposeMissingContentDescription` | Done | 6 (7 reports) | 6 | 6, all matched |
| O-01 `ComposeSmallTouchTarget` | Done | 5 | 5 (13 cases) | 5, all matched |
| O-03 `ComposeNestedClickable` | Done | 5 (8 reports) | 5 | 4, all matched |
| U-01 `ComposeMissingHeading` | Done | 5 (6 reports) | 5 | 4, all matched |
| U-02 `ComposeMissingStateDescription` | Done | 4 | 4 (9 cases) | 3, all matched |
| U-05 `ComposeTextFieldWithoutLabel` | Done | 4 (5 reports) | 4 | 3, all matched |
| R-01 `ComposeClickableWithoutRole` | Done | 4 (5 reports) | 4 | 4, all matched |

Notes:
- P-01 first matched nothing on the sample app although all unit tests passed. The compiled Compose libraries use mangled JVM names (`Icon-ww6aTOc`) and Lint resolves `Card(onClick = ...)` to the wrong overload. Both are handled in `ComposeCalls` (see DECISIONS, 2026-10-03). From now on the sample app check is required for every rule.
- Shared helpers for later rules: `taxonomy/A11yIssues` (issue creation with the severity mapping) and `util/Clickables` (clickable elements, Text and Icon names, accessible name search).
- `TaxonomyTest` checks taxonomy entries against their issues and the registry.
- O-01 also first failed on the sample app: compiled `Dp` parameters lose their names (`p`). `ComposeCalls.argument` now matches them by position (see DECISIONS). New shared helper: `util/ModifierSizes` (literal sizes and paddings along a modifier chain).
- O-03 matched the sample app on the first run. Open question for Sedra: should `Checkbox`/`Switch`/`RadioButton` with a callback inside a clickable row also count as nested clickables (see DECISIONS)?
- U-01: Material3 top app bars do not mark their title as a heading. The rule ignores them as CLAUDE.md says. Question for Sedra: keep ignoring or report them? New shared helper: `util/TextStyles` (typography style, font size, bold).
- U-02 matched the sample app on the first run. `outermostExpression` and `receivingElement` moved from O-01 into `ModifierChain` for reuse.
- U-05 passed its tests and the sample app on the first run.
- R-01 reported 10 role-less custom clickables in the earlier sample screens. They were given `role = Role.Button` so each screen shows only its own rule. New shared helper: `Clickables.isButtonLikeContainer` (the O-04 case of the overlap policy).

## Milestone 3: Major rules

In progress.

| Rule | Status | Positive tests | Negative tests | Sample EXPECT lines |
|---|---|---|---|---|
| P-02 `ComposeDecorativeImageLabeled` | Done | 2 | 4 | 1, matched |
| P-03 `ComposeLowContrastColors` | Done | 4 | 6 | 4, all matched |
| P-04 `ComposeTextSizeInDp` | Done | 4 | 6 | 3, all matched |
| P-06 `ComposeMissingLiveRegion` | Done | 4 | 6 | 2, all matched |
| O-02 `ComposeMissingOnClickLabel` | Done | 5 | 5 | 5, all matched |

Notes:
- P-02 first failed under the `PARENTHESIZED` lint test mode: the sibling-text lookup only matched a direct `UCallExpression` as a block statement, so wrapping sub-expressions in parentheses made the detector silently stop matching anything. Fixed with the same `Literals.unwrap(...)` pattern already used in `Clickables.isButtonLikeContainer`, also handling the fully-qualified-call case (see DECISIONS, 2026-10-04).
- The sample screens originally called `Icon`/`Image` with only `contentDescription`, which does not compile against the real Material3/Foundation signatures (both overloads require `imageVector` or `painter`). Fixed by passing a `painterResource`, matching the P-01 sample pattern.
- P-03 unit tests (4 positive, 6 negative) passed on the first run, but the sample app first reported only 2 of the 4 EXPECT lines: `Modifier.background(Color(...))` written positionally was not found, because `Color`'s compiled parameter name is lost the same way `Dp`'s is (found while building O-01). Fixed the same way `ModifierSizes` does, with a `kotlinNames` fallback (see DECISIONS). New shared helper: `Literals.colorLiteralValue` (reads a literal `Color(0x...)` factory call as packed ARGB). Message formatting avoids `String.format` to stay locale-independent (see DECISIONS).
- P-04 passed its unit tests and the sample app on the first run. New stubs: `Density`/`FontScaling` (`androidx.compose.ui.unit`), `CompositionLocal`/`staticCompositionLocalOf` (`androidx.compose.runtime`), `LocalDensity` (`androidx.compose.ui.platform`). `TextStyles.styleCall` was made non-private so P-04 can read a `TextStyle`'s own `fontSize` expression (see DECISIONS).
- P-06's first version checked for `mutableStateOf` by searching the variable declaration's source text, like U-02 does for flip targets, but this failed under the `IMPORT_ALIAS` test mode because that mode renames the call site. Fixed by resolving every call in the declaration's initializer or delegate and checking it against `androidx.compose.runtime.mutableStateOf` (see DECISIONS). Sample app and unit tests then passed together. This is the first STATIC_LLM rule since R-01; both are always reported as candidates, not hidden on uncertainty.
- O-02 passed its unit tests on the first run, but the sample app first reported 25 warnings instead of 5: the container list (`Row`/`Box`/`Column`/`Card`/`ListItem`) also matched `Modifier.clickable` calls built earlier for O-01, O-03, P-01, R-01 and U-02, on both bad and good screens. The same situation happened when R-01 was added (see PROGRESS, Milestone 2). Fixed by giving every one of those 20 calls an `onClickLabel`, the same way they were earlier given a `role`.
