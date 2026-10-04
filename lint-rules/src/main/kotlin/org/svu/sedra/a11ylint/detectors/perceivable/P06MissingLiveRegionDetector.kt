package org.svu.sedra.a11ylint.detectors.perceivable

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.ULocalVariable
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.UastBinaryOperator
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.svu.sedra.a11ylint.taxonomy.A11yIssues
import org.svu.sedra.a11ylint.taxonomy.Priority
import org.svu.sedra.a11ylint.util.Clickables
import org.svu.sedra.a11ylint.util.ComposeCalls
import org.svu.sedra.a11ylint.util.Literals
import org.svu.sedra.a11ylint.util.ModifierChain

/**
 * P-06: a `Text` whose text comes from a `mutableStateOf` local state that is set outside a
 * direct click handler (candidate), with no `liveRegion` to announce the change.
 *
 * Only a read written directly as `Text(text = s)` or `Text(text = s.value)` is recognised,
 * where `s` is a local declared in the same function with `mutableStateOf` somewhere in its
 * initializer or delegate. Every assignment to that local in the same function is found, and
 * classified by its nearest enclosing lambda: a lambda passed as `onClick`, `onCheckedChange` or
 * `onValueChange` is a direct click handler; anything else (including no enclosing lambda at
 * all) counts as indirect. This is a STATIC_LLM rule: every candidate is reported, because
 * whether the change is really asynchronous needs the context Phase 3 will add.
 */
class P06MissingLiveRegionDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf("Text")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!ComposeCalls.isCall(node, Clickables.textCalls)) return
        if (ModifierChain.semanticsAssignments(ModifierChain.modifierArgument(context, node)).contains("liveRegion")) {
            return
        }
        val state = stateRead(ComposeCalls.argument(context, node, "text")) ?: return
        val function = enclosingMethod(node) ?: return
        val assignments = assignmentsTo(state, function)
        if (assignments.isEmpty() || assignments.all { isDirectClickHandler(context, it) }) return

        context.report(
            ISSUE,
            node,
            context.getNameLocation(node),
            A11yIssues.message(
                TAXONOMY_ID,
                "Possible live region: this text may change without a direct click, but screen " +
                    "readers are not told to announce the change",
            ),
        )
    }

    /** A local `mutableStateOf` read: `s` or `s.value`. [pattern] decides which shape to match later. */
    private data class StateRead(val variable: ULocalVariable, val pattern: Pattern)

    private enum class Pattern { PLAIN, VALUE_PROPERTY }

    private fun stateRead(expression: UExpression?): StateRead? {
        return when (val value = Literals.unwrap(expression)) {
            is USimpleNameReferenceExpression -> mutableStateVariable(value)?.let { StateRead(it, Pattern.PLAIN) }
            is UQualifiedReferenceExpression -> {
                val selector = Literals.unwrap(value.selector) as? USimpleNameReferenceExpression ?: return null
                if (selector.identifier != "value") return null
                val receiver = Literals.unwrap(value.receiver) as? USimpleNameReferenceExpression ?: return null
                mutableStateVariable(receiver)?.let { StateRead(it, Pattern.VALUE_PROPERTY) }
            }
            else -> null
        }
    }

    /**
     * Returns the resolved local variable when its declaration calls `mutableStateOf` somewhere
     * in its initializer or its delegate, for example `var s by remember { mutableStateOf("") }`
     * or `val s = remember { mutableStateOf(0) }`. A delegated property's `uastInitializer` is
     * null; its delegate expression is read from the Kotlin PSI instead.
     */
    private fun mutableStateVariable(reference: USimpleNameReferenceExpression): ULocalVariable? {
        val variable = reference.resolve().toUElement() as? ULocalVariable ?: return null
        val initializer = variable.uastInitializer
            ?: (variable.sourcePsi as? KtProperty)?.delegate?.expression?.toUElement()
            ?: return null
        return variable.takeIf { callsMutableStateOf(initializer) }
    }

    /** Returns true when [root] contains a call resolving to `androidx.compose.runtime.mutableStateOf`. */
    private fun callsMutableStateOf(root: UElement): Boolean {
        var found = false
        root.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                if (!found && ComposeCalls.isCall(node, "androidx.compose.runtime.mutableStateOf")) found = true
                return super.visitCallExpression(node)
            }
        })
        return found
    }

    private fun enclosingMethod(node: UElement): UMethod? {
        var element: UElement? = node.uastParent
        while (element != null && element !is UMethod) element = element.uastParent
        return element as? UMethod
    }

    /** Every assignment to [state] in [function], in source order. */
    private fun assignmentsTo(state: StateRead, function: UMethod): List<UBinaryExpression> {
        val found = mutableListOf<UBinaryExpression>()
        function.uastBody?.accept(object : AbstractUastVisitor() {
            override fun visitBinaryExpression(node: UBinaryExpression): Boolean {
                if (node.operator == UastBinaryOperator.ASSIGN && targets(node.leftOperand, state)) found += node
                return super.visitBinaryExpression(node)
            }
        })
        return found
    }

    private fun targets(left: UExpression, state: StateRead): Boolean {
        val reference = when (state.pattern) {
            Pattern.PLAIN -> Literals.unwrap(left) as? USimpleNameReferenceExpression
            Pattern.VALUE_PROPERTY -> {
                val qualified = Literals.unwrap(left) as? UQualifiedReferenceExpression ?: return false
                val selector = Literals.unwrap(qualified.selector) as? USimpleNameReferenceExpression
                if (selector?.identifier != "value") return false
                Literals.unwrap(qualified.receiver) as? USimpleNameReferenceExpression
            }
        } ?: return false
        return (reference.resolve().toUElement() as? ULocalVariable)?.sourcePsi == state.variable.sourcePsi
    }

    /** True when [assignment]'s nearest enclosing lambda is a direct click or value-change handler. */
    private fun isDirectClickHandler(context: JavaContext, assignment: UBinaryExpression): Boolean {
        var element: UElement? = assignment.uastParent
        while (element != null && element !is UMethod) {
            if (element is ULambdaExpression) return isClickHandlerLambda(context, element)
            element = element.uastParent
        }
        return false
    }

    private fun isClickHandlerLambda(context: JavaContext, lambda: ULambdaExpression): Boolean {
        val call = lambda.uastParent as? UCallExpression ?: return false
        return CLICK_PARAMETER_NAMES.any { name ->
            Literals.unwrap(ComposeCalls.argument(context, call, name))?.sourcePsi == lambda.sourcePsi
        }
    }

    companion object {
        const val TAXONOMY_ID = "P-06"

        private val CLICK_PARAMETER_NAMES = setOf("onClick", "onCheckedChange", "onValueChange")

        @JvmField
        val ISSUE: Issue = A11yIssues.create(
            id = "ComposeMissingLiveRegion",
            briefDescription = "Text may change without announcing it to screen readers",
            explanation = """
                Text whose value changes on its own, for example after a network response or a \
                timer, needs a live region so screen readers announce the new value. Otherwise \
                users only hear it if they happen to move focus there again. This relates to \
                WCAG 2.2 success criterion 4.1.3 (Status Messages). Add `liveRegion = \
                LiveRegionMode.Polite` (or `Assertive` for an urgent message) in \
                `Modifier.semantics { }`, once the real cause of the change is confirmed.
            """,
            priority = Priority.MAJOR,
            detector = P06MissingLiveRegionDetector::class.java,
        )
    }
}
