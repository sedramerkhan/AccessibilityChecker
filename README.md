# Compose Accessibility Lint

Custom Android Lint rules that find **semantic accessibility defects in Jetpack Compose source
code**, while the developer is writing it.

This repository is the implementation for a master's thesis at the Syrian Virtual University:

> **An AI-Assisted Semantic Accessibility Analysis Framework for Jetpack Compose Applications**
> Student: Sedra Merkhan · Supervisor: Dr. Mazen Mustafa

The framework has four layers. **This repository is Layer 1**, and it is deliberately complete on
its own: it is fully deterministic, works offline, and contains no LLM, network or API code.

| Layer | What it does | Where |
|---|---|---|
| 1 Static rule engine | Lint rules detect defects in Kotlin UAST, with file, line and severity | **this repository** |
| 2 Semantic context | Extracts a code slice around each defect | Phase 3 |
| 3 Grounded LLM reasoning | Explains the defect and recommends a fix | Phase 3 |
| 4 Developer feedback | Shows enriched warnings in Android Studio | Phase 3 |

## What it finds

18 rules are implemented, each mapped to a WCAG 2.2 success criterion and organised by the POUR
principles. Some examples:

- **P-01** an interactive `Icon` with no `contentDescription`, so TalkBack only says "button"
- **O-01** a clickable element smaller than the 48dp minimum touch target
- **O-05** a clickable with nothing readable inside, announced without a name
- **U-03** a text field in its error state with no message saying what is wrong
- **R-06** a custom gesture that gives accessibility services no action at all

The full catalogue, with what each rule flags and ignores, is in **[docs/RULES.md](docs/RULES.md)**.

Every message carries its taxonomy ID, which Phase 3 parses:

```
P01BadScreen.kt:35: Error: [P-01] Interactive Icon has no contentDescription, so screen
readers announce it without a name [ComposeMissingContentDescription from compose-a11y-lint]
            Icon(painter = icon, contentDescription = null)
                                                      ~~~~
```

## Quick start

```bash
./gradlew :lint-rules:test        # run every rule against fake Compose code
./gradlew :sample-app:lintDebug   # run the rules on the sample app
```

The second command writes four reports to `sample-app/build/reports/`:

| File | Use |
|---|---|
| `lint-results-debug.html` | the readable one, good for a demo |
| `lint-results-debug.txt` | plain text, as shown above |
| `lint-results-debug.xml` | parsed by `scripts/check_sample_expectations.py` |
| `lint-results-debug.sarif` | for external tooling |

### In Android Studio

No commands needed. The rules are ordinary Lint checks, so warnings appear inline in the editor
with the squiggle and the full explanation in the tooltip, exactly like built-in checks. Open any
file under `sample-app/src/main/java/.../defects/` to see them.

After changing a rule, Android Studio can keep showing old results. Run *File > Sync Project with
Gradle Files*, or trust the Gradle run and the unit tests.

### Checking the sample app against its expectations

Each deliberately bad line in the sample app is marked `// EXPECT: <IssueId>`. The script
compares those markers with the Lint report in both directions, so a rule that stops firing and a
rule that fires somewhere it should not are both caught:

```bash
./gradlew :sample-app:lintDebug
python3 scripts/check_sample_expectations.py
```

Current state: 18 issue IDs, 64 expected, 64 reported, 0 missing, 0 unexpected.

## Using the rules in another project

The rules are published as an AAR, so no source copying is needed:

```bash
./gradlew :lint-library:publishToMavenLocal
```

Then in the other project:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

// app/build.gradle.kts
dependencies {
    implementation("org.svu.sedra:a11ylint:0.1.0")
}
```

Run `./gradlew :app:lintDebug` there. Findings tagged `from compose-a11y-lint` are ours; that
suffix is the quickest way to confirm the rules actually loaded.

## Repository layout

```
compose-a11y-lint/
├── lint-rules/      Kotlin JVM module: the detectors, the taxonomy and the shared utilities
├── lint-library/    Android library that packages the rules as an AAR (org.svu.sedra:a11ylint)
├── sample-app/      Compose app with a deliberately bad and a good screen per rule
├── scripts/         check_sample_expectations.py
└── docs/            RULES, DECISIONS, LIMITATIONS, PROGRESS, DEV_APP_RESULTS
```

Inside `lint-rules`:

```
org/svu/sedra/a11ylint/
├── ComposeA11yIssueRegistry.kt   registers every issue from the taxonomy
├── taxonomy/                     Pour, DetectionType, Priority, TaxonomyEntry, Taxonomy
├── util/                         ComposeCalls, ModifierChain, Clickables, Literals, Contrast, ...
└── detectors/                    perceivable/ operable/ understandable/ robust/
```

`Taxonomy.kt` is the single source of truth for rule metadata: the registry reads it, and Phase 3
will use it to map results back to taxonomy IDs.

## Requirements

| | |
|---|---|
| JDK | 17 or newer |
| Gradle | 9.6.0 (wrapper included) |
| AGP | 8.8.2 |
| Lint | 31.8.2 (AGP + 23) |
| Kotlin | 2.2.10 |
| Compose | BOM 2026.02.01 (Compose 1.10.4, Material3 1.4.0) |

The Lint artifact version must always be the AGP version plus 23. The rules are checked against
the real Compose sources, not against assumptions: several rules exist in their current form
because the compiled libraries behave differently from the source stubs (see
[docs/DECISIONS.md](docs/DECISIONS.md)).

## Documentation

| File | What is in it |
|---|---|
| [docs/RULES.md](docs/RULES.md) | the rule catalogue: what each flags, ignores and cannot see |
| [docs/DECISIONS.md](docs/DECISIONS.md) | every design choice and heuristic, dated, with the evidence |
| [docs/LIMITATIONS.md](docs/LIMITATIONS.md) | what static analysis cannot catch, with examples |
| [docs/PROGRESS.md](docs/PROGRESS.md) | status, what is left, open questions, thesis text to update |
| [docs/DEV_APP_RESULTS.md](docs/DEV_APP_RESULTS.md) | findings on the development apps (pending) |

**Start with [docs/PROGRESS.md](docs/PROGRESS.md)**: it opens with where the project stands, what
is left in order, and the questions waiting on a decision.

## Status

| Milestone | Status |
|---|---|
| 0 Setup | Done |
| 1 Infrastructure | Done |
| 2 Critical rules (7) | Rules done; the development app run is still open |
| 3 Major rules (12) | Done: 11 implemented, R-03 dropped as not applicable |
| 4 Minor rules (8) | Not started |
| 5 Packaging and reporting | Publishing works; scripts and the final docs pass are open |

One rule, **R-03**, was dropped after checking the framework: Compose does not hide interactive
children inside `semantics(mergeDescendants = true)`, because merging stops at any child that is
itself a merging root and every clickable modifier is one. The defect cannot occur, so no
detector was written. The evidence is in `docs/DECISIONS.md`.

## Scope of this phase

- No LLM code, no network calls, no API keys. The same input always produces the same warnings.
- No automatic code fixes. The scope is detection and explanation; a rule may describe the fix in
  its explanation text, but never rewrites code.
- The evaluation apps are never used while building rules. Only `sample-app` and the two
  development apps from `android/compose-samples` (Jetnews and Jetchat) are allowed, and those are
  cloned outside this repository.
