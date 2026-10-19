# Compose Accessibility Lint

Android Lint rules that find **accessibility problems in Jetpack Compose code** while you write
it: icons without a label, touch targets that are too small, clickable rows that TalkBack cannot
describe, text fields without a label, and 22 more.

The warnings appear in Android Studio as you type, and in the normal Lint reports on the command
line and in CI. Each one says what is wrong, who it affects, which WCAG 2.2 success criterion it
relates to, and what a correct version looks like.

```
HomeScreen.kt:35: Error: [P-01] Interactive Icon has no contentDescription, so screen
readers announce it without a name [ComposeMissingContentDescription from compose-a11y-lint]
            Icon(painter = icon, contentDescription = null)
                                                      ~~~~
```

## Installation

Add the JitPack repository in `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Add the library to the module that contains your Compose UI, for example `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.sedramerkhan:a11ylint:0.1.0")
}
```

Releases and build status: https://jitpack.io/#sedramerkhan/AccessibilityChecker

The library contains only Lint rules. It adds no code and no resources to your app.

## Usage

**In Android Studio** nothing else is needed. After a Gradle sync, the warnings appear in the
editor like built-in Lint checks, with the full explanation in the tooltip.

**On the command line or in CI**, run Lint as usual:

```bash
./gradlew :app:lintDebug
```

The reports are written to `app/build/reports/` (HTML, XML, and SARIF if you enable it). Our
findings end with `from compose-a11y-lint`, which is also the quickest way to confirm the rules
loaded.

## The rules

26 rules, grouped by the four WCAG principles: Perceivable, Operable, Understandable, Robust.
Every message starts with the rule's ID in brackets, for example `[P-01]`.

Rules marked **Possible** report a likely problem that depends on what you meant (for example,
whether a large text really is a heading). Their message starts with "Possible", and you should
check them before changing code.

| ID | Issue ID | Severity | Reports |
|---|---|---|---|
| P-01 | `ComposeMissingContentDescription` | Error | An interactive Icon or Image with no label |
| P-02 | `ComposeDecorativeImageLabeled` | Warning | Possible: an image label that repeats the text next to it |
| P-03 | `ComposeLowContrastColors` | Warning | Text and background colors below the WCAG contrast ratio |
| P-04 | `ComposeTextSizeInDp` | Warning | A font size converted from dp, which ignores the user's font scale |
| P-05 | `ComposeTextOverImage` | Warning | Possible: text drawn over an image with no background or scrim |
| P-06 | `ComposeMissingLiveRegion` | Warning | Possible: text that changes by itself with no live region |
| P-07 | `ComposeMissingPaneTitle` | Warning | A custom overlay or popup with no pane title |
| O-01 | `ComposeSmallTouchTarget` | Error | A clickable element smaller than 48dp |
| O-02 | `ComposeMissingOnClickLabel` | Warning | A clickable container with no click label |
| O-03 | `ComposeNestedClickable` | Error | A clickable element inside another clickable element |
| O-04 | `ComposeClickableContainer` | Warning | Possible: a clickable Box, Row or Column that should be a Button |
| O-05 | `ComposeEmptyClickable` | Warning | A clickable element with nothing readable inside |
| O-06 | `ComposeDisabledButClickable` | Warning | A click handler that checks "enabled" itself instead of passing it |
| O-07 | `ComposeMissingCustomActions` | Warning | Possible: a clickable card with inner buttons and no custom actions |
| U-01 | `ComposeMissingHeading` | Error | Possible: heading-styled text not marked as a heading |
| U-02 | `ComposeMissingStateDescription` | Error | A custom toggle that does not announce its state |
| U-03 | `ComposeMissingSemanticError` | Warning | A text field in error with no message saying what is wrong |
| U-04 | `ComposeVagueButtonLabel` | Warning | Possible: a button labeled "OK", "Click here", "Submit" and similar |
| U-05 | `ComposeTextFieldWithoutLabel` | Error | A text field with no label (a placeholder is not enough) |
| U-06 | `ComposeInconsistentLabels` | Warning | Possible: the same icon described differently in different places |
| U-07 | `ComposeHardcodedA11yText` | Warning | Accessibility text written as a literal instead of a string resource |
| R-01 | `ComposeClickableWithoutRole` | Error | Possible: a custom clickable element with no role |
| R-02 | `ComposeClearAndSetSemanticsLoss` | Warning | `clearAndSetSemantics` that removes a name, role or state |
| R-04 | `ComposeMissingCollectionInfo` | Warning | A list built with a loop that does not announce its size |
| R-05 | `ComposeMissingProgressRange` | Warning | A hand-drawn progress bar or slider that announces no value |
| R-06 | `ComposeComposableWithoutSemantics` | Warning | Possible: custom gestures that give TalkBack no action |

The full catalogue, with what each rule reports, what it ignores and a bad and a good example, is
in [docs/RULES.md](docs/RULES.md).

## Configuration

These are standard Lint settings, nothing specific to this library.

Turn a rule off, or change its severity, for a whole module:

```kotlin
android {
    lint {
        disable += "ComposeHardcodedA11yText"
        warning += "ComposeSmallTouchTarget"   // report as a warning instead of an error
    }
}
```

Suppress one finding in code:

```kotlin
@Suppress("ComposeMissingOnClickLabel")
@Composable
fun ArticleRow(...) { ... }
```

## Requirements

- An Android project that uses Jetpack Compose.
- Android Gradle Plugin 8.0 or newer.

## What the rules cannot see

The rules read your source code; they do not run the app. Some problems are therefore out of
reach, for example a label held in a variable that might be null, a modifier passed in from
another function, or content drawn by another composable that is called inside a clickable. The
full list, with examples, is in [docs/LIMITATIONS.md](docs/LIMITATIONS.md). Lint never changes
your code; each warning only describes the fix.

## About

This library is the static analysis part of a master's thesis at the Syrian Virtual University,
*An AI-Assisted Semantic Accessibility Analysis Framework for Jetpack Compose Applications*, by
Sedra Merkhan, supervised by Dr. Mazen Mustafa.

To build the rules from source or work on them, see [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).
