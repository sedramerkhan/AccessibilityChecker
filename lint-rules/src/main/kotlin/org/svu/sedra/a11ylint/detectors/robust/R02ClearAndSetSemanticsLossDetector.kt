package org.svu.sedra.a11ylint.detectors.robust

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.svu.sedra.a11ylint.taxonomy.A11yIssues
import org.svu.sedra.a11ylint.taxonomy.Priority
import org.svu.sedra.a11ylint.util.Clickables
import org.svu.sedra.a11ylint.util.ComposeCalls
import org.svu.sedra.a11ylint.util.Literals
import org.svu.sedra.a11ylint.util.ModifierChain

/**
 * R-02: `Modifier.clearAndSetSemantics { }` that throws away the semantics of its content
 * without putting them back.
 *
 * The modifier replaces the whole subtree's semantics with what its block sets, so anything the
 * block does not set is gone. The rule reports an empty block, a block that does not set a name
 * although the content has one, and a block that does not set `role` or `stateDescription`
 * although the content is interactive.
 */
class R02ClearAndSetSemanticsLossDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf("clearAndSetSemantics")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!ComposeCalls.isCall(node, CLEAR_AND_SET_SEMANTICS)) return
        val assigned = ModifierChain.blockAssignments(node)
        val element = ModifierChain.receivingElement(ModifierChain.outermostExpression(node)) as? UCallExpression

        val problem = when {
            assigned.isEmpty() ->
                "is empty, so everything it covers is hidden from screen readers"
            element == null -> return
            else -> {
                val lost = buildList {
                    if (contentHasName(context, element) && assigned.none { it in NAME_PROPERTIES }) {
                        add("the name of its content")
                    }
                    if (isInteractive(context, element) && assigned.none { it in ROLE_PROPERTIES }) {
                        add("the role and state of its content")
                    }
                }
                if (lost.isEmpty()) return
                "does not put back ${lost.joinToString(" or ")}, so screen readers lose it"
            }
        }

        context.report(
            ISSUE,
            node,
            context.getCallLocation(node, includeReceiver = false, includeArguments = false),
            A11yIssues.message(TAXONOMY_ID, "clearAndSetSemantics $problem"),
        )
    }

    /** True when the content of [element] holds a Text with text, or a labelled Icon or Image. */
    private fun contentHasName(context: JavaContext, element: UCallExpression): Boolean =
        contentCalls(element).any { call ->
            when {
                ComposeCalls.isCall(call, Clickables.textCalls) ->
                    !Literals.isEmptyStringLiteral(ComposeCalls.argument(context, call, "text"))
                ComposeCalls.isCall(call, Clickables.imageCalls) -> Clickables.isLabeledImage(context, call)
                else -> false
            }
        }

    /** True when [element] or anything in its content responds to clicks or carries state. */
    private fun isInteractive(context: JavaContext, element: UCallExpression): Boolean =
        Clickables.isClickableElement(context, element) ||
            contentCalls(element).any {
                Clickables.isClickableElement(context, it) || ComposeCalls.isCall(it, STATEFUL_COMPONENTS)
            }

    /** Every call inside the lambdas passed to [call], at any depth. */
    private fun contentCalls(call: UCallExpression): List<UCallExpression> {
        val calls = mutableListOf<UCallExpression>()
        val visitor = object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                calls += node
                return super.visitCallExpression(node)
            }
        }
        call.valueArguments
            .mapNotNull { Literals.unwrap(it) as? ULambdaExpression }
            .forEach { it.body.accept(visitor) }
        return calls
    }

    companion object {
        const val TAXONOMY_ID = "R-02"

        private const val CLEAR_AND_SET_SEMANTICS = "androidx.compose.ui.semantics.clearAndSetSemantics"

        /** Semantics that can stand in for the name the content used to provide. */
        private val NAME_PROPERTIES = setOf("contentDescription", "text")

        /** Semantics that can stand in for the role or state the content used to provide. */
        private val ROLE_PROPERTIES = setOf("role", "stateDescription", "toggleableState", "selected")

        /** Material components that carry an on or off state of their own. */
        private val STATEFUL_COMPONENTS = listOf("androidx.compose.material3", "androidx.compose.material")
            .flatMap { pkg ->
                listOf("Checkbox", "TriStateCheckbox", "Switch", "RadioButton").map { "$pkg.$it" }
            }

        @JvmField
        val ISSUE: Issue = A11yIssues.create(
            id = "ComposeClearAndSetSemanticsLoss",
            briefDescription = "clearAndSetSemantics removes semantics without replacing them",
            explanation = """
                `clearAndSetSemantics` replaces the semantics of everything inside it with what \
                its block sets, so whatever the block leaves out is gone for screen readers. An \
                empty block hides the content completely, and a block that sets no name, role or \
                state turns a labelled control into an element TalkBack cannot describe. This \
                relates to WCAG 2.2 success criterion 4.1.2 (Name, Role, Value). Set the name, \
                role and state the content used to provide, or use `Modifier.semantics \
                (mergeDescendants = true) { }` to merge them instead of clearing them.
            """,
            priority = Priority.MAJOR,
            detector = R02ClearAndSetSemanticsLossDetector::class.java,
        )
    }
}
