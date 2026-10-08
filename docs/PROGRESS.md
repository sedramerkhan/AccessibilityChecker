# Progress

## Where the project stands (2026-10-05)

| Milestone | Status |
|---|---|
| 0 Setup | Done |
| 1 Infrastructure | Done |
| 2 Critical rules (7) | Done, including the development app run (2026-10-06). |
| 3 Major rules (12) | Done: 11 implemented, R-03 dropped as not applicable. |
| 4 Minor rules (8) | In progress: P-05, P-07, O-06, O-07, U-07 and R-04 done. Left: U-06, R-05. |
| 5 Packaging and reporting | Started: publishing to mavenLocal works and is verified. Scripts and the final docs pass are open. |

24 detectors implemented, each registered in `Taxonomy.kt`, documented in `RULES.md` and covered
by a bad and a good sample screen. Across the whole sample app: 24 issue IDs, 173 expected,
173 reported, 0 missing, 0 unexpected. The jump from 77 to 171 of those is U-07 alone, which is
explained in the Milestone 4 notes.

The rules have now also been run on code we did not write: **33 findings across JetNews and
Jetchat**, after fixing the two rule defects that first run exposed in O-05 and R-02. See
[DEV_APP_RESULTS.md](DEV_APP_RESULTS.md). O-02, which CLAUDE.md expected to be noisy, produced
8 findings across two complete apps, and O-03 produced one, on a genuine defect.

### What is left, in order

1. **Milestone 4**: two minor rules left, U-06 and R-05. U-06 is the one
   that needs more than a detector: it is project-wide and needs Lint partial analysis.
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
6. **U-01**: should a dialog title be marked as a heading? The P-07 check found that
   `AlertDialog` sets a `paneTitle` on the dialog container (`AlertDialog.kt:171`), which is
   announced when the dialog opens, but that is not the same as marking the title Text with
   `heading()`. So U-01 reporting a dialog title as a possible heading is not wrong, it is a
   judgement about whether a heading adds anything once the pane is already announced.
7. **O-06**: should the rule also cover `combinedClickable`, `toggleable` and `selectable`? They
   take the same `enabled` parameter and have the same defect, but CLAUDE.md 7.4 names
   `clickable` only, so widening it is a taxonomy change.
8. **O-07 scope**: should a clickable `Box` or `Column` count as a container? It is the same
   defect, and O-02 already treats them as containers, but CLAUDE.md 7.4 names `Card`, `Surface`
   and `Row` for this rule.
9. **O-07 and O-03 together** give three warnings for one card (two nested-clickable errors plus
   the missing-actions warning). Acceptable, or should O-03 be suppressed on the children of a
   container O-07 already reports? See the Milestone 4 notes.
10. **U-07 scope**: should `onLongClickLabel` be read too? `combinedClickable` takes it and it is
    the same defect, but CLAUDE.md 7.4 names `onClickLabel`.
11. **U-07 and the sample corpus**: the 94 markers are now part of 28 screens, including good
    ones. Keep it that way (the honest reading), or quieten U-07 over the other screens somehow?
    See the Milestone 4 notes and DECISIONS before deciding.
12. **R-04 and list length**: a `Column` looping over a two-item list is reported exactly like one
    looping over two hundred. The size is almost never a literal, so it cannot be used as a
    threshold. Accept the noise, or require `verticalScroll`/`horizontalScroll` as well, which
    would miss short lists that happen to fit on screen? CLAUDE.md 7.4 says "often with
    verticalScroll", not "only with", so the rule does not require it today.

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

Status: done. The 7 rules were finished on 2026-10-03; the development app run that closes this
milestone was deferred in favour of Milestone 3 and was carried out on 2026-10-06, see
[DEV_APP_RESULTS.md](DEV_APP_RESULTS.md).

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

## Milestone 4: Minor rules

In progress.

| Rule | Status | Positive tests | Negative tests | Sample EXPECT lines |
|---|---|---|---|---|
| P-05 `ComposeTextOverImage` | Done | 2 | 5 | 2, all matched |
| P-07 `ComposeMissingPaneTitle` | Done | 2 | 5 | 2, all matched |
| O-06 `ComposeDisabledButClickable` | Done | 4 | 5 | 2, all matched |
| O-07 `ComposeMissingCustomActions` | Done | 5 | 4 | 3, all matched |
| U-07 `ComposeHardcodedA11yText` | Done | 5 | 5 | 94, all matched |
| R-04 `ComposeMissingCollectionInfo` | Done | 4 | 6 | 2, all matched |

Notes:
- P-07 began with the check CLAUDE.md asks for, and all three named Material overlays already set a `paneTitle`: `AlertDialog`, `ModalBottomSheet` and the modal drawers (so do `DatePicker`, `SnackbarHost`, `BasicTooltip` and `WideNavigationRail`). All are excluded. The real gap is one level down, in `androidx.compose.ui.window.Popup` and `Dialog`, which set none at all; those are what the rule reports. See DECISIONS for the source references.
- P-07's sample `ModalBottomSheet` needed `@OptIn(ExperimentalMaterial3Api::class)`. Until that was added the sample app did not compile, and because `lintDebug` then left the previous report in place, the expectation check was reading a **stale** report and appeared to show the rule finding nothing. Worth remembering: check for `BUILD SUCCESSFUL` explicitly, not just the task summary, and compare the report's timestamp when a result looks surprising.
- P-05's unit tests all passed, but the sample app caught a real scope error: the rule matched `Clickables.imageCalls`, which includes `Icon`, so it reported the O-04 bad screen's icon-and-label button as text over a picture. CLAUDE.md 7.4 says `Image`, and the rule now matches only that (see DECISIONS). A reminder that the sample app check earns its keep for every rule, not only for the ones that touch compiled-library behaviour.
- O-06 is the first rule that reasons about the *shape of a lambda body* rather than about arguments and modifiers, and the test modes found two UAST facts worth keeping (both in DECISIONS). `IF_TO_WHEN` rewrites every `if` into a `when`, so a detector that looks at conditions must accept `USwitchExpression` as well as `UIfExpression`. And Kotlin's implicit lambda return is a real node: a handler whose only statement is `if (enabled) { ... }` presents as a `UReturnExpression` wrapping an `if`, so the statements have to be unwrapped before their shape is tested. A third case, a condition with an `else` branch, was then excluded on its own merits: that is a choice between two actions, not a guard, and without the exclusion every two-way toggle written that way was reported. The full expectations check is 21 issue IDs, 70 expected, 70 reported, 0 missing, 0 unexpected.
- O-07 passed its unit tests and the sample app on the first run. Counting clickable **descendants** rather than direct children is what makes it match real code, since the favourite and share buttons of a list row are nearly always wrapped in an inner layout. Counting a clickable child without searching inside it gives the rule a useful property for free: only the nearest clickable container is reported, so a clickable `Row` inside a clickable `Card` produces one warning, not two (see DECISIONS, with a unit test for it). The full expectations check is now 22 issue IDs, 77 expected, 77 reported, 0 missing, 0 unexpected.
- **The O-03 overlap that DECISIONS parked on 2026-10-03 is now live, as predicted.** O-07 reports the container while O-03 reports each nested button, so a card with two icon buttons gives three warnings: two O-03 errors and one O-07 warning. They are different nodes, so CLAUDE.md 7.1 is satisfied, and they say different things: O-07's advice is literally the fix O-03's own explanation recommends. The `O03BadScreen.kt` sample now carries markers for both, which makes the arithmetic visible. **Question for Sedra:** is three warnings for one card acceptable, or should O-03 stop reporting children of a container that O-07 already covers? That changes a Critical rule whose precision has already been measured on the development apps, so it was not done unilaterally.
- **U-07 is the widest-reaching rule in the set, by a long way.** It reported **94 lines across 28 sample screens** on its first run, against 84 markers for the other 22 rules put together, and about half of those lines are on screens that are the *good* example for their own rule. Every one of them is a genuine hardcoded `contentDescription`, `onClickLabel`, `stateDescription`, `paneTitle`, error message or custom action label. The markers were added rather than the code rewritten, because several rules need those literals to work at all: P-02 compares a literal description with a sibling literal Text, U-04 reads a button's literal label, and P-01 distinguishes `null` from `""`. Converting the corpus to `stringResource` would have quietly disabled parts of three rules while looking like a tidy-up. The reasoning and the two rejected alternatives are in DECISIONS. The check is exact again: 23 issue IDs, 171 expected, 171 reported, 0 missing, 0 unexpected.
- **What that changes about the corpus:** a good screen is now good *with respect to its own rule* only. The screens already describe themselves that way ("O-06 must report nothing here"), but until U-07 every good screen happened to be clean for every rule, and that is no longer true. Worth stating in the thesis, because the sample app is presented as the rule corpus.
- **A finding worth a sentence in the thesis:** a corpus written deliberately and carefully for an accessibility study still carried hardcoded accessibility text on 94 lines. Nobody notices this defect in review because it is the text that is never drawn on screen.
- R-04 passed its unit tests and the sample app on the first run. It is the first rule that reasons about a **loop** rather than about a call's arguments or its modifier chain, and the guard that makes it precise is requiring the loop body to contain a `@Composable` call: without that, any `Column` that happens to add numbers up in a loop is reported as a list. Reusing O-07's "stop at a nested layout" walk gave the lazy-list exclusion for free, since the lazy lists sit in the same stop set, so a `Column` wrapped around a `LazyColumn` is not reported for the lazy list's items. The full expectations check is now 24 issue IDs, 173 expected, 173 reported, 0 missing, 0 unexpected.
- **Question for Sedra on O-06's scope:** `combinedClickable`, `toggleable` and `selectable` take the same `enabled` parameter and have exactly the same defect, but CLAUDE.md 7.4 names `clickable`, so the rule matches only that. Widening it would be a taxonomy change, which is why it was not done quietly.
