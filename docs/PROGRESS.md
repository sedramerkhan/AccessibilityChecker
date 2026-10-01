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

- Added the taxonomy enums and entry model.
- Added the WCAG contrast utility and reference tests.
- Added literal helpers for null, empty strings, resources, dp, and sp values.
- Added Compose call resolution helpers for composable detection, FQNs, arguments, and trailing lambdas.
- Added modifier-chain inspection and semantics assignment helpers.
- Added UI-scope detection that excludes state/effect and coroutine lambdas.
- Added minimal real-package Compose test stubs.
- Added the shared `A11yLintTest` base with automatic stubs and SDK-free execution.
- Contrast tests pass.
- Runtime utility probes remain pending until a real registered detector is available; the current `LintDetectorTest` harness does not dispatch the test-only UAST probe.
- UI-scope and literal-focused runtime tests remain pending.
