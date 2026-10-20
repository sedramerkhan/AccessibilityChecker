# Development

How to build, test and measure the rules in this repository. The README is for people who use
the rules in their own app; this file is for working on them.

## Quick start

```bash
./gradlew :lint-rules:test        # run every rule against fake Compose code
./gradlew :sample-app:lintDebug   # run the rules on the sample app
```

The second command writes four reports to `sample-app/build/reports/`:

| File | Use |
|---|---|
| `lint-results-debug.html` | the readable one, good for a demo |
| `lint-results-debug.txt` | plain text |
| `lint-results-debug.xml` | parsed by `scripts/check_sample_expectations.py` |
| `lint-results-debug.sarif` | for external tooling |

### In Android Studio

The rules are ordinary Lint checks, so warnings appear inline in the editor with the full
explanation in the tooltip. Open any file under `sample-app/src/main/java/.../defects/` to see
them.

After changing a rule, Android Studio can keep showing old results. Run *File > Sync Project with
Gradle Files*, or trust the Gradle run and the unit tests.

## Checking the sample app against its expectations

Each deliberately bad line in the sample app is marked `// EXPECT: <IssueId>`. The script
compares those markers with the Lint report in both directions, so a rule that stops firing and a
rule that fires somewhere it should not are both caught:

```bash
./gradlew :sample-app:lintDebug
py scripts/check_sample_expectations.py   # python3 on other machines
```

Current state (2026-10-19): 26 issue IDs, 209 expected, 209 reported, 0 missing, 0 unexpected, of which 114 are U-07.

## Exporting the findings as JSON

Phase 3 and the evaluation read the findings as JSON rather than parsing the Lint XML. The format
is fixed by CLAUDE.md section 8 and must stay stable:

```bash
py scripts/lint_xml_to_json.py --out findings.json          # the sample app
```

```json
{
  "taxonomyId": "P-01",
  "issueId": "ComposeMissingContentDescription",
  "severity": "Error",
  "file": "sample-app/src/main/java/.../P01BadScreen.kt",
  "line": 42,
  "column": 9,
  "message": "[P-01] Interactive Icon has no contentDescription, ...",
  "detection": "STATIC"
}
```

`taxonomyId` is read from the `[P-01]` prefix every message carries, and `detection` from
`Taxonomy.kt`, so the export cannot drift from the taxonomy. Built-in Lint checks in the same
report are skipped. Several reports can be converted at once, which is what a corpus run needs,
and `--root` controls what the paths are relative to:

```bash
py scripts/lint_xml_to_json.py --root ../corpus --out corpus.json \
    ../corpus/app-a/app/build/reports/lint-results-debug.xml \
    ../corpus/app-b/app/build/reports/lint-results-debug.xml
```

Findings are sorted by file, line and column, so two runs of the same code give byte-identical
output and a diff shows only real changes.

## Measuring what the rules add to a build

```bash
bash scripts/measure_lint_time.sh 7      # 7 runs with the rules, 7 without
```

Each run is `:sample-app:lintDebug --rerun-tasks`. "Without" means the `lintChecks` dependency is
not applied at all (`-Pa11ylint.off=true`), so the figure covers loading the rules as well as
running them. The rules jar is built once before timing and reused (`-x :lint-rules:jar`), so
compiling the rules is not counted, just as a project using the published library never
compiles them. Runs alternate between the two, and the summary leads with a **trimmed mean**, the
fastest and slowest run of each side dropped.

Each pair starts from a fresh Gradle daemon and an untimed warm-up of both sides, so pairs do not
influence each other; 7 pairs take roughly 25 to 35 minutes.

Run it on an otherwise idle machine, plugged in. The script warns when the spread between runs is wider than
the difference it is measuring, and the number is then not worth quoting. See `DECISIONS.md`,
2026-10-14.

## Publishing

The library is published through JitPack as `com.github.sedramerkhan:a11ylint`
(https://jitpack.io/#sedramerkhan/AccessibilityChecker). JitPack builds a release when a git tag
is pushed, using `jitpack.yml`:

```bash
git tag 0.1.0
git push origin 0.1.0
```

To test a change in another project before tagging, publish locally and add `mavenLocal()` to
that project's repositories:

```bash
./gradlew :lint-library:publishToMavenLocal
```

## Repository layout

```
compose-a11y-lint/
├── lint-rules/      Kotlin JVM module: the detectors, the taxonomy and the shared utilities
├── lint-library/    Android library that packages the rules as an AAR
├── sample-app/      Compose app with a deliberately bad and a good screen per rule
├── scripts/         sample check, JSON export, build time measurement
└── docs/            RULES, DECISIONS, LIMITATIONS, PROGRESS, DEV_APP_RESULTS, DEVELOPMENT
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

## Toolchain

| | |
|---|---|
| JDK | 17 or newer |
| Gradle | 9.6.0 (wrapper included) |
| AGP | 9.4.1 |
| Lint | 32.4.1 (AGP + 23) |
| Kotlin | 2.2.10 |
| Compose | BOM 2026.02.01 (Compose 1.10.4, Material3 1.4.0) |

The Lint artifact version must always be the AGP version plus 23. The rules are checked against
the real Compose sources, not against assumptions (see `DECISIONS.md`).

## The other docs

| File | What is in it |
|---|---|
| [RULES.md](RULES.md) | the rule catalogue: what each flags, ignores and cannot see |
| [DECISIONS.md](DECISIONS.md) | every design choice and heuristic, dated, with the evidence |
| [LIMITATIONS.md](LIMITATIONS.md) | what static analysis cannot catch, with examples |
| [PROGRESS.md](PROGRESS.md) | status, what is left, open questions, thesis text to update |
| [DEV_APP_RESULTS.md](DEV_APP_RESULTS.md) | findings on JetNews and Jetchat |

## Scope of this phase

- No LLM code, no network calls, no API keys. The same input always produces the same warnings.
- No automatic code fixes. A rule may describe the fix in its explanation, but never rewrites code.
- The evaluation apps are never used while building rules. Only `sample-app` and the two
  development apps from `android/compose-samples` (JetNews and Jetchat) are allowed, cloned
  outside this repository.
