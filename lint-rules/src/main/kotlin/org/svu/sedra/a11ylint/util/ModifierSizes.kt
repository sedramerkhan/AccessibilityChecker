package org.svu.sedra.a11ylint.util

import com.android.tools.lint.detector.api.JavaContext
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression

/**
 * Reads literal sizes and paddings from a modifier chain, in dp.
 *
 * Only values written as literals (`20.dp`) are known. Anything else makes the axis unknown,
 * and an unknown axis is never used to report a problem.
 */
object ModifierSizes {
    private const val LAYOUT = "androidx.compose.foundation.layout"

    /** A layout direction. */
    enum class Axis { WIDTH, HEIGHT }

    /** How one modifier call changes one axis. */
    sealed interface Effect {
        /** Sets the axis to a literal size. */
        data class Fixed(val dp: Float) : Effect

        /** Sets or limits the axis in a way that is not a literal (for example `fillMaxWidth()`). */
        data object Unknown : Effect

        /** Adds padding on both sides of the axis. `dp` is the total, or null when not a literal. */
        data class Padding(val dp: Float?) : Effect
    }

    /** A measured size and the modifier call that decided it. */
    data class Measured(val dp: Float, val call: UCallExpression)

    private val widthSetters = setOf("width", "requiredWidth")
    private val heightSetters = setOf("height", "requiredHeight")
    private val bothSetters = setOf("size", "requiredSize")
    private val widthUnknown = setOf("fillMaxWidth", "widthIn", "requiredWidthIn", "wrapContentWidth")
    private val heightUnknown = setOf("fillMaxHeight", "heightIn", "requiredHeightIn", "wrapContentHeight")
    private val bothUnknown = setOf(
        "fillMaxSize", "sizeIn", "requiredSizeIn", "defaultMinSize", "wrapContentSize", "aspectRatio",
    )
    private val paddings = setOf("padding", "absolutePadding")

    /**
     * Returns how [call] changes [axis], or null when it does not change it. Layout modifiers
     * from `androidx.compose.foundation.layout` are read; `weight` (a member of `RowScope` and
     * `ColumnScope`) makes both axes unknown.
     */
    fun effect(context: JavaContext, call: UCallExpression, axis: Axis): Effect? {
        val name = ComposeCalls.name(call) ?: return null
        if (name == "weight") return Effect.Unknown
        if (!ComposeCalls.isCall(call, "$LAYOUT.$name")) return null
        val setsAxis = if (axis == Axis.WIDTH) widthSetters else heightSetters
        val unknownAxis = if (axis == Axis.WIDTH) widthUnknown else heightUnknown
        val axisName = if (axis == Axis.WIDTH) "width" else "height"
        val names = kotlinNames(call, name)
        return when (name) {
            in setsAxis -> fixed(ComposeCalls.argument(context, call, axisName, names))
            in bothSetters -> fixed(
                ComposeCalls.argument(context, call, "size", names)
                    ?: ComposeCalls.argument(context, call, axisName, names),
            )
            in unknownAxis, in bothUnknown -> Effect.Unknown
            in paddings -> Effect.Padding(padding(context, call, axis, names))
            else -> null
        }
    }

    /**
     * Kotlin parameter names of the resolved size or padding overload, chosen by its number of
     * parameters. They are needed because the compiled library loses the names of `Dp`
     * parameters (see [ComposeCalls.argument]).
     */
    private fun kotlinNames(call: UCallExpression, name: String): List<String>? {
        val method = call.resolve() ?: return null
        val count = ComposeCalls.valueParameters(method).size
        return when (name) {
            "size", "requiredSize" -> if (count == 2) listOf("width", "height") else listOf("size")
            "width", "requiredWidth" -> listOf("width")
            "height", "requiredHeight" -> listOf("height")
            "absolutePadding" -> listOf("left", "top", "right", "bottom")
            "padding" -> when (count) {
                1 -> listOf("all")
                2 -> listOf("horizontal", "vertical")
                4 -> listOf("start", "top", "end", "bottom")
                else -> null
            }
            else -> null
        }
    }

    /**
     * Returns the size of the element at [index] in the modifier chain [calls] along [axis].
     *
     * Compose applies modifiers from the outside in. A size set before the element (closer to
     * `Modifier`) is the element's size, reduced by any padding between that size and the
     * element. Without one, a size set after the element is used, increased by any padding
     * between the element and that size. Returns null when the size is unknown.
     */
    fun measure(context: JavaContext, calls: List<UCallExpression>, index: Int, axis: Axis): Measured? {
        var padding = 0f
        for (i in index - 1 downTo 0) {
            when (val effect = effect(context, calls[i], axis)) {
                is Effect.Fixed -> return Measured(effect.dp - padding, calls[i])
                Effect.Unknown -> return null
                is Effect.Padding -> padding += effect.dp ?: return null
                null -> Unit
            }
        }
        padding = 0f
        for (i in index + 1 until calls.size) {
            when (val effect = effect(context, calls[i], axis)) {
                is Effect.Fixed -> return Measured(effect.dp + padding, calls[i])
                Effect.Unknown -> return null
                is Effect.Padding -> padding += effect.dp ?: return null
                null -> Unit
            }
        }
        return null
    }

    private fun fixed(argument: UExpression?): Effect =
        Literals.dpValue(argument)?.let { Effect.Fixed(it) } ?: Effect.Unknown

    /**
     * Total padding on [axis] for any `padding`/`absolutePadding` overload. Parameters that are
     * not passed count as 0. Returns null when a passed value is not a literal, when the
     * `PaddingValues` overload is used, or when the arguments cannot be matched to parameters.
     */
    private fun padding(context: JavaContext, call: UCallExpression, axis: Axis, names: List<String>?): Float? {
        if (ComposeCalls.argument(context, call, "paddingValues", names) != null) return null
        val weighted = if (axis == Axis.WIDTH) {
            listOf("all" to 2f, "horizontal" to 2f, "start" to 1f, "end" to 1f, "left" to 1f, "right" to 1f)
        } else {
            listOf("all" to 2f, "vertical" to 2f, "top" to 1f, "bottom" to 1f)
        }
        var total = 0f
        var matched = 0
        for ((name, factor) in weighted) {
            val argument = ComposeCalls.argument(context, call, name, names) ?: continue
            matched++
            total += factor * (Literals.dpValue(argument) ?: return null)
        }
        val otherAxis = if (axis == Axis.WIDTH) {
            listOf("vertical", "top", "bottom")
        } else {
            listOf("horizontal", "start", "end", "left", "right")
        }
        matched += otherAxis.count { ComposeCalls.argument(context, call, it, names) != null }
        // Every argument must belong to a known parameter, otherwise the padding is unknown.
        if (matched < call.valueArgumentCount) return null
        return total
    }
}
