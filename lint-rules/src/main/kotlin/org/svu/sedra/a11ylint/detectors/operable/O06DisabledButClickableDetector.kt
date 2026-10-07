package org.svu.sedra.a11ylint.detectors.operable

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UIfExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.USwitchClauseExpression
import org.jetbrains.uast.USwitchExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.svu.sedra.a11ylint.taxonomy.A11yIssues
import org.svu.sedra.a11ylint.taxonomy.Priority
import org.svu.sedra.a11ylint.util.ComposeCalls
import org.svu.sedra.a11ylint.util.Literals

/**
 * O-06: a `Modifier.clickable { }` whose handler decides for itself whether it is enabled,
 * instead of telling `clickable` through its `enabled` parameter.
 *
 * Guarding inside the lambda stops the action but leaves the element enabled as far as
 * accessibility services are concerned, so a screen reader still offers it and nothing happens
 * when the user activates it. The two shapes CLAUDE.md names are recognised: an early return
 * (`if (!enabled) return@clickable`) and a body wrapped in a condition (`if (enabled) { ... }`).
 */
class O06DisabledButClickableDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf("clickable")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!ComposeCalls.isCall(node, CLICKABLE)) return
        // The element already tells accessibility services whether it is enabled.
        if (ComposeCalls.argument(context, node, "enabled") != null) return

        val onClick = Literals.unwrap(ComposeCalls.argument(context, node, "onClick")) as? ULambdaExpression
            ?: return
        if (!isGuarded(onClick)) return

        context.report(
            ISSUE,
            node,
            context.getCallLocation(node, includeReceiver = false, includeArguments = false),
            A11yIssues.message(
                TAXONOMY_ID,
                "The click handler checks whether it is enabled but enabled is not passed to " +
                    "clickable, so screen readers still announce the element as enabled",
            ),
        )
    }

    /** True when the whole handler is wrapped in a condition, or it starts with a guard return. */
    private fun isGuarded(onClick: ULambdaExpression): Boolean {
        val body = onClick.body as? UBlockExpression ?: return false
        val statements = body.expressions.map(::statement)
        if (statements.isEmpty()) return false
        // `if (enabled) { ... }` as the only thing the handler does. A condition with an else
        // branch is a choice between two actions, not a guard, so it does not count.
        val only = statements.singleOrNull()
        if (only != null && isConditional(only) && !hasElseBranch(only)) return true
        // `if (!enabled) return@clickable` before the real work.
        return statements.any { isConditional(it) && containsReturn(it) }
    }

    /**
     * The expression a statement really is: without parentheses, and without the implicit
     * `return` UAST puts around the last expression of a lambda.
     */
    private fun statement(expression: UExpression): UExpression {
        val value = Literals.unwrap(expression) ?: expression
        if (value !is UReturnExpression) return value
        return value.returnExpression?.let { Literals.unwrap(it) } ?: value
    }

    /**
     * `if` or `when`. Both shapes matter: Lint's `IF_TO_WHEN` test mode rewrites every `if` into
     * a `when`, and real code uses both.
     */
    private fun isConditional(expression: UExpression): Boolean =
        expression is UIfExpression || expression is USwitchExpression

    /** True when the condition also says what to do in the other case. */
    private fun hasElseBranch(expression: UExpression): Boolean = when (expression) {
        is UIfExpression -> expression.elseExpression != null
        is USwitchExpression -> expression.body.expressions.any {
            it is USwitchClauseExpression && it.caseValues.isEmpty()
        }
        else -> false
    }

    private fun containsReturn(expression: UExpression): Boolean {
        var found = false
        expression.accept(object : AbstractUastVisitor() {
            override fun visitReturnExpression(node: UReturnExpression): Boolean {
                found = true
                return true
            }
        })
        return found
    }

    companion object {
        const val TAXONOMY_ID = "O-06"

        private const val CLICKABLE = "androidx.compose.foundation.clickable"

        @JvmField
        val ISSUE: Issue = A11yIssues.create(
            id = "ComposeDisabledButClickable",
            briefDescription = "Click handler guards on enabled instead of the clickable modifier",
            explanation = """
                Checking whether the element is enabled inside the click handler stops the \
                action, but accessibility services are never told: the element keeps its \
                clickable state, so TalkBack still offers it and the user activates it to no \
                effect, with no explanation. This relates to WCAG 2.2 success criterion 4.1.2 \
                (Name, Role, Value). Pass the state instead, as \
                `Modifier.clickable(enabled = isEnabled) { ... }`, which both blocks the click \
                and marks the element disabled.
            """,
            priority = Priority.MINOR,
            detector = O06DisabledButClickableDetector::class.java,
        )
    }
}
