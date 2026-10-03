package org.svu.sedra.a11ylint.stubs

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin

/*
 * Minimal Compose API declarations shared by detector tests.
 *
 * Package names, function names and parameter order were checked against the sources of
 * Compose 1.10.4 (runtime, ui, foundation) and Material3 1.4.0 from the Gradle cache
 * (Compose BOM 2026.02.01). Parameters that no rule reads are sometimes left out, but only
 * after the last parameter a rule uses, so positional argument mapping stays correct.
 * Bodies are empty or TODO(). Value classes are kept where the real API uses them.
 */

private val runtimeStubs = kotlin(
    "src/androidx/compose/runtime/Runtime.kt",
    """
    package androidx.compose.runtime

    @Retention(AnnotationRetention.BINARY)
    @Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.TYPE,
        AnnotationTarget.TYPE_PARAMETER,
        AnnotationTarget.PROPERTY_GETTER,
    )
    annotation class Composable

    interface State<out T> { val value: T }
    interface MutableState<T> : State<T> { override var value: T }
    inline operator fun <T> State<T>.getValue(thisObj: Any?, property: kotlin.reflect.KProperty<*>): T = value
    inline operator fun <T> MutableState<T>.setValue(thisObj: Any?, property: kotlin.reflect.KProperty<*>, value: T) {
        this.value = value
    }

    @Composable inline fun <T> remember(crossinline calculation: () -> T): T = calculation()
    @Composable inline fun <T> remember(key1: Any?, crossinline calculation: () -> T): T = calculation()
    @Composable inline fun <T> remember(vararg keys: Any?, crossinline calculation: () -> T): T = calculation()
    fun <T> mutableStateOf(value: T): MutableState<T> = TODO()
    fun <T> derivedStateOf(calculation: () -> T): State<T> = TODO()

    @Composable fun LaunchedEffect(key1: Any?, block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {}
    @Composable fun LaunchedEffect(vararg keys: Any?, block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {}
    class DisposableEffectScope
    interface DisposableEffectResult
    @Composable fun DisposableEffect(key1: Any?, effect: DisposableEffectScope.() -> DisposableEffectResult) {}
    @Composable fun SideEffect(effect: () -> Unit) {}
    interface ProduceStateScope<T> : MutableState<T>
    @Composable fun <T> produceState(initialValue: T, producer: suspend ProduceStateScope<T>.() -> Unit): State<T> = TODO()
    @Composable fun rememberCoroutineScope(): kotlinx.coroutines.CoroutineScope = TODO()
    """,
).indented()

private val saveableStubs = kotlin(
    "src/androidx/compose/runtime/saveable/Saveable.kt",
    """
    package androidx.compose.runtime.saveable

    import androidx.compose.runtime.Composable

    @Composable fun <T : Any> rememberSaveable(vararg inputs: Any?, init: () -> T): T = init()
    """,
).indented()

private val coroutineStubs = kotlin(
    "src/kotlinx/coroutines/Coroutine.kt",
    """
    package kotlinx.coroutines

    interface CoroutineScope
    interface Job
    interface Deferred<out T> : Job

    fun CoroutineScope.launch(block: suspend CoroutineScope.() -> Unit): Job = TODO()
    fun <T> CoroutineScope.async(block: suspend CoroutineScope.() -> T): Deferred<T> = TODO()
    suspend fun delay(timeMillis: Long) {}
    """,
).indented()

private val uiStubs = kotlin(
    "src/androidx/compose/ui/Ui.kt",
    """
    package androidx.compose.ui

    interface Modifier {
        infix fun then(other: Modifier): Modifier = other
        companion object : Modifier
    }

    interface Alignment {
        companion object {
            val TopStart: Alignment = TODO()
            val Center: Alignment = TODO()
        }
    }
    """,
).indented()

private val semanticsStubs = kotlin(
    "src/androidx/compose/ui/semantics/Semantics.kt",
    """
    package androidx.compose.ui.semantics

    import androidx.compose.ui.Modifier

    interface SemanticsPropertyReceiver

    fun Modifier.semantics(
        mergeDescendants: Boolean = false,
        properties: (SemanticsPropertyReceiver.() -> Unit),
    ): Modifier = this
    fun Modifier.clearAndSetSemantics(properties: (SemanticsPropertyReceiver.() -> Unit)): Modifier = this

    @JvmInline
    value class Role private constructor(private val value: Int) {
        companion object {
            val Button = Role(0)
            val Checkbox = Role(1)
            val Switch = Role(2)
            val RadioButton = Role(3)
            val Tab = Role(4)
            val Image = Role(5)
        }
    }

    @JvmInline
    value class LiveRegionMode private constructor(private val value: Int) {
        companion object {
            val Polite = LiveRegionMode(0)
            val Assertive = LiveRegionMode(1)
        }
    }

    class CustomAccessibilityAction(val label: String, val action: () -> Boolean)
    class CollectionInfo(val rowCount: Int, val columnCount: Int)
    class CollectionItemInfo(val rowIndex: Int, val rowSpan: Int, val columnIndex: Int, val columnSpan: Int)
    class ProgressBarRangeInfo(val current: Float, val range: ClosedFloatingPointRange<Float>, val steps: Int = 0)

    var SemanticsPropertyReceiver.contentDescription: String
        get() = TODO()
        set(value) {}
    var SemanticsPropertyReceiver.stateDescription: String
        get() = TODO()
        set(value) {}
    var SemanticsPropertyReceiver.role: Role
        get() = TODO()
        set(value) {}
    var SemanticsPropertyReceiver.liveRegion: LiveRegionMode
        get() = TODO()
        set(value) {}
    var SemanticsPropertyReceiver.paneTitle: String
        get() = TODO()
        set(value) {}
    var SemanticsPropertyReceiver.customActions: List<CustomAccessibilityAction>
        get() = TODO()
        set(value) {}
    var SemanticsPropertyReceiver.collectionInfo: CollectionInfo
        get() = TODO()
        set(value) {}
    var SemanticsPropertyReceiver.collectionItemInfo: CollectionItemInfo
        get() = TODO()
        set(value) {}
    var SemanticsPropertyReceiver.progressBarRangeInfo: ProgressBarRangeInfo
        get() = TODO()
        set(value) {}
    var SemanticsPropertyReceiver.selected: Boolean
        get() = TODO()
        set(value) {}
    var SemanticsPropertyReceiver.toggleableState: androidx.compose.ui.state.ToggleableState
        get() = TODO()
        set(value) {}

    fun SemanticsPropertyReceiver.heading() {}
    fun SemanticsPropertyReceiver.error(description: String) {}
    """,
).indented()

private val toggleableStateStubs = kotlin(
    "src/androidx/compose/ui/state/ToggleableState.kt",
    """
    package androidx.compose.ui.state

    enum class ToggleableState { On, Off, Indeterminate }
    """,
).indented()

private val pointerStubs = kotlin(
    "src/androidx/compose/ui/input/pointer/Pointer.kt",
    """
    package androidx.compose.ui.input.pointer

    import androidx.compose.ui.Modifier

    interface PointerInputScope

    fun Modifier.pointerInput(key1: Any?, block: suspend PointerInputScope.() -> Unit): Modifier = this
    fun Modifier.pointerInput(vararg keys: Any?, block: suspend PointerInputScope.() -> Unit): Modifier = this
    """,
).indented()

private val drawStubs = kotlin(
    "src/androidx/compose/ui/draw/Draw.kt",
    """
    package androidx.compose.ui.draw

    import androidx.compose.ui.Modifier
    import androidx.compose.ui.graphics.drawscope.DrawScope

    fun Modifier.drawBehind(onDraw: DrawScope.() -> Unit): Modifier = this
    """,
).indented()

private val graphicsStubs = kotlin(
    "src/androidx/compose/ui/graphics/Graphics.kt",
    """
    package androidx.compose.ui.graphics

    @JvmInline
    value class Color(val value: ULong) {
        companion object {
            val Unspecified: Color = TODO()
            val Black: Color = TODO()
            val White: Color = TODO()
        }
    }

    fun Color(color: Long): Color = TODO()
    fun Color(color: Int): Color = TODO()

    interface Shape
    object RectangleShape : Shape
    """,
).indented()

private val drawScopeStubs = kotlin(
    "src/androidx/compose/ui/graphics/drawscope/DrawScope.kt",
    """
    package androidx.compose.ui.graphics.drawscope

    interface DrawScope
    """,
).indented()

private val vectorStubs = kotlin(
    "src/androidx/compose/ui/graphics/vector/Vector.kt",
    """
    package androidx.compose.ui.graphics.vector

    class ImageVector
    """,
).indented()

private val painterStubs = kotlin(
    "src/androidx/compose/ui/graphics/painter/Painter.kt",
    """
    package androidx.compose.ui.graphics.painter

    abstract class Painter
    """,
).indented()

private val unitStubs = kotlin(
    "src/androidx/compose/ui/unit/Unit.kt",
    """
    package androidx.compose.ui.unit

    @JvmInline
    value class Dp(val value: Float) {
        companion object {
            val Unspecified = Dp(Float.NaN)
        }
    }
    val Int.dp: Dp get() = Dp(this.toFloat())
    val Double.dp: Dp get() = Dp(this.toFloat())
    val Float.dp: Dp get() = Dp(this)

    @JvmInline
    value class TextUnit internal constructor(internal val packedValue: Long) {
        companion object {
            val Unspecified: TextUnit = TODO()
        }
    }
    val Float.sp: TextUnit get() = TODO()
    val Double.sp: TextUnit get() = TODO()
    val Int.sp: TextUnit get() = TODO()
    """,
).indented()

private val textStubs = kotlin(
    "src/androidx/compose/ui/text/Text.kt",
    """
    package androidx.compose.ui.text

    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.unit.TextUnit

    class TextLayoutResult

    class TextStyle(
        val color: Color = Color.Unspecified,
        val fontSize: TextUnit = TextUnit.Unspecified,
        val fontWeight: FontWeight? = null,
    ) {
        fun copy(
            color: Color = this.color,
            fontSize: TextUnit = this.fontSize,
            fontWeight: FontWeight? = this.fontWeight,
        ): TextStyle = TODO()
    }
    """,
).indented()

private val fontStubs = kotlin(
    "src/androidx/compose/ui/text/font/Font.kt",
    """
    package androidx.compose.ui.text.font

    class FontStyle
    class FontFamily

    class FontWeight(val weight: Int) {
        companion object {
            val Normal = FontWeight(400)
            val Medium = FontWeight(500)
            val SemiBold = FontWeight(600)
            val Bold = FontWeight(700)
            val ExtraBold = FontWeight(800)
            val Black = FontWeight(900)
            val W700 = FontWeight(700)
            val W800 = FontWeight(800)
            val W900 = FontWeight(900)
        }
    }
    """,
).indented()

private val textStyleStubs = kotlin(
    "src/androidx/compose/ui/text/style/TextStyles.kt",
    """
    package androidx.compose.ui.text.style

    class TextDecoration
    class TextAlign
    class TextOverflow {
        companion object {
            val Clip = TextOverflow()
            val Ellipsis = TextOverflow()
        }
    }
    """,
).indented()

private val resourceStubs = kotlin(
    "src/androidx/compose/ui/res/Resource.kt",
    """
    package androidx.compose.ui.res

    import androidx.compose.runtime.Composable

    @Composable fun stringResource(id: Int): String = TODO()
    @Composable fun stringResource(id: Int, vararg formatArgs: Any): String = TODO()
    @Composable fun pluralStringResource(id: Int, count: Int): String = TODO()
    @Composable fun pluralStringResource(id: Int, count: Int, vararg formatArgs: Any): String = TODO()
    @Composable fun painterResource(id: Int): androidx.compose.ui.graphics.painter.Painter = TODO()
    """,
).indented()

private val previewStubs = kotlin(
    "src/androidx/compose/ui/tooling/preview/Preview.kt",
    """
    package androidx.compose.ui.tooling.preview

    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
    annotation class Preview(val name: String = "", val showBackground: Boolean = false)
    """,
).indented()

private val foundationStubs = kotlin(
    "src/androidx/compose/foundation/Foundation.kt",
    """
    package androidx.compose.foundation

    import androidx.compose.runtime.Composable
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.graphics.RectangleShape
    import androidx.compose.ui.graphics.Shape
    import androidx.compose.ui.graphics.drawscope.DrawScope
    import androidx.compose.ui.graphics.painter.Painter
    import androidx.compose.ui.graphics.vector.ImageVector
    import androidx.compose.ui.semantics.Role

    fun Modifier.clickable(
        enabled: Boolean = true,
        onClickLabel: String? = null,
        role: Role? = null,
        onClick: () -> Unit,
    ): Modifier = this

    fun Modifier.combinedClickable(
        enabled: Boolean = true,
        onClickLabel: String? = null,
        role: Role? = null,
        onLongClickLabel: String? = null,
        onLongClick: (() -> Unit)? = null,
        onDoubleClick: (() -> Unit)? = null,
        onClick: () -> Unit,
    ): Modifier = this

    fun Modifier.background(color: Color, shape: Shape = RectangleShape): Modifier = this

    class ScrollState
    @Composable fun rememberScrollState(initial: Int = 0): ScrollState = TODO()
    fun Modifier.verticalScroll(state: ScrollState, enabled: Boolean = true): Modifier = this
    fun Modifier.horizontalScroll(state: ScrollState, enabled: Boolean = true): Modifier = this

    @Composable
    fun Image(
        imageVector: ImageVector,
        contentDescription: String?,
        modifier: Modifier = Modifier,
        alignment: Alignment = Alignment.Center,
    ) {}

    @Composable
    fun Image(
        painter: Painter,
        contentDescription: String?,
        modifier: Modifier = Modifier,
        alignment: Alignment = Alignment.Center,
    ) {}

    @Composable fun Canvas(modifier: Modifier, onDraw: DrawScope.() -> Unit) {}
    """,
).indented()

private val selectionStubs = kotlin(
    "src/androidx/compose/foundation/selection/Selection.kt",
    """
    package androidx.compose.foundation.selection

    import androidx.compose.ui.Modifier
    import androidx.compose.ui.semantics.Role

    fun Modifier.toggleable(
        value: Boolean,
        enabled: Boolean = true,
        role: Role? = null,
        onValueChange: (Boolean) -> Unit,
    ): Modifier = this

    fun Modifier.selectable(
        selected: Boolean,
        enabled: Boolean = true,
        role: Role? = null,
        onClick: () -> Unit,
    ): Modifier = this
    """,
).indented()

private val gestureStubs = kotlin(
    "src/androidx/compose/foundation/gestures/Gesture.kt",
    """
    package androidx.compose.foundation.gestures

    import androidx.compose.ui.input.pointer.PointerInputScope

    suspend fun PointerInputScope.detectTapGestures(
        onDoubleTap: ((Any) -> Unit)? = null,
        onLongPress: ((Any) -> Unit)? = null,
        onTap: ((Any) -> Unit)? = null,
    ) {}
    """,
).indented()

private val layoutStubs = kotlin(
    "src/androidx/compose/foundation/layout/Layout.kt",
    """
    package androidx.compose.foundation.layout

    import androidx.compose.runtime.Composable
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.unit.Dp
    import androidx.compose.ui.unit.dp

    interface BoxScope
    interface RowScope
    interface ColumnScope

    @Composable
    inline fun Box(
        modifier: Modifier = Modifier,
        contentAlignment: Alignment = Alignment.TopStart,
        propagateMinConstraints: Boolean = false,
        content: @Composable BoxScope.() -> Unit,
    ) {}
    @Composable fun Box(modifier: Modifier) {}

    @Composable
    inline fun Row(
        modifier: Modifier = Modifier,
        content: @Composable RowScope.() -> Unit,
    ) {}

    @Composable
    inline fun Column(
        modifier: Modifier = Modifier,
        content: @Composable ColumnScope.() -> Unit,
    ) {}

    @Composable fun Spacer(modifier: Modifier) {}

    fun Modifier.size(size: Dp): Modifier = this
    fun Modifier.size(width: Dp, height: Dp): Modifier = this
    fun Modifier.width(width: Dp): Modifier = this
    fun Modifier.height(height: Dp): Modifier = this
    fun Modifier.requiredSize(size: Dp): Modifier = this
    fun Modifier.requiredSize(width: Dp, height: Dp): Modifier = this
    fun Modifier.requiredWidth(width: Dp): Modifier = this
    fun Modifier.requiredHeight(height: Dp): Modifier = this
    fun Modifier.widthIn(min: Dp = Dp.Unspecified, max: Dp = Dp.Unspecified): Modifier = this
    fun Modifier.heightIn(min: Dp = Dp.Unspecified, max: Dp = Dp.Unspecified): Modifier = this
    fun Modifier.defaultMinSize(minWidth: Dp = Dp.Unspecified, minHeight: Dp = Dp.Unspecified): Modifier = this
    fun Modifier.padding(all: Dp): Modifier = this
    // The real overload has defaults (0.dp). They are left out because Lint's JVM_OVERLOADS test
    // mode would turn them into a padding(Dp) overload that clashes with padding(all).
    fun Modifier.padding(horizontal: Dp, vertical: Dp): Modifier = this
    // Same reason: the real defaults (0.dp) would create a padding(Dp) overload under JVM_OVERLOADS.
    fun Modifier.padding(start: Dp, top: Dp, end: Dp, bottom: Dp): Modifier = this
    fun Modifier.absolutePadding(left: Dp = 0.dp, top: Dp = 0.dp, right: Dp = 0.dp, bottom: Dp = 0.dp): Modifier = this
    fun Modifier.fillMaxWidth(fraction: Float = 1f): Modifier = this
    fun Modifier.fillMaxSize(fraction: Float = 1f): Modifier = this
    fun Modifier.fillMaxHeight(fraction: Float = 1f): Modifier = this
    """,
).indented()

private val lazyStubs = kotlin(
    "src/androidx/compose/foundation/lazy/Lazy.kt",
    """
    package androidx.compose.foundation.lazy

    import androidx.compose.runtime.Composable
    import androidx.compose.ui.Modifier

    interface LazyItemScope
    interface LazyListScope {
        fun item(key: Any? = null, content: @Composable LazyItemScope.() -> Unit)
    }

    @Composable fun LazyColumn(modifier: Modifier = Modifier, content: LazyListScope.() -> Unit) {}
    @Composable fun LazyRow(modifier: Modifier = Modifier, content: LazyListScope.() -> Unit) {}
    """,
).indented()

private val material3Stubs = kotlin(
    "src/androidx/compose/material3/Material3.kt",
    """
    package androidx.compose.material3

    import androidx.compose.foundation.layout.ColumnScope
    import androidx.compose.foundation.layout.RowScope
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.graphics.painter.Painter
    import androidx.compose.ui.graphics.vector.ImageVector
    import androidx.compose.ui.text.TextLayoutResult
    import androidx.compose.ui.text.TextStyle
    import androidx.compose.ui.text.font.FontFamily
    import androidx.compose.ui.text.font.FontStyle
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.text.style.TextDecoration
    import androidx.compose.ui.text.style.TextOverflow
    import androidx.compose.ui.unit.TextUnit

    @Composable
    fun Icon(
        imageVector: ImageVector,
        contentDescription: String?,
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
    ) {}

    @Composable
    fun Icon(
        painter: Painter,
        contentDescription: String?,
        modifier: Modifier = Modifier,
        tint: Color = Color.Unspecified,
    ) {}

    @Composable
    fun Text(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = Color.Unspecified,
        fontSize: TextUnit = TextUnit.Unspecified,
        fontStyle: FontStyle? = null,
        fontWeight: FontWeight? = null,
        fontFamily: FontFamily? = null,
        letterSpacing: TextUnit = TextUnit.Unspecified,
        textDecoration: TextDecoration? = null,
        textAlign: TextAlign? = null,
        lineHeight: TextUnit = TextUnit.Unspecified,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = true,
        maxLines: Int = Int.MAX_VALUE,
        minLines: Int = 1,
        onTextLayout: ((TextLayoutResult) -> Unit)? = null,
        style: TextStyle = TextStyle(),
    ) {}

    @Composable
    fun Button(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        content: @Composable RowScope.() -> Unit,
    ) {}

    @Composable
    fun TextButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        content: @Composable RowScope.() -> Unit,
    ) {}

    @Composable
    fun OutlinedButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        content: @Composable RowScope.() -> Unit,
    ) {}

    @Composable
    fun IconButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        content: @Composable () -> Unit,
    ) {}

    @Composable
    fun FloatingActionButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit,
    ) {}

    @Composable
    fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {}

    @Composable
    fun Card(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        content: @Composable ColumnScope.() -> Unit,
    ) {}

    @Composable
    fun Surface(
        modifier: Modifier = Modifier,
        color: Color = Color.Unspecified,
        content: @Composable () -> Unit,
    ) {}

    @Composable
    fun Surface(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        color: Color = Color.Unspecified,
        content: @Composable () -> Unit,
    ) {}

    @Composable
    fun TextField(
        value: String,
        onValueChange: (String) -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        readOnly: Boolean = false,
        textStyle: TextStyle = TextStyle(),
        label: @Composable (() -> Unit)? = null,
        placeholder: @Composable (() -> Unit)? = null,
        leadingIcon: @Composable (() -> Unit)? = null,
        trailingIcon: @Composable (() -> Unit)? = null,
        prefix: @Composable (() -> Unit)? = null,
        suffix: @Composable (() -> Unit)? = null,
        supportingText: @Composable (() -> Unit)? = null,
        isError: Boolean = false,
    ) {}

    @Composable
    fun OutlinedTextField(
        value: String,
        onValueChange: (String) -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        readOnly: Boolean = false,
        textStyle: TextStyle = TextStyle(),
        label: @Composable (() -> Unit)? = null,
        placeholder: @Composable (() -> Unit)? = null,
        leadingIcon: @Composable (() -> Unit)? = null,
        trailingIcon: @Composable (() -> Unit)? = null,
        prefix: @Composable (() -> Unit)? = null,
        suffix: @Composable (() -> Unit)? = null,
        supportingText: @Composable (() -> Unit)? = null,
        isError: Boolean = false,
    ) {}

    @Composable
    fun Switch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier) {}

    @Composable
    fun Checkbox(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier) {}

    @Composable
    fun RadioButton(selected: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {}

    @Composable
    fun ListItem(
        headlineContent: @Composable () -> Unit,
        modifier: Modifier = Modifier,
        overlineContent: @Composable (() -> Unit)? = null,
        supportingContent: @Composable (() -> Unit)? = null,
        leadingContent: @Composable (() -> Unit)? = null,
        trailingContent: @Composable (() -> Unit)? = null,
    ) {}

    @Composable
    fun TopAppBar(
        title: @Composable () -> Unit,
        modifier: Modifier = Modifier,
        navigationIcon: @Composable () -> Unit = {},
        actions: @Composable RowScope.() -> Unit = {},
    ) {}

    @Composable
    fun CenterAlignedTopAppBar(
        title: @Composable () -> Unit,
        modifier: Modifier = Modifier,
        navigationIcon: @Composable () -> Unit = {},
        actions: @Composable RowScope.() -> Unit = {},
    ) {}

    @Composable
    fun LargeTopAppBar(
        title: @Composable () -> Unit,
        modifier: Modifier = Modifier,
        navigationIcon: @Composable () -> Unit = {},
        actions: @Composable RowScope.() -> Unit = {},
    ) {}

    @Composable
    fun ModalBottomSheet(
        onDismissRequest: () -> Unit,
        modifier: Modifier = Modifier,
        content: @Composable ColumnScope.() -> Unit,
    ) {}

    fun Modifier.minimumInteractiveComponentSize(): Modifier = this

    class Typography(
        val displayLarge: TextStyle = TextStyle(),
        val displayMedium: TextStyle = TextStyle(),
        val displaySmall: TextStyle = TextStyle(),
        val headlineLarge: TextStyle = TextStyle(),
        val headlineMedium: TextStyle = TextStyle(),
        val headlineSmall: TextStyle = TextStyle(),
        val titleLarge: TextStyle = TextStyle(),
        val titleMedium: TextStyle = TextStyle(),
        val titleSmall: TextStyle = TextStyle(),
        val bodyLarge: TextStyle = TextStyle(),
        val bodyMedium: TextStyle = TextStyle(),
        val bodySmall: TextStyle = TextStyle(),
        val labelLarge: TextStyle = TextStyle(),
        val labelMedium: TextStyle = TextStyle(),
        val labelSmall: TextStyle = TextStyle(),
    )

    class ColorScheme(
        val primary: Color,
        val onPrimary: Color,
        val surface: Color,
        val onSurface: Color,
    )

    object MaterialTheme {
        val typography: Typography
            @Composable get() = TODO()
        val colorScheme: ColorScheme
            @Composable get() = TODO()
    }
    """,
).indented()

private val material2Stubs = kotlin(
    "src/androidx/compose/material/Material.kt",
    """
    package androidx.compose.material

    import androidx.compose.runtime.Composable
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.text.TextLayoutResult
    import androidx.compose.ui.text.TextStyle
    import androidx.compose.ui.text.font.FontFamily
    import androidx.compose.ui.text.font.FontStyle
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.text.style.TextDecoration
    import androidx.compose.ui.text.style.TextOverflow
    import androidx.compose.ui.unit.TextUnit

    @Composable
    fun Text(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = Color.Unspecified,
        fontSize: TextUnit = TextUnit.Unspecified,
        fontStyle: FontStyle? = null,
        fontWeight: FontWeight? = null,
        fontFamily: FontFamily? = null,
        letterSpacing: TextUnit = TextUnit.Unspecified,
        textDecoration: TextDecoration? = null,
        textAlign: TextAlign? = null,
        lineHeight: TextUnit = TextUnit.Unspecified,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = true,
        maxLines: Int = Int.MAX_VALUE,
        minLines: Int = 1,
        onTextLayout: ((TextLayoutResult) -> Unit)? = null,
        style: TextStyle = TextStyle(),
    ) {}

    class Typography(
        val h1: TextStyle = TextStyle(),
        val h2: TextStyle = TextStyle(),
        val h3: TextStyle = TextStyle(),
        val h4: TextStyle = TextStyle(),
        val h5: TextStyle = TextStyle(),
        val h6: TextStyle = TextStyle(),
        val body1: TextStyle = TextStyle(),
    )

    object MaterialTheme {
        val typography: Typography
            @Composable get() = TODO()
    }
    """,
).indented()

private val iconsStubs = kotlin(
    "src/androidx/compose/material/icons/Icons.kt",
    """
    package androidx.compose.material.icons

    object Icons {
        object Filled
        object Outlined
        val Default = Filled
    }
    """,
).indented()

private val filledIconsStubs = kotlin(
    "src/androidx/compose/material/icons/filled/FilledIcons.kt",
    """
    package androidx.compose.material.icons.filled

    import androidx.compose.material.icons.Icons
    import androidx.compose.ui.graphics.vector.ImageVector

    val Icons.Filled.Add: ImageVector get() = TODO()
    val Icons.Filled.Close: ImageVector get() = TODO()
    val Icons.Filled.Delete: ImageVector get() = TODO()
    val Icons.Filled.Favorite: ImageVector get() = TODO()
    val Icons.Filled.Share: ImageVector get() = TODO()
    """,
).indented()

/** All Compose stubs. Every detector test gets these through [org.svu.sedra.a11ylint.testing.A11yLintTest]. */
val composeStubs: Array<TestFile> = arrayOf(
    runtimeStubs,
    saveableStubs,
    coroutineStubs,
    uiStubs,
    semanticsStubs,
    toggleableStateStubs,
    pointerStubs,
    drawStubs,
    graphicsStubs,
    drawScopeStubs,
    vectorStubs,
    painterStubs,
    unitStubs,
    textStubs,
    fontStubs,
    textStyleStubs,
    resourceStubs,
    previewStubs,
    foundationStubs,
    selectionStubs,
    gestureStubs,
    layoutStubs,
    lazyStubs,
    material3Stubs,
    material2Stubs,
    iconsStubs,
    filledIconsStubs,
)
