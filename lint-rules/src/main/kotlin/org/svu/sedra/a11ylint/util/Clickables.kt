package org.svu.sedra.a11ylint.util

import com.android.tools.lint.detector.api.JavaContext
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * Knowledge about clickable Compose elements and the text that names them.
 * All names are fully qualified and were checked against Compose 1.10.4 and Material3 1.4.0.
 */
object Clickables {
    private const val FOUNDATION = "androidx.compose.foundation"
    private const val SELECTION = "androidx.compose.foundation.selection"
    private const val M3 = "androidx.compose.material3"
    private const val M2 = "androidx.compose.material"

    /** Modifiers that make an element respond to clicks or toggles. */
    val clickableModifiers = setOf(
        "$FOUNDATION.clickable",
        "$FOUNDATION.combinedClickable",
        "$SELECTION.toggleable",
        "$SELECTION.triStateToggleable",
        "$SELECTION.selectable",
    )

    /** Material components that are always clickable. */
    val clickableComponents = listOf(M3, M2).flatMap { pkg ->
        listOf(
            "Button", "TextButton", "OutlinedButton", "IconButton", "IconToggleButton",
            "FloatingActionButton", "ExtendedFloatingActionButton",
        ).map { "$pkg.$it" }
    }.toSet() + setOf(
        "$M3.ElevatedButton",
        "$M3.FilledTonalButton",
        "$M3.FilledIconButton",
        "$M3.FilledTonalIconButton",
        "$M3.OutlinedIconButton",
        "$M3.SmallFloatingActionButton",
        "$M3.LargeFloatingActionButton",
    )

    /** Material containers that are clickable only in their `onClick` (or `onCheckedChange`) overload. */
    val clickableContainers = setOf(
        "$M3.Card", "$M3.ElevatedCard", "$M3.OutlinedCard", "$M3.Surface",
        "$M2.Card", "$M2.Surface",
    )

    /**
     * Material selection controls, mapped to the Kotlin parameter names of their signature up
     * to and including the callback that decides whether the control is interactive.
     *
     * Each of these installs its click modifier only when that callback is non-null:
     * `Checkbox` adds `Modifier.triStateToggleable` (Material3 1.4.0 `Checkbox.kt:277`),
     * `RadioButton` adds `Modifier.selectable` (`RadioButton.kt:90`) and `Switch` adds
     * `Modifier.toggleable` (`Switch.kt:105`). Passing `null` is the documented way to let a
     * surrounding toggleable row own the interaction, so the callback is what separates a
     * second touch target from the correct pattern.
     *
     * The name list is the positional fallback for [ComposeCalls.argument]: the compiled
     * Material3 signatures can lose their parameter names the same way `Dp` and `Color`
     * parameters do.
     */
    private val selectionControlCallbacks: Map<String, List<String>> = buildMap {
        for (pkg in listOf(M3, M2)) {
            put("$pkg.Checkbox", listOf("checked", "onCheckedChange"))
            put("$pkg.Switch", listOf("checked", "onCheckedChange"))
            put("$pkg.RadioButton", listOf("selected", "onClick"))
            put("$pkg.TriStateCheckbox", listOf("state", "onClick"))
        }
    }

    /** Material selection controls: `Checkbox`, `TriStateCheckbox`, `Switch` and `RadioButton`. */
    val selectionControls: Set<String> = selectionControlCallbacks.keys

    /**
     * Returns true when [call] is a Material selection control that is a touch target in its
     * own right, that is, one whose callback is anything other than a `null` literal.
     *
     * This is deliberately **not** part of [isClickableElement]. Nine rules share that
     * predicate, and a selection control has no content lambda and no name of its own, so
     * widening it would make O-05 report every labelled `Row { Checkbox(...); Text(...) }` as
     * an empty clickable. Only O-03 asks this question.
     *
     * A callback that is a variable counts as interactive, matching [isLabeledImage], which
     * treats a variable `contentDescription` as a real value rather than guessing.
     */
    fun isInteractiveSelectionControl(context: JavaContext, call: UCallExpression): Boolean {
        val method = call.resolve() ?: return false
        val names = ComposeCalls.qualifiedNames(method)
            .firstNotNullOfOrNull { selectionControlCallbacks[it] } ?: return false
        val callback = ComposeCalls.argument(context, call, names.last(), names) ?: return false
        return !Literals.isNullLiteral(callback)
    }

    /** Composables that show text. */
    val textCalls = setOf("$M3.Text", "$M2.Text", "$FOUNDATION.text.BasicText")

    /** Composables that show an icon or image with a `contentDescription` parameter. */
    val imageCalls = setOf("$M3.Icon", "$M2.Icon", "$FOUNDATION.Image")

    /** Returns true when a modifier expression contains a click or toggle modifier. */
    fun hasClickableModifier(modifier: UExpression?): Boolean =
        ModifierChain.calls(modifier).any { ComposeCalls.isCall(it, clickableModifiers) }

    /**
     * Returns true when [call] emits a clickable element: a Material clickable component, a
     * Material container called with `onClick` or `onCheckedChange`, or any composable whose
     * `modifier` argument contains a click or toggle modifier.
     */
    fun isClickableElement(context: JavaContext, call: UCallExpression): Boolean =
        isMaterialClickable(context, call) ||
            hasClickableModifier(ModifierChain.modifierArgument(context, call))

    /**
     * Returns true when [call] is a Material clickable component, or a Material container
     * called with `onClick` or `onCheckedChange`. These components handle the click and set
     * the minimum touch target size themselves.
     */
    fun isMaterialClickable(context: JavaContext, call: UCallExpression): Boolean {
        if (ComposeCalls.isCall(call, clickableComponents)) return true
        if (!ComposeCalls.isCall(call, clickableContainers)) return false
        return ComposeCalls.argument(context, call, "onClick") != null ||
            ComposeCalls.argument(context, call, "onCheckedChange") != null
    }

    /** Returns true when a modifier expression sets `contentDescription` or `text` in semantics. */
    fun hasSemanticsLabel(modifier: UExpression?): Boolean {
        val assigned = ModifierChain.semanticsAssignments(modifier)
        return "contentDescription" in assigned || "text" in assigned
    }

    /**
     * Returns true when an Icon or Image call has a label: its `contentDescription` is anything
     * other than a `null` or `""` literal (a variable counts as a label), or its own modifier
     * sets `contentDescription` in semantics.
     */
    fun isLabeledImage(context: JavaContext, call: UCallExpression): Boolean {
        val description = ComposeCalls.argument(context, call, "contentDescription")
        val missing = description == null ||
            Literals.isNullLiteral(description) ||
            Literals.isEmptyStringLiteral(description)
        return !missing || hasSemanticsLabel(ModifierChain.modifierArgument(context, call))
    }

    /**
     * Returns true when the element emitted by [call] has an accessible name that does not come
     * from [ignored]: its modifier sets `contentDescription` in semantics, or one of its content
     * lambdas contains a Text with non-empty text, a labelled Icon or Image, or any element whose
     * modifier sets `contentDescription`. Content in other composable functions is not visible.
     */
    fun hasAccessibleName(context: JavaContext, call: UCallExpression, ignored: UCallExpression? = null): Boolean {
        if (hasSemanticsLabel(ModifierChain.modifierArgument(context, call))) return true
        var found = false
        val visitor = object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                if (found || node == ignored) return found
                if (isNamingCall(context, node)) found = true
                return found
            }
        }
        call.valueArguments
            .mapNotNull { Literals.unwrap(it) as? ULambdaExpression }
            .forEach { it.body.accept(visitor) }
        return found
    }

    /** Layout containers that CLAUDE.md treats as custom clickable elements in O-04 and R-01. */
    val layoutContainers = setOf("$FOUNDATION.layout.Box", "$FOUNDATION.layout.Row", "$FOUNDATION.layout.Column")

    private const val SPACER = "$FOUNDATION.layout.Spacer"

    /**
     * Returns true when [container] is a `Box`, `Row` or `Column` whose content is button-like:
     * exactly one Text, or one Icon or Image and one Text in any order. `Spacer` calls between
     * them are ignored. Only the direct children of the content lambda are read. This is the
     * O-04 case of the overlap policy (CLAUDE.md 7.1).
     */
    fun isButtonLikeContainer(context: JavaContext, container: UCallExpression): Boolean {
        if (!ComposeCalls.isCall(container, layoutContainers)) return false
        val lambda = ComposeCalls.contentLambda(context, container) ?: return false
        val statements = (lambda.body as? UBlockExpression)?.expressions ?: return false
        val children = statements.map { statement ->
            when (val value = Literals.unwrap((statement as? UReturnExpression)?.returnExpression ?: statement)) {
                is UCallExpression -> value
                // A fully qualified call (`androidx.compose.material3.Text(...)`).
                is UQualifiedReferenceExpression -> value.selector as? UCallExpression ?: return false
                else -> return false
            }
        }.filterNot { ComposeCalls.isCall(it, SPACER) }
        val texts = children.count { ComposeCalls.isCall(it, textCalls) }
        val images = children.count { ComposeCalls.isCall(it, imageCalls) }
        return texts == 1 && images <= 1 && texts + images == children.size
    }

    private fun isNamingCall(context: JavaContext, call: UCallExpression): Boolean = when {
        ComposeCalls.isCall(call, textCalls) -> !Literals.isEmptyStringLiteral(ComposeCalls.argument(context, call, "text"))
        ComposeCalls.isCall(call, imageCalls) -> isLabeledImage(context, call)
        else -> hasSemanticsLabel(ModifierChain.modifierArgument(context, call))
    }
}
