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

**Question for Sedra:** should a `semantics { onClick(label = ...) }` on the parent count as a
name for P-01, or stay as it is? This is a good candidate for the Phase 3 layer to judge rather
than for tuning the static rule.

**U-01, `PostCards.kt:169`** (JetNews) reports the `title` slot of an `AlertDialog` as a possible
heading. Dialog titles may already be announced through the dialog's own pane semantics. U-01 is
a STATIC_LLM rule and reports candidates, so this is within its design, but it is worth checking
whether Material3 already handles dialog titles, the same check that was done for top app bars.

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

### Still open

1. **P-01**: does a parent's `semantics { onClick(label = ...) }` count as a name? Left as it is
   for now, since the rule is behaving per its recorded decision and this is a good candidate for
   the Phase 3 layer to judge rather than for tuning a static rule.
2. **U-01**: check whether Material3 gives `AlertDialog` titles their own semantics, the same
   check that was done for top app bars.

## A note for the evaluation

These two apps are development apps, used precisely so that tuning does not touch the evaluation
corpus. The three false positives above were found and fixed here, which is what this run is for.
The remaining 33 findings have not been individually verified beyond the 11 inspected, so they
should not be quoted as a precision figure without a full manual pass.
