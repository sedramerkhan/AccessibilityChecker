# Rules

Catalog of the Compose accessibility rules. One section per rule. Every message starts with the taxonomy ID in brackets, for example `[P-01]`.

| ID | Issue ID | Severity | Detection | WCAG | Status |
|---|---|---|---|---|---|
| P-01 | `ComposeMissingContentDescription` | Error (Critical) | STATIC | 1.1.1 | Done |

---

## P-01 · ComposeMissingContentDescription

- **Taxonomy:** Perceivable, Critical, STATIC.
- **Lint:** `Severity.ERROR`, priority 9, category `A11Y`.
- **WCAG 2.2:** 1.1.1 Non-text Content.
- **Detector:** `detectors/perceivable/P01MissingContentDescriptionDetector.kt`
- **Message:** `[P-01] Interactive Icon has no contentDescription, so screen readers announce it without a name` (`Image` instead of `Icon` for images).
- **Reported at:** the `contentDescription` argument.

### What it flags

An `Icon` (Material3 or Material) or `Image` (foundation) whose `contentDescription` is the literal `null` or `""`, when the icon or image is the only possible label of a clickable element. That is the case when either:

1. its own `modifier` contains `clickable`, `combinedClickable`, `toggleable`, `triStateToggleable` or `selectable`, or
2. the nearest clickable element around it in the same function has no other name. Clickable elements are the Material buttons (`Button`, `TextButton`, `OutlinedButton`, `ElevatedButton`, `FilledTonalButton`), icon buttons (`IconButton`, `IconToggleButton`, `FilledIconButton`, `FilledTonalIconButton`, `OutlinedIconButton`), floating action buttons, `Card`/`ElevatedCard`/`OutlinedCard`/`Surface` called with `onClick` or `onCheckedChange`, and any composable whose `modifier` has one of the modifiers above. The element has no other name when its content has no `Text` with text, no other labelled `Icon` or `Image`, and nothing sets `contentDescription` in semantics (on the element or inside it).

### What it ignores

- An Icon with `null` inside a button that also has a `Text` (the icon is decorative, the button already has a name).
- An Icon with `null` next to another Icon that has a label.
- Non-interactive images with `null` or `""` (decorative images are allowed).
- `contentDescription = stringResource(...)`, a non-empty literal, or any other expression, including a variable that may be null.
- A label set with `Modifier.semantics { contentDescription = ... }` on the icon or on the clickable parent.

### Example

Bad:

```kotlin
IconButton(onClick = onDelete) {
    Icon(Icons.Filled.Delete, contentDescription = null)
}
```

Good:

```kotlin
IconButton(onClick = onDelete) {
    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_draft))
}

Button(onClick = onDelete) {
    Icon(Icons.Filled.Delete, contentDescription = null) // decorative
    Text(stringResource(R.string.delete))
}
```

### Overlap with other rules

O-05 (`ComposeEmptyClickable`) covers clickables with no readable content at all. When the clickable contains an unlabelled Icon or Image, P-01 reports the icon and O-05 must not report the clickable (see DECISIONS, 2026-10-03).

### Known limitations

- A `contentDescription` held in a variable is never reported, even when it may be null.
- The clickable parent must be in the same function. An Icon in a separate composable called from an `IconButton` is not seen.
- A Text or label inside another composable called from the clickable's content is not seen, so the rule cannot know that the clickable already has a name. (This can cause a false positive.)
- A positional `onClick` on `Card`/`Surface` (`Card(onAction) { }`) is not recognised on the compiled Material3 library. See LIMITATIONS.
- Custom icon wrappers (for example a project's own `AppIcon(...)` composable) are not recognised.

### Tests and sample

- `P01MissingContentDescriptionDetectorTest`: 6 positive tests (7 reported icons) and 6 negative tests.
- Sample: `sample-app/.../defects/p01/P01BadScreen.kt` (6 `// EXPECT` lines) and `P01GoodScreen.kt` (no reports).
