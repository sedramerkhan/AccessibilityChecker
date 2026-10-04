# Rules

Catalog of the Compose accessibility rules. One section per rule. Every message starts with the taxonomy ID in brackets, for example `[P-01]`.

| ID | Issue ID | Severity | Detection | WCAG | Status |
|---|---|---|---|---|---|
| P-01 | `ComposeMissingContentDescription` | Error (Critical) | STATIC | 1.1.1 | Done |
| P-02 | `ComposeDecorativeImageLabeled` | Warning (Major) | STATIC_LLM | 1.1.1 | Done |
| P-03 | `ComposeLowContrastColors` | Warning (Major) | STATIC | 1.4.3 | Done |
| P-04 | `ComposeTextSizeInDp` | Warning (Major) | STATIC | 1.4.4 | Done |
| O-01 | `ComposeSmallTouchTarget` | Error (Critical) | STATIC | 2.5.8 | Done |
| O-03 | `ComposeNestedClickable` | Error (Critical) | STATIC | 2.4.3, 4.1.2 | Done |
| U-01 | `ComposeMissingHeading` | Error (Critical) | STATIC_LLM | 1.3.1, 2.4.6 | Done |
| U-02 | `ComposeMissingStateDescription` | Error (Critical) | STATIC | 4.1.2 | Done |
| U-05 | `ComposeTextFieldWithoutLabel` | Error (Critical) | STATIC | 1.3.1, 3.3.2 | Done |
| R-01 | `ComposeClickableWithoutRole` | Error (Critical) | STATIC_LLM | 4.1.2 | Done |

---

## P-02 · ComposeDecorativeImageLabeled

- **Taxonomy:** Perceivable, Major, STATIC_LLM.
- **Lint:** `Severity.WARNING`, priority 6, category `A11Y`.
- **WCAG 2.2:** 1.1.1 Non-text Content.
- **Detector:** `detectors/perceivable/P02DecorativeImageLabeledDetector.kt`
- **Message:** `[P-02] Possible decorative image label repeats visible text`.

### What it flags

A non-interactive Material `Icon` or foundation `Image` with a non-empty literal `contentDescription` that equals or is contained in a direct sibling `Text` literal in the same content lambda.

### What it ignores

- Interactive images.
- Resource, variable, or computed descriptions.
- Different descriptions and visible text.
- Text emitted by another composable function.

### Known limitations

Only direct sibling literal text is compared. Aliases, string resources, variables, and text in separately called composables are not followed.

### Tests and sample

- `P02DecorativeImageLabeledDetectorTest`: 2 positive and 4 negative cases.
- Sample: `sample-app/.../defects/p02/P02BadScreen.kt` and `P02GoodScreen.kt`.

---

## P-03 · ComposeLowContrastColors

- **Taxonomy:** Perceivable, Major, STATIC.
- **Lint:** `Severity.WARNING`, priority 6, category `A11Y`.
- **WCAG 2.2:** 1.4.3 Contrast (Minimum).
- **Detector:** `detectors/perceivable/P03LowContrastColorsDetector.kt`
- **Message:** `[P-03] Text color has a contrast ratio of X:1 against its background, below the required Y:1`.

### What it flags

A `Text` whose literal `color = Color(0x...)` has a WCAG contrast ratio below 4.5:1 (or 3:1 for text at 18sp and above, or 14sp and above when bold) against a literal background color. The background is read from the Text's own `Modifier.background(Color(...))`, or else from the single composable call whose content lambda directly contains the Text: a `Surface(color = ...)` or a `Box`/`Row`/`Column` with `Modifier.background(...)`.

### What it ignores

- Any non-literal text or background color, including `MaterialTheme.colorScheme...` and variables.
- A container that is not the Text's direct parent (no multi-level lookup).
- Text with no background found at all (own modifier, and direct parent, both without a literal background).

### Known limitations

Only one level of container is checked. A background set two levels up, or a background that comes from a `Modifier` passed in as a function parameter, is not seen (see `docs/LIMITATIONS.md`).

### Tests and sample

- `P03LowContrastColorsDetectorTest`: 4 positive and 6 negative cases.
- Sample: `sample-app/.../defects/p03/P03BadScreen.kt` and `P03GoodScreen.kt`.

---

## P-04 · ComposeTextSizeInDp

- **Taxonomy:** Perceivable, Major, STATIC.
- **Lint:** `Severity.WARNING`, priority 6, category `A11Y`.
- **WCAG 2.2:** 1.4.4 Resize Text.
- **Detector:** `detectors/perceivable/P04TextSizeInDpDetector.kt`
- **Message:** `[P-04] Text size is converted from a dp value, so it will not scale again when the user changes their system font size`.

### What it flags

A `fontSize` (on `Text` directly, or in a `TextStyle(...)`/`.copy(...)` passed as `style`) computed by calling `.toSp()` on a `Dp` value, for example `with(LocalDensity.current) { 16.dp.toSp() }` or `with(LocalDensity.current) { cardPadding.toSp() }`.

### What it ignores

- A literal or variable `sp` value.
- A Material typography style with no `fontSize` override.
- Any other expression as `fontSize` (its value is unknown, so it is not assumed to be a problem).

### Known limitations

Only a `with(density) { ... }` block or a bare `.toSp()` call is recognised. A `.toSp()` call made another way (for example `density.run { ... }`, or inside a separate function that returns the converted size) is not seen.

### Tests and sample

- `P04TextSizeInDpDetectorTest`: 4 positive and 6 negative cases.
- Sample: `sample-app/.../defects/p04/P04BadScreen.kt` and `P04GoodScreen.kt`.

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

---

## O-01 · ComposeSmallTouchTarget

- **Taxonomy:** Operable, Critical, STATIC.
- **Lint:** `Severity.ERROR`, priority 9, category `A11Y`.
- **WCAG 2.2:** 2.5.8 Target Size (Minimum). The threshold is the Android guideline of 48dp, which is stricter than the 24 CSS pixels of WCAG.
- **Detector:** `detectors/operable/O01SmallTouchTargetDetector.kt`
- **Message:** `[O-01] Clickable element is only 20dp wide and 20dp high, smaller than the 48dp minimum touch target` (only the axes below 48dp are named).
- **Reported at:** the size modifier call that makes the target too small (for example `size(20.dp)`).

### What it flags

A modifier chain with `clickable`, `combinedClickable`, `toggleable`, `triStateToggleable` or `selectable` whose clickable area is narrower or lower than 48dp, measured from literal sizes:

- A size set before the clickable modifier (`Modifier.size(20.dp).clickable { }`) is the clickable size, minus any padding between the two (`Modifier.size(48.dp).padding(8.dp).clickable { }` is 32dp).
- Without one, the first size set after it (`Modifier.clickable { }.size(32.dp)`) is used, plus any padding between the two.
- Sizes are read from `size`, `width`, `height`, `requiredSize`, `requiredWidth` and `requiredHeight`. Paddings from every `padding` overload and `absolutePadding`.
- A modifier chain stored in a local `val` of the same function is followed.

### What it ignores

- Chains that contain `minimumInteractiveComponentSize()` anywhere.
- Chains passed to Material components that enforce 48dp themselves: buttons, icon buttons, FABs, and `Card`/`Surface` with `onClick`.
- Sizes or paddings that are not literals, and axes set by `fillMaxWidth`, `widthIn`, `sizeIn`, `defaultMinSize`, `weight`, `aspectRatio` and similar. Such an axis is unknown and never reported.
- Elements without a click or toggle modifier.

### Example

Bad:

```kotlin
Icon(
    painter = closeIcon,
    contentDescription = "Close",
    modifier = Modifier.size(24.dp).clickable { onClose() },
)
```

Good:

```kotlin
IconButton(onClick = onClose) {
    Icon(closeIcon, contentDescription = "Close", modifier = Modifier.size(24.dp))
}

Box(Modifier.clickable { onClose() }.padding(12.dp).size(24.dp)) { ... } // 48dp target
```

### Known limitations

- The size of the parent and the layout constraints are not known. A small element inside a parent that stretches it (for example `Row` with `fillMaxHeight` children) may be reported.
- A modifier passed in as a parameter is not followed (see LIMITATIONS).
- `minimumInteractiveComponentSize()` anywhere in the chain suppresses the warning, even in an order where it would not help.
- Only `dp` literals are read. `DpSize`, values from `dimensionResource` and constants are unknown.

### Tests and sample

- `O01SmallTouchTargetDetectorTest`: 5 positive tests and 5 negative tests (13 cases in the negative tests).
- Sample: `sample-app/.../defects/o01/O01BadScreen.kt` (5 `// EXPECT` lines) and `O01GoodScreen.kt` (no reports).

---

## O-03 · ComposeNestedClickable

- **Taxonomy:** Operable, Critical, STATIC.
- **Lint:** `Severity.ERROR`, priority 9, category `A11Y`.
- **WCAG 2.2:** 2.4.3 Focus Order, 4.1.2 Name, Role, Value.
- **Detector:** `detectors/operable/O03NestedClickableDetector.kt`
- **Message:** `[O-03] Clickable IconButton is nested inside the clickable Card, so screen readers may skip it or announce both as one element`
- **Reported at:** the name of the inner element call (for example `IconButton`).

### What it flags

A clickable element inside the content lambda of another clickable element in the same function. Layout calls in between (`Row`, `Column`, a `Box` without a click modifier) are passed through. Clickable elements are the same as in P-01: click and toggle modifiers (`clickable`, `combinedClickable`, `toggleable`, `triStateToggleable`, `selectable`), Material buttons, icon buttons and FABs, and `Card`/`ElevatedCard`/`OutlinedCard`/`Surface` called with `onClick` or `onCheckedChange`.

Each inner clickable is reported once, against its nearest clickable ancestor. With three levels, the middle and the inner element are both reported.

### What it ignores

- Sibling clickables.
- Clickables inside non-clickable containers (`Card` or `Surface` without `onClick`, a list, a plain `Row`).
- The content of a Material button (`Icon` and `Text` are not clickable).
- Clickables inside a separate composable called from the content (not followed in Phase 2).
- `Checkbox`, `Switch` and `RadioButton` are not counted as clickable elements (see DECISIONS).

### Example

Bad:

```kotlin
Card(onClick = onOpen) {
    Text(article.title)
    IconButton(onClick = onFavorite) {
        Icon(Icons.Filled.Favorite, contentDescription = "Favorite")
    }
}
```

Good:

```kotlin
Card(
    onClick = onOpen,
    modifier = Modifier.semantics {
        customActions = listOf(CustomAccessibilityAction("Favorite") { onFavorite(); true })
    },
) {
    Text(article.title)
    FavoriteIcon() // not clickable on its own
}
```

### Known limitations

- Clickables inside another composable called from the content are not seen.
- Common card designs (a clickable card with a favourite or bookmark button) are reported. Whether they are true defects depends on how TalkBack groups them, so precision must be measured on the development apps.

### Tests and sample

- `O03NestedClickableDetectorTest`: 5 positive tests (8 reports) and 5 negative tests.
- Sample: `sample-app/.../defects/o03/O03BadScreen.kt` (4 `// EXPECT` lines) and `O03GoodScreen.kt` (no reports).

---

## U-01 · ComposeMissingHeading

- **Taxonomy:** Understandable, Critical, STATIC_LLM (a candidate: whether the text is really a heading needs a judgement about intent).
- **Lint:** `Severity.ERROR`, priority 9, category `A11Y`.
- **WCAG 2.2:** 1.3.1 Info and Relationships, 2.4.6 Headings and Labels.
- **Detector:** `detectors/understandable/U01MissingHeadingDetector.kt`
- **Message:** `[U-01] Possible heading: this Text is styled like a heading (headlineSmall) but has no heading() semantics, so screen reader users cannot jump to it`. The part in brackets names the style, or the size, for example `24sp bold`.
- **Reported at:** the name of the `Text` call.

### What it flags

A Material 3 or Material 2 `Text` that looks like a heading and whose own modifier has no `heading()` in `semantics { }` or `clearAndSetSemantics { }`. It looks like a heading when:

1. its `style` is `MaterialTheme.typography.<style>` (also with `.copy(...)`) and the style is `displayLarge`, `displayMedium`, `displaySmall`, `headlineLarge`, `headlineMedium`, `headlineSmall` or `titleLarge` (Material 2: `h1` to `h6`), or
2. its font size is at least 20sp and its font weight is bold (700 or more: `Bold`, `ExtraBold`, `Black`, `W700` to `W900`, or `FontWeight(700+)`). Size and weight are read from the `fontSize`/`fontWeight` arguments, or from a `TextStyle(...)` or `.copy(...)` passed as `style`.

### What it ignores

- Text with `heading()` in its own semantics.
- Text inside a Material button, icon button or FAB (it is the button label).
- Text inside a top app bar (`TopAppBar`, `CenterAlignedTopAppBar`, `MediumTopAppBar`, `LargeTopAppBar`, Material 2 `TopAppBar`). Note: Material3 does not mark these titles as headings (see DECISIONS).
- Body, label and other title styles, large text that is not bold, and bold text below 20sp.
- A style stored in a variable or passed in as a parameter.

### Example

Bad:

```kotlin
Text("Account settings", style = MaterialTheme.typography.headlineSmall)
```

Good:

```kotlin
Text(
    text = "Account settings",
    style = MaterialTheme.typography.headlineSmall,
    modifier = Modifier.semantics { heading() },
)
```

### Known limitations

- Large numbers, prices or display text in a heading style are reported although they are not headings. The message says "Possible", and Phase 3 will judge intent.
- `heading()` on a parent (for example a `Row` with `mergeDescendants = true` that holds the title) is not considered.
- Styles from variables, custom theme objects (`AppTheme.typography.title`) and a theme's own `TextStyle` constants are not recognised.

### Tests and sample

- `U01MissingHeadingDetectorTest`: 5 positive tests (6 reports) and 5 negative tests.
- Sample: `sample-app/.../defects/u01/U01BadScreen.kt` (4 `// EXPECT` lines) and `U01GoodScreen.kt` (no reports).

---

## U-02 · ComposeMissingStateDescription

- **Taxonomy:** Understandable, Critical, STATIC.
- **Lint:** `Severity.ERROR`, priority 9, category `A11Y`.
- **WCAG 2.2:** 4.1.2 Name, Role, Value.
- **Detector:** `detectors/understandable/U02MissingStateDescriptionDetector.kt`
- **Message:** `[U-02] Clickable element toggles expanded but exposes no state, so screen readers do not announce whether it is on or off` (names the flipped Boolean as written in the code).
- **Reported at:** the `clickable` or `combinedClickable` call (its name only).

### What it flags

`Modifier.clickable { }` or `Modifier.combinedClickable { }` whose click lambda flips a Boolean:

- `x = !x` (also with a delegated `var x by remember { mutableStateOf(false) }`),
- `x.value = !x.value`,
- a call to a function named `on...Change` or `on...Changed` with a negated argument, for example `onCheckedChange(!checked)`,

when the same modifier chain does not expose the state: no `toggleable`, `triStateToggleable` or `selectable` modifier, and no `stateDescription`, `toggleableState` or `selected` set in semantics.

### What it ignores

- Material `Switch`, `Checkbox` and `RadioButton`, and `Modifier.toggleable`/`selectable` (they expose state already).
- Click handlers that do not flip a Boolean (navigation, counters, `x = true`).
- Chains with state semantics.
- A chain passed to a Material clickable component.

### Example

Bad:

```kotlin
var expanded by remember { mutableStateOf(false) }
Row(Modifier.clickable { expanded = !expanded }) { Text("Details") }
```

Good:

```kotlin
Row(
    Modifier.toggleable(value = expanded, onValueChange = { expanded = it }),
) { Text("Details") }
```

### Known limitations

- Only the three flip patterns above are recognised. A flip inside a called function (`toggle()`), `x = x.not()`, `setX(!x)` from a destructured state, or a ViewModel method is not seen.
- Material clickable components with a flipping `onClick` (for example `IconButton(onClick = { favorite = !favorite })`) are not checked, because CLAUDE.md limits the rule to `Modifier.clickable`.

### Tests and sample

- `U02MissingStateDescriptionDetectorTest`: 4 positive tests and 4 negative tests (9 cases in the negative tests).
- Sample: `sample-app/.../defects/u02/U02BadScreen.kt` (3 `// EXPECT` lines) and `U02GoodScreen.kt` (no reports).

---

## U-05 · ComposeTextFieldWithoutLabel

- **Taxonomy:** Understandable, Critical, STATIC.
- **Lint:** `Severity.ERROR`, priority 9, category `A11Y`.
- **WCAG 2.2:** 1.3.1 Info and Relationships, 3.3.2 Labels or Instructions.
- **Detector:** `detectors/understandable/U05TextFieldWithoutLabelDetector.kt`
- **Message:** `[U-05] TextField has no label, so screen reader users are not told what to enter`. When a placeholder is set, the message adds `(the placeholder disappears when the user types)`.
- **Reported at:** the name of the text field call.

### What it flags

A Material 3 or Material 2 `TextField` or `OutlinedTextField` (every overload: `String`, `TextFieldValue` and `TextFieldState`) whose `label` argument is not passed or is the literal `null`. A `placeholder` alone does not count.

### What it ignores

- Fields with any `label` argument other than `null`, including a label passed in as a variable.
- Functions with the same name in other packages.

### Example

Bad:

```kotlin
OutlinedTextField(
    value = email,
    onValueChange = onEmailChange,
    placeholder = { Text("you@example.com") },
)
```

Good:

```kotlin
OutlinedTextField(
    value = email,
    onValueChange = onEmailChange,
    label = { Text("Email address") },
    placeholder = { Text("you@example.com") },
)
```

### Known limitations

- A field labelled through `Modifier.semantics { contentDescription = ... }` is still reported. It has a name for screen readers but no visible label, so 3.3.2 is still not met. Decide with Sedra if this should change.
- `BasicTextField` and custom field wrappers are not checked.
- A visible `Text` above the field is not linked to the field and does not count as a label.

### Tests and sample

- `U05TextFieldWithoutLabelDetectorTest`: 4 positive tests (5 reports) and 4 negative tests.
- Sample: `sample-app/.../defects/u05/U05BadScreen.kt` (3 `// EXPECT` lines) and `U05GoodScreen.kt` (no reports).

---

## R-01 · ComposeClickableWithoutRole

- **Taxonomy:** Robust, Critical, STATIC_LLM (a candidate: the right role depends on what the element is for).
- **Lint:** `Severity.ERROR`, priority 9, category `A11Y`.
- **WCAG 2.2:** 4.1.2 Name, Role, Value.
- **Detector:** `detectors/robust/R01ClickableWithoutRoleDetector.kt`
- **Message:** `[R-01] Possible missing role: clickable Row has no role, so screen readers do not say what kind of control it is` (names the element that receives the modifier, or says "clickable element" when the modifier is stored in a variable).
- **Reported at:** the `clickable` or `combinedClickable` call (its name only).

### What it flags

`Modifier.clickable` or `Modifier.combinedClickable` without a `role` argument (or with `role = null`), when no `role` is set in the semantics of the same modifier chain, on a custom element.

### What it ignores

- Click modifiers with a role, or chains that set `role` in `semantics { }`.
- Chains passed to a Material clickable component (`Button`, `IconButton`, FAB, `Card`/`Surface` with `onClick`, and the others in `Clickables`), which set their own role.
- The O-04 case (overlap policy, CLAUDE.md 7.1): a clickable `Box`, `Row` or `Column` whose direct content is one `Text`, or one `Icon`/`Image` and one `Text` (with optional `Spacer`s). O-04 will report it as "use Button".
- `toggleable` and `selectable`, which are not part of this rule.

### Example

Bad:

```kotlin
Row(Modifier.clickable { open(contact) }) {
    Text(contact.name)
    Text(contact.status)
}
```

Good:

```kotlin
Row(Modifier.clickable(role = Role.Button) { open(contact) }) {
    Text(contact.name)
    Text(contact.status)
}
```

### Known limitations

- A role set on a parent or child element is not considered.
- A `Card` or `Surface` without `onClick` that is made clickable with a modifier is reported, because it is then a custom clickable element without a role.
- The O-04 exclusion only reads the direct children of the content lambda. Button-like content inside a nested layout is treated as rich content.

### Tests and sample

- `R01ClickableWithoutRoleDetectorTest`: 4 positive tests (5 reports) and 4 negative tests.
- Sample: `sample-app/.../defects/r01/R01BadScreen.kt` (4 `// EXPECT` lines) and `R01GoodScreen.kt` (no reports).
