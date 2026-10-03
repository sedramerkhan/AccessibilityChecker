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
