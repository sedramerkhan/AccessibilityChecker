# Development App Results

First run of the rules against code we did not write, on the two development apps CLAUDE.md
allows: **JetNews** and **Jetchat** from `android/compose-samples`. Run on 2026-10-06 with all 18
rules.

This is the first evidence of precision from real code. Everything before this was measured
against `sample-app`, which was written to match the rules, so it could only ever confirm them.

## How it was run

The apps were cloned **outside** this repository, as CLAUDE.md section 9 requires, and no code
from them is committed here:

```
StudioProjects/
├── AccessibilityChecker/          this repository
└── dev-apps/compose-samples/      shallow clone of android/compose-samples
```

The rules were consumed as a published artifact, not copied:

```bash
# in this repository
./gradlew :lint-library:publishToMavenLocal      # publishes org.svu.sedra:a11ylint:0.1.0
```

```kotlin
// in each app: settings.gradle.kts, inside dependencyResolutionManagement.repositories
mavenLocal()

// in each app: app/build.gradle.kts
implementation("org.svu.sedra:a11ylint:0.1.0")
```

```bash
./gradlew :app:lintDebug
```

Two changes were needed to make the apps build on this machine, neither of which affects the
findings:

- **Gradle 9.5.0 to 9.6.0.** Both apps' wrappers ask for 9.5.0, whose download timed out here.
  9.6.0 was already cached and both apps build with it. Their AGP is 9.3.1, ours is 9.4.1.
- **`local.properties`** copied from this repository, so the apps can find the Android SDK.

Both builds report `BUILD FAILED`, which is expected and not a problem: the apps leave
`abortOnError` at its default, and our rules report errors. The reports are still written.

## What was found

36 findings from our rules in total.

| Rule | JetNews | Jetchat | Total |
|---|---|---|---|
| R-01 `ComposeClickableWithoutRole` | 4 | 7 | 11 |
| O-02 `ComposeMissingOnClickLabel` | 2 | 6 | 8 |
| U-01 `ComposeMissingHeading` | 4 | 2 | 6 |
| P-01 `ComposeMissingContentDescription` | 1 | 1 | 2 |
| O-05 `ComposeEmptyClickable` | 1 | 1 | 2 |
| O-03 `ComposeNestedClickable` | 1 | 0 | 1 |
| U-05 `ComposeTextFieldWithoutLabel` | 1 | 0 | 1 |
| R-02 `ComposeClearAndSetSemanticsLoss` | 1 | 0 | 1 |
| O-04 `ComposeClickableContainer` | 0 | 1 | 1 |
| O-01 `ComposeSmallTouchTarget` | 0 | 1 | 1 |
| U-02 `ComposeMissingStateDescription` | 0 | 1 | 1 |
| R-06 `ComposeComposableWithoutSemantics` | 0 | 1 | 1 |
| **Total** | **15** | **21** | **36** |

Seven rules reported nothing on either app: P-02, P-03, P-04, P-06, U-03, U-04. That is a
plausible result rather than a worry, since these apps use theme colours, `sp` sizes, string
resources and descriptive button labels throughout, which is exactly what those rules ask for.

### On the two rules CLAUDE.md asked about

- **O-02 is not as noisy as feared.** 8 findings across two complete apps, second most common but
  far from a flood. No tuning looks necessary on this evidence.
- **O-03 fired once**, on a genuine defect (below). No sign of the false positives on card
  patterns that CLAUDE.md anticipated.

## Assessment

11 of the 36 findings were checked by reading the surrounding source. The rest are not disputed
but are also not individually verified, so the counts below are a floor, not a final precision
figure.

### Confirmed true positives

- **O-03, `PostCards.kt:157`** (JetNews). A clickable `Row` whose content includes an
  `IconButton` for the overflow menu. A real nested clickable, exactly the defect O-03 describes.
- **R-01, `PostCards.kt:187`** (JetNews). An `AlertDialog` confirm button built as a bare
  `Text` with `Modifier.clickable` and no role. TalkBack announces it as plain text.
- **U-05, `HomeScreens.kt:578`** (JetNews). The search field is an `OutlinedTextField` with a
  `placeholder` and no `label`, which is precisely the defect: the placeholder disappears as soon
  as the user types.
- **R-01 and O-02** on the clickable rows in `PostCards.kt` and `JetchatDrawer.kt`. These rows
  really do carry no role and no action label.

### Confirmed false positives: two rule defects to fix

**1. O-05 reports any clickable custom composable. Both of its findings are false positives.**

```kotlin
// JetNews, HomeScreens.kt:474
PostCardTop(post = post, modifier = Modifier.clickable(onClick = { navigateToPost(post.id) }))

// Jetchat, JetchatAppBar.kt:48
JetchatIcon(
    contentDescription = stringResource(id = R.string.navigation_drawer_open),
    modifier = Modifier.size(64.dp).clickable(onClick = onNavIconPressed).padding(16.dp),
)
```

Both elements are named: `PostCardTop` renders the post title, and `JetchatIcon` takes a
`contentDescription` of its own. The rule has an exclusion for content it cannot see into, but
that exclusion only inspects the element's **content lambdas**. When the clickable element *is
itself* an unknown composable, it has no content lambda, the walk finds nothing, and the rule
concludes there is nothing to read.

**Proposed fix:** skip when the element is a composable the rule cannot see into, that is, not
one of the layouts and Material components it knows. This is the same reasoning the existing
exclusion already uses, applied to the element as well as to its content. On this evidence it
would remove both false positives and no true positives.

**2. R-02 reports the deliberate "hide this subtree" pattern.**

```kotlin
// JetNews, PostCards.kt:124
BookmarkButton(
    isBookmarked = isFavorite,
    onClick = onToggleFavorite,
    // Remove button semantics so action can be handled at row level
    modifier = Modifier.clearAndSetSemantics {}.padding(vertical = 2.dp, horizontal = 6.dp),
)
```

The empty block is intentional, and the comment says so: the row above offers the action as a
custom accessibility action, so the inner button is silenced on purpose. This is the documented
way to do it.

This is the open question already recorded for R-02, now with evidence from Google's own sample.
**Proposed fix:** report an empty block only when the content had a name or was interactive. In
this case the rule cannot see inside `BookmarkButton`, so it would stay silent.

### Arguable, worth a decision

**P-01, `JetnewsIcons.kt:56`** (JetNews):

```kotlin
IconToggleButton(
    modifier = Modifier.semantics {
        // We only want to override the label, not the actual action, so for the action we pass null.
        this.onClick(label = clickLabel, action = null)
    },
) {
    Icon(painter = ..., contentDescription = null) // handled by click label of parent
}
```

The authors believe the icon is covered by the parent's click label, and say so in a comment. Our
rule disagrees, and consistently with its own recorded decision: an action label describes what
activating the control does, not what the control is, so the element still has no name. By WCAG
this is arguably a true positive, but developers will read it as a false positive, and it comes
from an expert-written sample.

**Answered on 2026-10-13: it stays as it is, and this is a true positive.** `onClick(label, action)`
writes `SemanticsActions.OnClick` (`SemanticsProperties.kt:1251`) and never touches
`ContentDescription`, so the button has no name; the label only changes how the action is
announced. `Modifier.clickable(onClickLabel = ...)` is the same property by the same code path
(`Clickable.kt:1597`), so an exception here would have reversed P-01's recorded decision for
every clickable. A test now pins the behaviour. See `DECISIONS.md`.

**U-01, `PostCards.kt:169`** (JetNews) reports the `title` slot of an `AlertDialog` as a possible
heading. **Checked on 2026-10-12:** `AlertDialog.kt:171` sets a `paneTitle` on the dialog
container, which is announced when the dialog opens, but nothing marks the title as a heading.
U-01 is a STATIC_LLM rule and reports candidates, so the finding stays. The check also uncovered
why this finding exists at all: Material supplies the title style internally, so U-01 sees only
titles a developer restyled by hand. See `DECISIONS.md` and `LIMITATIONS.md`.

## What this changed

The two rule defects were fixed, with Sedra's agreement, on 2026-10-06. The design of each fix is
in `DECISIONS.md`.

### After the fixes

| | Before | After | Change |
|---|---|---|---|
| JetNews | 15 | **13** | O-05 and R-02 false positives gone |
| Jetchat | 21 | **20** | O-05 false positive gone |
| **Total** | **36** | **33** | the 3 confirmed false positives, and nothing else |

| Rule | JetNews | Jetchat | Total |
|---|---|---|---|
| R-01 `ComposeClickableWithoutRole` | 4 | 7 | 11 |
| O-02 `ComposeMissingOnClickLabel` | 2 | 6 | 8 |
| U-01 `ComposeMissingHeading` | 4 | 2 | 6 |
| P-01 `ComposeMissingContentDescription` | 1 | 1 | 2 |
| O-03 `ComposeNestedClickable` | 1 | 0 | 1 |
| U-05 `ComposeTextFieldWithoutLabel` | 1 | 0 | 1 |
| O-01 `ComposeSmallTouchTarget` | 0 | 1 | 1 |
| U-02 `ComposeMissingStateDescription` | 0 | 1 | 1 |
| O-04 `ComposeClickableContainer` | 0 | 1 | 1 |
| R-06 `ComposeComposableWithoutSemantics` | 0 | 1 | 1 |
| **Total** | **13** | **20** | **33** |

Every remaining finding is unchanged from the first run, so no true positive was lost. The fixes
were also checked against the unit tests, each with a new regression test written from the real
code above, and against the sample app, which stayed at 64 expected and 64 reported with
0 missing and 0 unexpected.

### Both follow-ups are now closed

1. **P-01** and the parent's `semantics { onClick(label = ...) }`: the rule is unchanged and the
   finding is a true positive, settled against the Compose sources and pinned by a test
   (2026-10-13).
2. **U-01** and `AlertDialog` titles: Material3 sets a `paneTitle` on the dialog container but
   never a heading, so the candidate stays (2026-10-12).

Both are recorded in `DECISIONS.md` with the source references.

## A note for the evaluation

These two apps are development apps, used precisely so that tuning does not touch the evaluation
corpus. The three false positives above were found and fixed here, which is what this run is for.
The remaining 33 findings have not been individually verified beyond the 11 inspected, so they
should not be quoted as a precision figure without a full manual pass.

---

# Second run: 2026-10-14, all 26 rules

The first run was made on 2026-10-06 with the 18 rules that existed then. This second run was
made after Milestone 4 completed the taxonomy and after four scope changes were agreed on
2026-10-12 and 2026-10-13 (O-03's selection controls, and the widening of O-06, U-07 and O-07).
Its purpose is to measure what those scope changes cost in precision, which is the reason the
development apps exist.

Same method as the first run, with the rules republished to `mavenLocal()` first.

## What changed

| Rule | 2026-10-06 | 2026-10-14 | Change |
|---|---|---|---|
| R-01 `ComposeClickableWithoutRole` | 11 | 11 | none |
| O-02 `ComposeMissingOnClickLabel` | 8 | 8 | none |
| U-01 `ComposeMissingHeading` | 6 | 6 | none |
| P-01 `ComposeMissingContentDescription` | 2 | 2 | none |
| O-03 `ComposeNestedClickable` | 1 | 1 | none |
| U-05 `ComposeTextFieldWithoutLabel` | 1 | 1 | none |
| O-01 `ComposeSmallTouchTarget` | 1 | 1 | none |
| U-02 `ComposeMissingStateDescription` | 1 | 1 | none |
| O-04 `ComposeClickableContainer` | 1 | 1 | none |
| R-06 `ComposeComposableWithoutSemantics` | 1 | 1 | none |
| R-04 `ComposeMissingCollectionInfo` | — | 4 | new rule |
| U-07 `ComposeHardcodedA11yText` | — | 3 | new rule |
| **Total** | **33** | **40** | **+7** |

Per app: JetNews 13 to 17, Jetchat 20 to 23.

## The result the run was made for

**The four scope changes produced no new findings at all.** Every one of the +7 comes from two
rules that did not exist in the first run, R-04 and U-07. Each rule that existed in both runs
reports exactly the same number in both.

Specifically:

- **O-06** widened from `clickable` to all five modifiers that take `enabled`: **0 findings**, as
  before.
- **O-07** widened to clickable `Box` and `Column`, the change expected to carry the most risk:
  **0 findings**, as before.
- **U-07** widened to `onLongClickLabel` and the `onClick`/`onLongClick` action labels: its 3
  findings are all plain `contentDescription` literals (`Conversation.kt:374, 383, 392`), so
  **none** comes from the widened part.
- **O-03** widened to Material selection controls with a live callback: still 1 finding, the same
  one.

So on this evidence the scope decisions of 2026-10-12 and 2026-10-13 cost nothing in precision.
They also found nothing new here, which is the honest other half: two apps are a small sample,
and the patterns the widenings target (a guard inside a `toggleable`, a clickable `Box` with two
buttons, a hardcoded action label) simply do not occur in these two. The widenings rest on the
source analysis recorded in `DECISIONS.md`, and this run shows they are not noisy.

## Two observations worth keeping

**U-07's three findings are debatable, in an interesting way.** All three are the names of
fictional people used as avatar descriptions: `contentDescription = "Ali Conors"`. A proper name
usually should not be translated, so these are arguably false positives for the localization
defect the rule describes, even though each is literally a hardcoded accessibility string. The
rule cannot tell a name from a label, and nothing at the call site could. This is a good example
of a defect class where Layer 3 judges better than any static rule, and it belongs in the error
analysis rather than in a rule change.

**Fourteen of the twenty-six rules report nothing on either app.** That is not a defect: these
are two small, carefully written Google samples, and rules such as P-03 (literal low contrast),
P-07 (custom overlays without a pane title) and R-05 (hand-drawn progress) target patterns that
good code avoids. It does mean the development apps cannot exercise most of the taxonomy, and
that the evaluation corpus has to be large enough to reach the rules these two never touch.

## A note on the build

`:app:lintDebug` ends with `BUILD FAILED` on JetNews because the rules report `Severity.ERROR`
issues and the app leaves `abortOnError` at its default. The lint reports are written in full
before the build fails, so the run is complete; the failure is the rules doing their job, not a
broken build. For an evaluation harness, set `abortOnError = false` so a corpus run does not stop
at the first app with an error-level finding.
