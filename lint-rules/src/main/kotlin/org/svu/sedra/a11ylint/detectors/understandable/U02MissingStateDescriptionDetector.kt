package org.svu.sedra.a11ylint.detectors.understandable

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UPrefixExpression
import org.jetbrains.uast.UastBinaryOperator
import org.jetbrains.uast.UastPrefixOperator
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.svu.sedra.a11ylint.taxonomy.A11yIssues
import org.svu.sedra.a11ylint.taxonomy.Priority
import org.svu.sedra.a11ylint.util.Clickables
import org.svu.sedra.a11ylint.util.ComposeCalls
import org.svu.sedra.a11ylint.util.Literals
import org.svu.sedra.a11ylint.util.ModifierChain

/**
 * U-02: a `Modifier.clickable { }` whose click handler flips a Boolean, on an element that does
 * not expose that state to accessibility services.
 *
 * A flip is `x = !x`, `x.value = !x.value`, or a call such as `onCheckedChange(!checked)` (a
 * function named `on...Change` or `on...Changed` called with a negated value). The element
 * exposes its state when its modifier chain has `toggleable`, `triStateToggleable` or
 * `selectable`, or sets `stateDescription`, `toggleableState` or `selected` in semantics.
 */
class U02MissingStateDescriptionDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf("clickable", "combinedClickable")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!ComposeCalls.isCall(node, CLICK_MODIFIERS)) return
        val onClick = Literals.unwrap(ComposeCalls.argument(context, node, "onClick")) as? ULambdaExpression ?: return
        val state = flippedBoolean(onClick) ?: return

        val chain = ModifierChain.outermostExpression(node)
        if (exposesState(chain)) return
        val receiver = ModifierChain.receivingElement(chain) as? UCallExpression
        if (receiver != null && Clickables.isMaterialClickable(context, receiver)) return

        context.report(
            ISSUE,
            node,
            context.getCallLocation(node, includeReceiver = false, includeArguments = false),
            A11yIssues.message(
                TAXONOMY_ID,
                "Clickable element toggles $state but exposes no state, so screen readers do not " +
                    "announce whether it is on or off",
            ),
        )
    }

    /** Returns true when the chain has a toggle or selection modifier, or state semantics. */
    private fun exposesState(chain: UExpression): Boolean {
        if (ModifierChain.calls(chain).any { ComposeCalls.isCall(it, STATE_MODIFIERS) }) return true
        return ModifierChain.semanticsAssignments(chain).any { it in STATE_PROPERTIES }
    }

    /**
     * Returns the source text of the Boolean that the click handler flips (for example
     * `expanded` or `favorite.value`), or null when it flips nothing.
     */
    private fun flippedBoolean(onClick: ULambdaExpression): String? {
        var flipped: String? = null
        onClick.body.accept(object : AbstractUastVisitor() {
            override fun visitBinaryExpression(node: UBinaryExpression): Boolean {
                if (flipped == null && node.operator == UastBinaryOperator.ASSIGN) {
                    val target = text(node.leftOperand)
                    if (target != null && text(negatedOperand(node.rightOperand)) == target) flipped = target
                }
                return super.visitBinaryExpression(node)
            }

            override fun visitCallExpression(node: UCallExpression): Boolean {
                // Calling a function-type parameter can be named `invoke`; the identifier keeps the name.
                val name = node.methodIdentifier?.name ?: node.methodName ?: ""
                if (flipped == null && CHANGE_CALLBACK.matches(name)) {
                    flipped = node.valueArguments.firstNotNullOfOrNull { text(negatedOperand(it)) }
                }
                return super.visitCallExpression(node)
            }
        })
        return flipped
    }

    /** Returns `x` for `!x` (ignoring parentheses), or null when the expression is not a negation. */
    private fun negatedOperand(expression: UExpression?): UExpression? {
        val prefix = Literals.unwrap(expression) as? UPrefixExpression ?: return null
        return prefix.operand.takeIf { prefix.operator == UastPrefixOperator.LOGICAL_NOT }
    }

    /** Source text of an expression without parentheses and whitespace, used to compare targets. */
    private fun text(expression: UExpression?): String? =
        Literals.unwrap(expression)?.sourcePsi?.text?.filterNot { it.isWhitespace() }

    companion object {
        const val TAXONOMY_ID = "U-02"

        private val CLICK_MODIFIERS = listOf(
            "androidx.compose.foundation.clickable",
            "androidx.compose.foundation.combinedClickable",
        )
        private val STATE_MODIFIERS = listOf(
            "androidx.compose.foundation.selection.toggleable",
            "androidx.compose.foundation.selection.triStateToggleable",
            "androidx.compose.foundation.selection.selectable",
        )
        private val STATE_PROPERTIES = setOf("stateDescription", "toggleableState", "selected")

        /** A state change callback such as `onCheckedChange` or `onExpandedChanged`. */
        private val CHANGE_CALLBACK = Regex("""on\w*Changed?""")

        @JvmField
        val ISSUE: Issue = A11yIssues.create(
            id = "ComposeMissingStateDescription",
            briefDescription = "Clickable element toggles a state that screen readers cannot hear",
            explanation = """
                This element switches something on and off when clicked, but it only tells \
                accessibility services that it is clickable. TalkBack users hear "double tap to \
                activate" and never hear whether the option is now on or off. This relates to \
                WCAG 2.2 success criterion 4.1.2 (Name, Role, Value). Use \
                `Modifier.toggleable(value = checked, onValueChange = ...)` instead of \
                `clickable`, or set `stateDescription` in `Modifier.semantics { }`.
            """,
            priority = Priority.CRITICAL,
            detector = U02MissingStateDescriptionDetector::class.java,
        )
    }
}
