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
