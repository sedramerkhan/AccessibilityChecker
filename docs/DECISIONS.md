# Decisions

## 2026-10-01

- Used AGP 9.4.1 plus 23 for the Lint artifact version, resulting in Lint 32.4.1.
- Used JDK 17 for `lint-rules` compilation, matching the project requirement and installed runtime.
- Placed the sample app lint configuration inside `android { lint { } }`, which is the supported DSL location for this AGP version.
- Milestone 0 uses a temporary marker rule only to prove registry loading. It must be removed before Milestone 1.
