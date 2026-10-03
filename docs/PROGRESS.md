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

Next: Milestone 2, starting with P-01 `ComposeMissingContentDescription`.
