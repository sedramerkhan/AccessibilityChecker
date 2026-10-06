# Progress

## Where the project stands (2026-10-05)

| Milestone | Status |
|---|---|
| 0 Setup | Done |
| 1 Infrastructure | Done |
| 2 Critical rules (7) | Done, including the development app run (2026-10-06). |
| 3 Major rules (12) | Done: 11 implemented, R-03 dropped as not applicable. |
| 4 Minor rules (8) | Not started: P-05, P-07, O-06, O-07, U-06, U-07, R-04, R-05. |
| 5 Packaging and reporting | Started: publishing to mavenLocal works and is verified. Scripts and the final docs pass are open. |

18 detectors implemented, each registered in `Taxonomy.kt`, documented in `RULES.md` and covered
by a bad and a good sample screen. Across the whole sample app: 18 issue IDs, 64 expected,
64 reported, 0 missing, 0 unexpected.

The rules have now also been run on code we did not write: **33 findings across JetNews and
Jetchat**, after fixing the two rule defects that first run exposed in O-05 and R-02. See
[DEV_APP_RESULTS.md](DEV_APP_RESULTS.md). O-02, which CLAUDE.md expected to be noisy, produced
8 findings across two complete apps, and O-03 produced one, on a genuine defect.

### What is left, in order

1. **Milestone 4**, the eight minor rules. Two need more than a detector: P-07 needs the
   Material3 check for whether `ModalBottomSheet`, `ModalNavigationDrawer` and `AlertDialog`
   already set a pane title, and U-06 is project-wide and needs Lint partial analysis.
2. **Milestone 5**: `scripts/measure_lint_time.sh`, the XML to JSON converter in the format
   Phase 3 expects, and a final pass over `RULES.md`. The publishing part of this milestone is
   already done and verified.

### Open questions for Sedra

Collected here so they do not have to be hunted for. Each is also recorded next to the rule it
belongs to.

1. **`minApi = 14`** (Milestone 1) lets the rules load on AGP 8.0 and newer, untested on older
   AGP. Acceptable?
2. **Gradle daemon on JDK 25** (Milestone 1), written by Android Studio into
   `gradle-daemon-jvm.properties`. The rules still compile with JDK 17. Keep it?
3. **O-03**: should `Checkbox`, `Switch` or `RadioButton` with a callback inside a clickable row
   count as a nested clickable? Currently they do not.
4. **U-01**: Material3 top app bars do not mark their title as a heading. The rule ignores them,
   as CLAUDE.md says. Keep ignoring, or report them, since the title is usually the screen's
   main heading?
5. **P-01**: should a parent's `semantics { onClick(label = ...) }` count as a name? JetNews uses
   that pattern deliberately and our rule still reports the icon, consistently with its recorded
   decision that an action label is not a name. Left as it is; a good candidate for Phase 3 to
   judge rather than for tuning the static rule. See `DEV_APP_RESULTS.md`.
6. **U-01**: does Material3 give `AlertDialog` titles their own semantics? The same check that was
   done for top app bars. U-01 reports a dialog title as a possible heading on JetNews.

**Answered:** R-02's empty-block question. The development app run showed Google's own code using
an empty `clearAndSetSemantics { }` deliberately, so the rule was narrowed to report an empty
block only when the content had a name or an interaction to lose (2026-10-06, see DECISIONS).

### Thesis text that needs updating

These are not questions, they are edits the taxonomy chapter needs.

0. **R-02's wording narrows.** The taxonomy says an empty `clearAndSetSemantics { }` is flagged
   without qualification. The rule now reports an empty block only when it hides a name or an
   interaction, because flagging it unconditionally reported a correct, deliberate pattern on
   JetNews. See `DEV_APP_RESULTS.md`.

1. **R-03 `ComposeMergeHidesInteractive` is void.** The defect cannot occur in Compose 1.10.4:
   merging stops at any child that is itself a merging root, and every clickable modifier sets
   `shouldMergeDescendantSemantics = true`. No detector exists. The taxonomy becomes 26 rules
   plus R-03 recorded as not applicable. Evidence in `DECISIONS.md`.
2. **R-06's wording must change** from "contains clickable or toggleable UI and sets no
   semantics" to custom gestures only, which is what the rule detects and what the real defect
   is. CLAUDE.md anticipated this change.

### Known gap in the checks

`scripts/check_sample_expectations.py` has not been run on this machine: Python is not
installed here, although CLAUDE.md section 12 lists it as a standard command. Every sample app
figure in this file was produced by an equivalent check written in PowerShell, which compares
the same two things in both directions (every `// EXPECT:` marker against the Lint XML report).
The Python script itself is therefore still unverified and should be run once on a machine that
has Python.

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

Rules: done (2026-10-03), all 7. The development app run that closes this milestone was deferred
in favour of continuing with Milestone 3 and is still open; see "What is left" at the top.

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

Status: done (2026-10-05). Eleven rules implemented and one (R-03) dropped as not applicable.
Across the whole sample app the expectation check is 18 issue IDs, 64 expected, 64 reported,
0 missing, 0 unexpected. The development app run on Jetnews and Jetchat is next, and it is now
overdue: it was already due after Milestone 2, and CLAUDE.md asks for O-02 and O-03 precision to
be measured there before either is tuned.

| Rule | Status | Positive tests | Negative tests | Sample EXPECT lines |
|---|---|---|---|---|
| P-02 `ComposeDecorativeImageLabeled` | Done | 2 | 4 | 1, matched |
| P-03 `ComposeLowContrastColors` | Done | 4 | 6 | 4, all matched |
| P-04 `ComposeTextSizeInDp` | Done | 4 | 6 | 3, all matched |
| P-06 `ComposeMissingLiveRegion` | Done | 4 | 6 | 2, all matched |
| O-02 `ComposeMissingOnClickLabel` | Done | 5 | 5 | 5, all matched |
| O-04 `ComposeClickableContainer` | Done | 5 | 6 | 3, all matched |
| O-05 `ComposeEmptyClickable` | Done | 6 | 7 | 5, all matched |
| U-03 `ComposeMissingSemanticError` | Done | 4 | 5 | 3, all matched |
| U-04 `ComposeVagueButtonLabel` | Done | 4 | 6 | 3, all matched |
| R-02 `ComposeClearAndSetSemanticsLoss` | Done | 4 | 5 | 4, all matched |
| R-03 `ComposeMergeHidesInteractive` | Dropped, not applicable | — | — | — |
| R-06 `ComposeComposableWithoutSemantics` | Done | 3 | 5 | 2, all matched |

Notes:
- P-02 first failed under the `PARENTHESIZED` lint test mode: the sibling-text lookup only matched a direct `UCallExpression` as a block statement, so wrapping sub-expressions in parentheses made the detector silently stop matching anything. Fixed with the same `Literals.unwrap(...)` pattern already used in `Clickables.isButtonLikeContainer`, also handling the fully-qualified-call case (see DECISIONS, 2026-10-04).
- The sample screens originally called `Icon`/`Image` with only `contentDescription`, which does not compile against the real Material3/Foundation signatures (both overloads require `imageVector` or `painter`). Fixed by passing a `painterResource`, matching the P-01 sample pattern.
- P-03 unit tests (4 positive, 6 negative) passed on the first run, but the sample app first reported only 2 of the 4 EXPECT lines: `Modifier.background(Color(...))` written positionally was not found, because `Color`'s compiled parameter name is lost the same way `Dp`'s is (found while building O-01). Fixed the same way `ModifierSizes` does, with a `kotlinNames` fallback (see DECISIONS). New shared helper: `Literals.colorLiteralValue` (reads a literal `Color(0x...)` factory call as packed ARGB). Message formatting avoids `String.format` to stay locale-independent (see DECISIONS).
- P-04 passed its unit tests and the sample app on the first run. New stubs: `Density`/`FontScaling` (`androidx.compose.ui.unit`), `CompositionLocal`/`staticCompositionLocalOf` (`androidx.compose.runtime`), `LocalDensity` (`androidx.compose.ui.platform`). `TextStyles.styleCall` was made non-private so P-04 can read a `TextStyle`'s own `fontSize` expression (see DECISIONS).
- P-06's first version checked for `mutableStateOf` by searching the variable declaration's source text, like U-02 does for flip targets, but this failed under the `IMPORT_ALIAS` test mode because that mode renames the call site. Fixed by resolving every call in the declaration's initializer or delegate and checking it against `androidx.compose.runtime.mutableStateOf` (see DECISIONS). Sample app and unit tests then passed together. This is the first STATIC_LLM rule since R-01; both are always reported as candidates, not hidden on uncertainty.
- R-06 passed its unit tests and the sample app on the first run. It narrows the taxonomy wording as CLAUDE.md anticipated: it reports **custom gestures** with no semantics, not "clickable or toggleable UI", because a clickable already carries an action and is covered by R-01, O-02 and U-02. **The thesis taxonomy text for R-06 needs that wording change.** Two of the gesture APIs CLAUDE.md names could not be used: `swipeable` exists only in Material 2 and is entirely deprecated, and `anchoredDraggable` is internal in Material3 1.4.0, so `draggable2D` and the `detect*Gestures` family were matched instead (see DECISIONS). New stubs: `draggable`, `Orientation`, `DraggableState` and two more gesture detectors.
- R-03 was dropped after the check CLAUDE.md asks for. Compose does not hide interactive children inside `semantics(mergeDescendants = true)`: merging stops at any child that is itself a merging root, and `AbstractClickableNode` (the base of every clickable modifier and so of every Material button) sets `shouldMergeDescendantSemantics = true`. The rule would only ever report correct code, so it is recorded as not applicable rather than implemented, agreed with Sedra on 2026-10-05 (see DECISIONS for the source evidence). The implemented taxonomy is therefore 26 rules plus R-03 documented as void. Layout Inspector was not available here, so the finding rests on the Compose 1.10.4 sources; worth confirming on a device before the thesis text is final.
- R-02 passed its unit tests and the sample app on the first run. One message covers whichever of the name, role and state the block failed to put back. **Question for Sedra:** CLAUDE.md asks to flag every empty `clearAndSetSemantics { }`, but an empty block is also the documented way to hide a decorative subtree, so that legitimate use is reported too. Should the empty case be narrowed to blocks whose content had a name or was interactive (see DECISIONS)?
- U-04 passed its unit tests and the sample app on the first run. The vague label list lives in one constant (`VAGUE_LABELS`) so the rule and the thesis cannot drift apart, and the whole trimmed label must match, so "Read more about shipping" stays clean while "more" is reported (see DECISIONS).
- U-03 started with the check CLAUDE.md asks for: Material **does** already set an `error(...)` semantic for `isError = true`, in both Material3 1.4.0 and Material 1.10.4, but only with its generic default message ("Error"). The rule therefore reports an error state with no specific message, exactly as CLAUDE.md words it. Its unit tests passed, but the sample app still reported the `semantics { error(...) }` field: a probe in the message showed `argument(call, "modifier")` returning the `onValueChange` argument, because Lint resolves `TextField` to the shifted `TextFieldState` overload. `ComposeCalls.argument` now reads a source-level named argument before the mapping (see DECISIONS). This is a shared fix, re-checked with the full unit suite and the full sample expectation check.
- O-05 passed its unit tests and the sample app on the first run, and was the first Milestone 3 rule that needed no retrofit of the earlier screens: its overlap with P-01 was decided when P-01 was built, so it skips any clickable holding an Icon or Image and only reports one with no Icon, no Image and no Text (see DECISIONS). The full expectations check is now 52 expected, 52 reported, 0 missing, 0 unexpected.
- O-04 completed the R-01 overlap that was prepared in Milestone 2: both rules visit the same node and both call `Clickables.isButtonLikeContainer`, R-01 to bail out and O-04 to continue, so exactly one of the two reports (see DECISIONS). One negative test first failed under the `REORDER_ARGUMENTS` test mode for the reason already recorded for P-01 (a trailing comma before a trailing lambda cannot be rewritten); the test was written without the trailing comma, and no test mode is disabled. O-04 then reported on eight role-less clickables in the O-03, P-01 and U-02 screens, which were given roles, the same retrofit R-01 and O-02 needed.
- O-02 passed its unit tests on the first run, but the sample app first reported 25 warnings instead of 5: the container list (`Row`/`Box`/`Column`/`Card`/`ListItem`) also matched `Modifier.clickable` calls built earlier for O-01, O-03, P-01, R-01 and U-02, on both bad and good screens. The same situation happened when R-01 was added (see PROGRESS, Milestone 2). Fixed by giving every one of those 20 calls an `onClickLabel`, the same way they were earlier given a `role`.
