# Limitations

Patterns that the static rules cannot see, each with a short example. Rule-specific limitations are added as rules are implemented.

## Shared utilities (all rules)

### Modifiers passed in from another function

A modifier that comes from a function parameter is not followed. Rules only see the calls written in the same expression, or in a local `val` of the same function.

```kotlin
@Composable
fun DeleteIcon(modifier: Modifier) {
    // The caller passed Modifier.clickable { }, but this function cannot see it.
    Icon(Icons.Filled.Delete, contentDescription = null, modifier = modifier)
}
```

### Modifiers stored in a `var`, a property or a conditional

A local `var` is not followed, because it may be reassigned. Class properties, top-level values, and `if`/`when` expressions that choose between modifiers are not followed either.

```kotlin
val modifier = if (editable) Modifier.clickable { edit() } else Modifier
Icon(Icons.Filled.Edit, contentDescription = null, modifier = modifier) // not analysed
```

### Values that are not literals

Constants and variables are not evaluated. A `contentDescription` held in a variable may be null at run time, but it is treated as unknown and never reported.

```kotlin
val label: String? = if (loaded) "Delete" else null
Icon(Icons.Filled.Delete, contentDescription = label) // unknown, not reported
```

### Composable lambdas outside a composable function

UI scope is decided by the nearest enclosing function. UI written directly inside `setContent { }` in an Activity is treated as outside UI scope.

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    setContent { Text("Hello") } // not UI scope for the rules
}
```

### Multipreview annotations

`UiScope.isPreview` only recognises `@Preview` written directly on the function. A custom multipreview annotation (an annotation class that is itself annotated with `@Preview`) is not recognised.

### Semi-transparent colors

`Contrast` ignores the alpha channel, so it cannot judge text drawn in a transparent color on top of another color.

### Positional `onClick` on Material containers

On the compiled Material3 library, Lint resolves `Card(onClick = ...)` to the wrong overload. The rules then read the `onClick` argument from the source by its name. A positional `onClick` has no name, so the Card is not recognised as clickable.

```kotlin
Card(onAction) { Icon(icon, contentDescription = null) } // not recognised as clickable
Card(onClick = onAction) { Icon(icon, contentDescription = null) } // recognised
```

## P-01 ComposeMissingContentDescription

### Description held in a variable

A variable may be null at run time, but its value is unknown statically, so it is not reported.

```kotlin
val label: String? = item.title
IconButton(onClick = onOpen) {
    Icon(Icons.Filled.Info, contentDescription = label) // not reported
}
```

### Icon or clickable parent in another composable

The search for the clickable parent stops at the enclosing function.

```kotlin
@Composable
fun DeleteIcon() = Icon(Icons.Filled.Delete, contentDescription = null) // not reported

@Composable
fun DeleteButton(onDelete: () -> Unit) = IconButton(onClick = onDelete) { DeleteIcon() }
```

### Name provided by another composable

When the clickable's Text is inside a separate composable, the rule cannot see it and reports the unlabelled icon. This is a possible false positive.

```kotlin
Row(Modifier.clickable { open() }) {
    Icon(Icons.Filled.Info, contentDescription = null) // reported
    ItemTitle(item) // contains a Text, not visible to the rule
}
```

### Custom icon wrappers

Only Material `Icon` and foundation `Image` are checked. A project's own wrapper, such as `AppIcon(resId, description)`, is not recognised.

## O-01 ComposeSmallTouchTarget

### Size comes from the parent or the layout

The rule only reads the modifiers of the element. Constraints from the parent are unknown, so an element that the parent forces to a larger size is still reported.

```kotlin
Box(
    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
    propagateMinConstraints = true,
) {
    Box(Modifier.size(20.dp).clickable { open() }) // reported, but the parent makes it 48dp
}
```

### Sizes that are not dp literals

Constants, `dimensionResource(...)`, `DpSize` values and computed sizes are unknown, so the element is not reported.

```kotlin
val iconSize = 20.dp
Box(Modifier.size(iconSize).clickable { open() }) // not reported
```

### `minimumInteractiveComponentSize()` in an order where it does not help

Any position in the chain suppresses the warning.

```kotlin
Modifier.size(20.dp).clickable { open() }.minimumInteractiveComponentSize() // not reported
```

## O-03 ComposeNestedClickable

### Nested clickable in another composable

The search stops at the enclosing function.

```kotlin
@Composable
fun FavoriteButton(onClick: () -> Unit) = IconButton(onClick = onClick) { ... }

Card(onClick = onOpen) {
    FavoriteButton(onFavorite) // not reported
}
```

### Clickable defined by a modifier parameter

A container that becomes clickable through a modifier passed in by the caller is not known to be clickable.

```kotlin
@Composable
fun Tile(modifier: Modifier) = Box(modifier) { Button(onClick = {}) { Text("Go") } }

Tile(Modifier.clickable { open() }) // not reported
```

## U-01 ComposeMissingHeading

### Text styles from variables or custom themes

Only `MaterialTheme.typography.<style>` written in the call is recognised.

```kotlin
val titleStyle = MaterialTheme.typography.headlineSmall
Text("Settings", style = titleStyle) // not reported
Text("Settings", style = AppTheme.typography.screenTitle) // not reported
```

### Heading-styled text that is not a heading

Big numbers and promotional text often use display or headline styles. They are reported as possible headings, and only a judgement about intent (Phase 3) can rule them out.

```kotlin
Text("42 steps", style = MaterialTheme.typography.displayLarge) // reported, but not a heading
```

### Heading set on a parent

`heading()` is only looked for on the Text's own modifier.

```kotlin
Row(Modifier.semantics(mergeDescendants = true) { heading() }) {
    Text("Inbox", style = MaterialTheme.typography.titleLarge) // reported
}
```

## U-02 ComposeMissingStateDescription

### Flips that are not written in the click lambda

```kotlin
Row(Modifier.clickable { viewModel.toggleFavorite() }) { ... } // not reported
Row(Modifier.clickable { toggle() }) { ... } // not reported
Row(Modifier.clickable { expanded = expanded.not() }) { ... } // not reported
```

### Material buttons that toggle

The rule only checks modifier-based clicks.

```kotlin
IconButton(onClick = { favorite = !favorite }) { ... } // not reported; IconToggleButton is the fix
```

## U-03 ComposeMissingSemanticError

### The message is a sibling, not the supporting text

Only the field's own `supportingText` slot and its own semantics are read. A message shown next to the field is not linked to it for accessibility anyway, so the field is still reported.

```kotlin
Column {
    TextField(value = email, onValueChange = onChange, label = { Text("Email") }, isError = true) // reported
    Text("Enter a valid email address") // not linked to the field
}
```

## U-04 ComposeVagueButtonLabel

### A labelled icon is not read as part of the label

The icon's `contentDescription` becomes part of the name a screen reader announces, but the rule only judges the Text, so this button is still reported.

```kotlin
Button(onClick = onDelete) {
    Icon(Icons.Filled.Delete, contentDescription = "Delete draft")
    Text("OK") // reported, although TalkBack says "Delete draft OK"
}
```

### Labels that are not literals

A label from `stringResource(...)` or a variable is unknown, so a vague label kept in `strings.xml` is never reported.

## U-05 ComposeTextFieldWithoutLabel

### Text fields that are not Material components

`BasicTextField` and a project's own field wrapper are not checked.

```kotlin
BasicTextField(value = query, onValueChange = onQueryChange) // not reported
AppTextField(value = query, onValueChange = onQueryChange) // not reported
```

### Text above the field

A `Text` shown above the field looks like a label but is not linked to it, so the field is still reported. The rule cannot tell whether the developer meant the Text as the label.

## P-03 ComposeLowContrastColors

### Background set two levels up, or through a parameter

Only the Text's own modifier and its single direct parent call are checked.

```kotlin
Surface(color = Color(0xFFFFFFFF)) {
    Box { // no background of its own
        Text("Delete draft", color = Color(0xFFB0B0B0)) // not reported; Surface is one level too far
    }
}

@Composable
fun Tile(modifier: Modifier) = Box(modifier) { Text("Delete draft", color = Color(0xFFB0B0B0)) }
Tile(Modifier.background(Color(0xFFFFFFFF))) // not reported
```

### Color read at run time

A color computed from a theme, a resource, or a variable is unknown, even if it resolves to a low-contrast literal in practice.

```kotlin
val warningColor = if (isDark) Color(0xFFB0B0B0) else Color(0xFF909090)
Text("Delete draft", color = warningColor) // not reported
```

## P-04 ComposeTextSizeInDp

### Conversion through another function or receiver

Only a `with(density) { ... }` block or a bare `.toSp()` call in the `fontSize` expression itself is recognised.

```kotlin
fun pixelsToSp(dp: Dp, density: Density): TextUnit = density.run { dp.toSp() } // not recognised

@Composable
fun cardFontSize(): TextUnit = LocalDensity.current.run { 16.dp.toSp() }
Text("Caption", fontSize = cardFontSize()) // not recognised
```

## P-05 ComposeTextOverImage

### Scrims drawn without a background modifier

Only `Modifier.background(...)` is recognised as protection. A scrim painted with `drawBehind`, a gradient `Brush`, or a translucent overlay drawn in a `Canvas` is not seen, so the text is still reported.

```kotlin
Box {
    Image(photo, contentDescription = null)
    Box(Modifier.fillMaxSize().drawBehind { drawRect(scrimBrush) }) // not recognised
    Text("Summer sale") // still reported
}
```

### Whether it is actually unreadable

The rule cannot know what the picture looks like. A caption over a dark, uniform photo may be perfectly readable, and one over a bright photo may not, so this is a candidate for Phase 3 rather than a certain defect.

## P-07 ComposeMissingPaneTitle

### Overlays that are not Popup or Dialog

CLAUDE.md also mentions "a full-screen `Box` shown conditionally with `AnimatedVisibility`". That is not detected: a Box that is really an overlay cannot be told apart from an ordinary animated Box without guessing, and P-07 is a STATIC rule, so it only reports what it is sure of.

```kotlin
AnimatedVisibility(visible = filtersOpen) {
    Box(Modifier.fillMaxSize().background(Color.White)) { // not reported
        FilterOptions()
    }
}
```

### A title set by the caller

The pane title is looked for on the overlay and inside its content. An overlay whose content is a composable that sets the title internally is still reported, since the rule does not follow calls.

## P-06 ComposeMissingLiveRegion

### State read or assigned in another function

Only a direct `Text(text = s)`/`Text(text = s.value)` in the same function as the declaration is recognised, and only assignments written in that same function are searched.

```kotlin
@Composable
fun StatusText(viewModel: StatusViewModel) {
    Text(text = viewModel.status) // not recognised: the state and its assignment are in the ViewModel
}
```

### A state holder other than `mutableStateOf`

`mutableStateListOf`, `mutableIntStateOf` and similar specialised holders are not recognised, only a declaration whose initializer or delegate calls `mutableStateOf` itself.

## O-02 ComposeMissingOnClickLabel

### Noise acknowledged, not yet tuned

CLAUDE.md flags this rule as possibly noisy on real apps. It is implemented exactly as specified and will be measured, not pre-tuned, on the development app run.

## O-05 ComposeEmptyClickable

### The name comes from another composable

A clickable is never reported when the rule cannot read what it renders, because that composable may well provide the name. This holds both when the unreadable composable is the content and when it is the clickable element itself. A deliberate false negative: reporting these flagged correct code on both development apps.

```kotlin
Box(Modifier.clickable { open() }) {
    ItemRow(item) // may contain a Text; not reported either way
}

// The element itself, as in JetNews PostCardTop and Jetchat JetchatIcon
PostCardTop(post = post, modifier = Modifier.clickable { navigateToPost(post.id) }) // not reported
```

So a genuinely empty custom composable with a click modifier is missed. The rule only judges what it can read: the layouts, the Material components, `Spacer` and `Canvas`.

## R-02 ComposeClearAndSetSemanticsLoss

### An empty block over content the rule cannot read is never reported

Since the development app run the rule only reports what the content demonstrably provided. When the content is a composable it cannot see into, nothing is known to be lost, so a block that really does hide a name stays silent.

```kotlin
BookmarkButton(onClick = onToggle, modifier = Modifier.clearAndSetSemantics { }) // not reported
```

This is the deliberate trade: the same shape is the documented way to hide a decorative subtree, and reporting it was a false positive on JetNews.

### Content in another composable

The content is read only where it is written. A row whose children come from another composable is not known to have a name or to be interactive, so a block that drops them is not reported.

## R-06 ComposeComposableWithoutSemantics

### The gesture and the semantics are in different functions

The check is per function, so a composable that only draws the gesture and leaves the semantics to its caller is reported.

```kotlin
@Composable
fun Swatch(modifier: Modifier) = // reported
    Box(modifier.pointerInput(Unit) { detectTapGestures { pick() } })

Swatch(Modifier.semantics { contentDescription = "Pick red" }) // the caller supplies the name
```

### Gesture APIs that are not public

`swipeable` (Material 2 only, fully deprecated) and `anchoredDraggable` (internal in Material3 1.4.0) are not matched, so a component built on them is not reported. See `docs/DECISIONS.md`.

## R-01 ComposeClickableWithoutRole

### Role set elsewhere

Only the click modifier and the semantics of the same chain are read.

```kotlin
Box(Modifier.semantics { role = Role.Button }) {
    Row(Modifier.clickable { open() }) { Text("Open"); Text("now") } // reported
}
```

### Button-like content inside a nested layout

The O-04 exclusion reads only the direct children, so this is reported by R-01 although it is button-like:

```kotlin
Box(Modifier.clickable { share() }) {
    Row { Icon(shareIcon, null); Text("Share") }
}
```

## O-06 ComposeDisabledButClickable

### The guard may be in a function the rule cannot read

Only the handler written at the call site is examined. A guard moved into a helper, or into a
lambda the handler calls, is not seen:

```kotlin
fun submitIfReady(ready: Boolean, onSubmit: () -> Unit) { if (!ready) return; onSubmit() }

Row(Modifier.clickable { submitIfReady(formComplete, onSubmit) }) { Text("Send") } // not reported
```

### What the condition tests is not read

Any guard counts, not only one on an enabled state, so a guard on something else is reported
with the same message:

```kotlin
Row(Modifier.clickable { if (items.isEmpty()) return@clickable; open() }) { Text("Open") }
```

The shape of the defect is the same, but the wording of the warning assumes an enabled state.
Phase 3 can read the condition and say what it really means.

### Guards written without a condition

`require`, `check`, an early `?: return` or a `takeIf` are all ways to stop the handler that are
not an `if` or a `when`, and none of them is matched:

```kotlin
Row(Modifier.clickable { formComplete.takeIf { it } ?: return@clickable; onSubmit() })
```

### Only `Modifier.clickable`

`combinedClickable`, `toggleable` and `selectable` have the same `enabled` parameter and the same
defect, but the rule follows CLAUDE.md 7.4 and matches `clickable` only.

## O-07 ComposeMissingCustomActions

### Children emitted by another composable are not counted

The content lambda is read, so buttons that a helper composable emits are invisible and the
container is not reported:

```kotlin
@Composable fun RowActions(onFavorite: () -> Unit, onShare: () -> Unit) {
    Row { IconButton(onClick = onFavorite) { ... }; IconButton(onClick = onShare) { ... } }
}

Card(onClick = onOpen) {
    Text("Shipping forecast")
    RowActions(onFavorite, onShare) // two clickable children the rule cannot see
}
```

This is the shared limitation recorded for O-05 and O-03, in its counting form: here it causes a
missed defect rather than a false positive.

### What is inside `customActions` is not checked

Any `customActions` assignment silences the rule, even an empty list or one action next to three
buttons:

```kotlin
Card(onClick = onOpen, modifier = Modifier.semantics { customActions = emptyList() }) { ... }
```

Matching the actions against the children means reading the labels and guessing which button each
one stands for, which is a Layer 3 judgement.

### `Box` and `Column` are not containers for this rule

A clickable `Box` or `Column` with two buttons has the same defect and is not reported, because
CLAUDE.md 7.4 names `Card`, `Surface` and `Row`. Note that O-02 does treat `Box` and `Column` as
containers, so the two rules use different sets.

### Actions provided somewhere other than the container's own modifier

Only the container's own modifier chain is read. `customActions` set on a wrapper around the
container, or in a modifier passed in from the caller, is not seen.

## U-07 ComposeHardcodedA11yText

### Text held in a constant or a variable

Only literals written at the place the text is used are read, so moving the same hardcoded string
one line up hides it:

```kotlin
private const val DELETE = "Delete draft"

Icon(icon, contentDescription = DELETE) // not reported
```

Following constants would mean deciding which values are "really" literal, which the project
deliberately does not do anywhere (see `Literals`).

### Concatenated and templated strings

A string built from parts is not a single literal, so it is not reported even when every part is
hardcoded:

```kotlin
Icon(icon, contentDescription = "Delete " + item.name) // not reported
Icon(icon, contentDescription = "Delete ${'$'}{item.name}") // not reported
```

### `onLongClickLabel` is not read

`combinedClickable(onLongClickLabel = "Open menu")` is the same defect, but CLAUDE.md 7.4 names
`onClickLabel`.

### A resource string can still be the wrong language

The rule checks only that the text comes from a resource, not that a translation exists. A
`stringResource` with no entry in any other locale is just as untranslated at run time, which no
source-level check can see.
