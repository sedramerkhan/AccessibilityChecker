package org.svu.sedra.a11ylint.stubs

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin

/** Minimal Compose API declarations shared by detector tests. */
val composeStubs: Array<TestFile> = arrayOf(
    kotlin(
        "ComposeStubs.kt",
        """
        package androidx.compose.runtime

        annotation class Composable
        annotation class Preview

        fun <T> remember(vararg keys: Any?, calculation: () -> T): T = TODO()
        fun <T> rememberSaveable(vararg inputs: Any?, calculation: () -> T): T = TODO()
        fun <T> mutableStateOf(value: T): MutableState<T> = TODO()
        interface MutableState<T> { var value: T }
        fun LaunchedEffect(vararg keys: Any?, block: suspend () -> Unit) = Unit
        fun DisposableEffect(vararg keys: Any?, effect: () -> DisposableEffectResult): Unit = Unit
        fun SideEffect(effect: () -> Unit) = Unit
        fun <T> produceState(initialValue: T, producer: suspend MutableState<T>.() -> Unit): MutableState<T> = TODO()
        """,
    ),
    kotlin(
        "ComposeUiStubs.kt",
        """
        package androidx.compose.ui

        interface Modifier {
            companion object : Modifier
        }

        fun Modifier.semantics(mergeDescendants: Boolean = false, properties: androidx.compose.ui.semantics.SemanticsPropertyReceiver.() -> Unit): Modifier = this
        fun Modifier.clearAndSetSemantics(properties: androidx.compose.ui.semantics.SemanticsPropertyReceiver.() -> Unit): Modifier = this
        fun Modifier.clickable(enabled: Boolean = true, onClickLabel: String? = null, role: androidx.compose.ui.semantics.Role? = null, onClick: () -> Unit): Modifier = this
        """,
    ),
    kotlin(
        "SemanticsStubs.kt",
        """
        package androidx.compose.ui.semantics

        interface SemanticsPropertyReceiver {
            var contentDescription: List<String>
            var stateDescription: String
            var role: Role
            var liveRegion: LiveRegionMode
            var paneTitle: String
            var customActions: List<CustomAccessibilityAction>
            var collectionInfo: CollectionInfo
            var progressBarRangeInfo: ProgressBarRangeInfo
        }
        fun SemanticsPropertyReceiver.heading() = Unit
        fun SemanticsPropertyReceiver.error(message: String) = Unit
        class Role
        class LiveRegionMode
        class CustomAccessibilityAction(val label: String, val action: () -> Boolean)
        class CollectionInfo
        class CollectionItemInfo
        class ProgressBarRangeInfo
        """,
    ),
    kotlin(
        "FoundationStubs.kt",
        """
        package androidx.compose.foundation

        import androidx.compose.runtime.Composable
        import androidx.compose.ui.Modifier

        @Composable fun Image(contentDescription: String? = null, modifier: Modifier = Modifier) = Unit
        @Composable fun Canvas(modifier: Modifier = Modifier, onDraw: () -> Unit) = Unit
        fun Modifier.toggleable(value: Boolean, enabled: Boolean = true, onValueChange: (Boolean) -> Unit): Modifier = this
        fun Modifier.selectable(selected: Boolean, enabled: Boolean = true, onClick: () -> Unit): Modifier = this
        fun Modifier.background(color: androidx.compose.ui.graphics.Color): Modifier = this
        fun Modifier.pointerInput(key: Any?, block: suspend () -> Unit): Modifier = this
        """,
    ),
    kotlin(
        "LayoutStubs.kt",
        """
        package androidx.compose.foundation.layout

        import androidx.compose.runtime.Composable
        import androidx.compose.ui.Modifier
        import androidx.compose.ui.unit.Dp

        @Composable fun Box(modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) = Unit
        @Composable fun Row(modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) = Unit
        @Composable fun Column(modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) = Unit
        fun Modifier.size(size: Dp): Modifier = this
        fun Modifier.width(size: Dp): Modifier = this
        fun Modifier.height(size: Dp): Modifier = this
        fun Modifier.requiredSize(size: Dp): Modifier = this
        fun Modifier.padding(size: Dp): Modifier = this
        """,
    ),
    kotlin(
        "MaterialStubs.kt",
        """
        package androidx.compose.material3

        import androidx.compose.runtime.Composable
        import androidx.compose.ui.Modifier

        @Composable fun Icon(contentDescription: String? = null, modifier: Modifier = Modifier) = Unit
        @Composable fun Text(text: String, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified) = Unit
        @Composable fun Button(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) = Unit
        @Composable fun TextButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) = Unit
        @Composable fun IconButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) = Unit
        @Composable fun Card(onClick: (() -> Unit)? = null, modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) = Unit
        @Composable fun Surface(onClick: (() -> Unit)? = null, modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) = Unit
        @Composable fun TextField(value: String, onValueChange: (String) -> Unit, label: (@Composable () -> Unit)? = null, placeholder: (@Composable () -> Unit)? = null, isError: Boolean = false) = Unit
        @Composable fun OutlinedTextField(value: String, onValueChange: (String) -> Unit, label: (@Composable () -> Unit)? = null, placeholder: (@Composable () -> Unit)? = null, isError: Boolean = false) = Unit
        @Composable fun Switch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?) = Unit
        @Composable fun Checkbox(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?) = Unit
        fun minimumInteractiveComponentSize(): Modifier = Modifier
        object MaterialTheme { val typography = Typography(); val colorScheme = ColorScheme() }
        class Typography
        class ColorScheme
        """,
    ),
    kotlin(
        "UnitStubs.kt",
        """
        package androidx.compose.ui.unit

        class Dp
        val Int.dp: Dp get() = Dp()
        val Float.dp: Dp get() = Dp()
        class TextUnit
        val Int.sp: TextUnit get() = TextUnit()
        val Float.sp: TextUnit get() = TextUnit()
        """,
    ),
    kotlin(
        "ColorStubs.kt",
        """
        package androidx.compose.ui.graphics

        class Color(val value: Long = 0L) {
            companion object { val Unspecified = Color() }
        }
        """,
    ),
    kotlin(
        "ResourceStubs.kt",
        """
        package androidx.compose.ui.res

        fun stringResource(id: Int, vararg formatArgs: Any): String = TODO()
        fun pluralStringResource(id: Int, count: Int, vararg formatArgs: Any): String = TODO()
        """,
    ),
)
