# Thesis updates from the implementation (Phase 2)

This file is a hand-over for updating the thesis document. It lists every place where the
implementation now differs from what the thesis (and the original CLAUDE.md specification) says,
what the new text should say, and why. It also lists the numbers that are ready to quote and the
ones that are not.

Status on 2026-10-22: Phase 2 (Layer 1, the static Lint rules) is complete, including the build
overhead measurement (section 4). Full evidence for every point is in `DECISIONS.md`, by date.

---

## 1. The taxonomy: 26 rules plus one not applicable

The taxonomy keeps its 27 entries, but **R-03 is recorded as not applicable** and has no
detector, so **26 rules are implemented**.

**R-03 `ComposeMergeHidesInteractive`.** The defect it describes cannot occur in Compose 1.10.4.
When a container uses `semantics(mergeDescendants = true)`, merging stops at any child that merges
its own descendants, and every click modifier does that. So an interactive child is never hidden
inside a merged container; it stays a separate stop for TalkBack. This was established by reading
the Compose sources (`SemanticsNode.kt`), not by assumption. Suggested wording: "R-03 was dropped
after analysis showed that the framework already prevents the defect."

---

## 2. Rule wording that changed

Each change below was agreed with Sedra. The "old" text is what the taxonomy says now.

| Rule | Old wording | New wording | Why |
|---|---|---|---|
| R-02 | Flags any empty `clearAndSetSemantics { }` | Flags an empty block only when the content had a name or an interaction to lose | Google's own JetNews uses an empty block deliberately and correctly; flagging it unconditionally was a false positive |
| R-05 | A `Float` parameter "named like progress, percent, fraction or value" | A `Float` parameter whose name **contains** progress, percent or fraction, or **is exactly** `value`; `Modifier.progressSemantics` counts as setting the range | `alphaValue` or `animatedValue` drawn on a Canvas is not progress; but a hand-drawn slider with `value: Float` has exactly this defect (Material's own Slider sets the range) |
| R-06 | A composable that "contains clickable or toggleable UI and sets no semantics" | A composable that handles input through **custom gestures** (`pointerInput`, `detectTapGestures`, `draggable`, `anchoredDraggable`) and sets no semantics | Custom gestures give TalkBack no action at all, which is the real defect; clickable and toggleable UI is covered by other rules |
| O-06 | `Modifier.clickable` guarded by a condition without `enabled` | Any of the five click and toggle modifiers that take `enabled`: `clickable`, `combinedClickable`, `toggleable`, `triStateToggleable`, `selectable` | All five have the identical defect |
| O-07 | A clickable `Card`, `Surface` or `Row` with two or more clickable children | Any clickable container, including `Column` and `Box` | The defect does not depend on the layout |
| U-06 | The same `Icons.X.Y` used with different descriptions | The same icon however it is loaded: `Icons.X.Y`, a drawable read at the call site, or any property returning an `ImageVector` or `Painter` | A search of all 364 Kotlin files in `android/compose-samples` found 123 `Icon(...)` calls and **zero** uses of `Icons.X.Y`; the original wording could not have fired on real code |
| U-07 | Literals in `contentDescription`, `stateDescription`, `onClickLabel`, `paneTitle`, `error(...)`, custom action labels | Also `onLongClickLabel` and the labels of the `onClick` and `onLongClick` semantics actions | These write the same property as `onClickLabel` |
| O-01 | Clickable element below 48dp, Error | Below **24dp**: Error (fails WCAG 2.5.8). From 24dp up to 48dp: Warning (meets WCAG, below the Android 48dp guideline) | WCAG 2.5.8's minimum is 24 by 24; the old rule called a 32dp target a WCAG failure, which it is not |
| U-01 | WCAG 1.3.1, 2.4.6 | WCAG **1.3.1 only** | 2.4.6 requires headings to be descriptive, not to be marked as headings |
| P-01 | WCAG 1.1.1 | WCAG 1.1.1 **and 4.1.2** | An interactive icon with no description leaves the control without an accessible name |
| U-05 | Text field without a label, Error | No name at all: Error. Named only through `contentDescription` in semantics (no visible label): Warning | The second case has a name for screen readers; only the visible label (3.3.2) is missing |
| O-03 | Nested clickables: "TalkBack may merge them or skip the inner one" | Nested clickables give **two overlapping touch targets**, and the outer element does not mention the second action; TalkBack reaches both as separate stops | The old claim contradicts the R-03 finding: Compose keeps the inner element separately focusable |

---

## 3. Severity policy (new, important for the methodology chapter)

**Policy:** a Lint error fails the build of the project that uses the rules (`abortOnError` is on
by default; both development app builds ended in `BUILD FAILED` because of our errors). So:

- **Error = a definite WCAG failure.**
- **Warning = a candidate that needs a judgment of intent (STATIC_LLM), or a platform guideline
  rather than a WCAG criterion.**

This replaces the original mapping "Critical = Error". The **taxonomy priorities do not change**:
Critical rules keep Lint priority 9, so they still rank above Major and Minor rules. Only the
reported severity changed for these:

| Rule | Taxonomy priority | Reported as | Reason |
|---|---|---|---|
| P-01 | Critical | Error | Definite failure (no name) |
| U-02 | Critical | Error | Definite failure (state not exposed) |
| U-05 | Critical | Error, or Warning when named only in semantics | See section 2 |
| O-01 | Critical | Error below 24dp, Warning from 24 to 47dp | See section 2 |
| U-01 | Critical | Warning | Candidate ("Possible heading") |
| R-01 | Critical | Warning | Candidate ("Possible missing role") |
| O-03 | Critical | Warning | Usability defect, not a definite WCAG failure |

All Major and Minor rules are warnings, as before.

**Effect on the development apps** (computed by applying the new severities to the 40 findings of
the 2026-10-14 run, not from a new run): errors drop from **23 of 40 to 4 of 40** (P-01 twice,
U-05 once, U-02 once). The other 36 are warnings.

---

## 4. Numbers ready to quote

| What | Value |
|---|---|
| Rules implemented | 26 (27 in the taxonomy, R-03 not applicable) |
| Unit tests | 311 tests in 32 test classes, all passing |
| Tests per rule | at least 4 positive and 4 negative each |
| Sample app check | 26 issue IDs, 209 expected findings, 209 reported, 0 missing, 0 unexpected |
| Of those, U-07 | 114 (the other 25 rules: 95) |
| Development apps (JetNews, Jetchat) | 40 findings: JetNews 17, Jetchat 23 (run of 2026-10-14) |
| Rules with no findings on either app | 14 of 26 |
| Rule defects found on the development apps | 2 (in O-05 and R-02), both fixed after the first run |
| Build overhead (Plan section 6.5) | median 8.8 s added per `lintDebug` run on the sample app, 95% CI for the mean 7.4 to 12.0 s, 7 paired runs; about 42% of a median 21.1 s run without the rules |

**On the build overhead.** Measured on 2026-10-22 with a fresh Gradle daemon before each pair and
the two runs of a pair back to back, so background load cancels within a pair. The figure covers
the whole Gradle task, including loading the rules. The sample app is small but dense in defects,
so the relative figure is specific to it. It was measured on one laptop with other programs open
but idle; state its processor and memory. Two earlier attempts were rejected and are documented in
`DECISIONS.md` (2026-10-19 and 2026-10-20).

**On U-07 and the sample app.** Give the sample totals both with and without U-07 (209 and 95).
U-07 reports hardcoded accessibility text, and the sample app uses literals almost everywhere
(other rules need them), so it dominates the count. This is also a small finding of its own: a
corpus written on purpose for an accessibility study still had hardcoded accessibility text on
114 lines, because that text is never drawn on screen and nobody notices it.

**On the development apps.** These are two apps only, and only 11 of the first 36 findings were
checked by hand. They are a tuning record, not a precision figure. Precision comes from the
evaluation corpus (15 to 30 apps), which has not been run yet.

---

## 5. Implementation facts for the implementation chapter

- **Toolchain:** AGP 9.4.1, Lint 32.4.1 (always AGP + 23), Kotlin 2.2.10, Compose BOM 2026.02.01
  (Compose 1.10.4, Material3 1.4.0), JDK 17.
- **Supported projects:** Android Gradle Plugin 8.0 or newer (registry `minApi = 14`). This is a
  stated scope limit, not an untested risk.
- **Distribution:** the rules are packaged as an Android library (AAR) and published through
  JitPack as `com.github.sedramerkhan:a11ylint:0.1.0`
  (https://jitpack.io/#sedramerkhan/AccessibilityChecker). Not public yet: the repository will be
  made public and tagged `0.1.0` after checking with the supervisor, right before the evaluation,
  so the evaluated version is fixed.
- **Output formats:** HTML, XML, SARIF and plain text from Lint, plus a JSON export for Phase 3 and
  the evaluation (`scripts/lint_xml_to_json.py`). Every message starts with the taxonomy ID, for
  example `[P-01]`, and every candidate message starts with "Possible".
- **One rule, two severities:** O-01 and U-05 report one issue at two severities using Lint's
  per-finding `Incident.overrideSeverity`, so the taxonomy keeps one entry per defect.
- **No automatic fixes, no network, no LLM code** in Layer 1, as specified.

---

## 6. Not ready yet: do not write these numbers

- **Evaluation results** (precision on the corpus). Not run yet.

---

## 7. Things the thesis should not claim

- That O-03 hides or merges the inner element (it does not; section 2).
- That every target below 48dp fails WCAG (only below 24dp does; section 2).
- That all Critical rules are reported as errors (section 3).
- A precision figure from the two development apps (section 4).
