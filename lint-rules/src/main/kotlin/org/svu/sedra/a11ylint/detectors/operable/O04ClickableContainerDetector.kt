package org.svu.sedra.a11ylint.detectors.operable

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.svu.sedra.a11ylint.taxonomy.A11yIssues
import org.svu.sedra.a11ylint.taxonomy.Priority
import org.svu.sedra.a11ylint.util.Clickables
import org.svu.sedra.a11ylint.util.ComposeCalls
import org.svu.sedra.a11ylint.util.Literals
import org.svu.sedra.a11ylint.util.ModifierChain

/**
 * O-04 (candidate): a clickable `Box`, `Row` or `Column` with no role whose content is
 * button-like, so it should be a real `Button`.
 *
 * This is the other side of R-01 (CLAUDE.md overlap policy 7.1: O-04 over R-01). Both rules
 * look at the same node, a click modifier with no role on a non-Material element, and split it
 * by content: button-like content (one Text, or one Icon or Image and one Text) is O-04, and
 * anything else is R-01, so exactly one of the two reports.
 */
class O04ClickableContainerDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf("clickable", "combinedClickable")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!ComposeCalls.isCall(node, CLICK_MODIFIERS)) return
        val role = ComposeCalls.argument(context, node, "role")
        if (role != null && !Literals.isNullLiteral(role)) return

        val chain = ModifierChain.outermostExpression(node)
        if ("role" in ModifierChain.semanticsAssignments(chain)) return
        val container = ModifierChain.receivingElement(chain) as? UCallExpression ?: return
        if (!Clickables.isButtonLikeContainer(context, container)) return

        val name = ComposeCalls.name(container) ?: "container"
        context.report(
            ISSUE,
            node,
            context.getCallLocation(node, includeReceiver = false, includeArguments = false),
            A11yIssues.message(
                TAXONOMY_ID,
                "Possible button: clickable $name holds only a label, so screen readers " +
                    "announce a plain container instead of a button",
            ),
        )
    }

    companion object {
        const val TAXONOMY_ID = "O-04"

        private val CLICK_MODIFIERS = listOf(
            "androidx.compose.foundation.clickable",
            "androidx.compose.foundation.combinedClickable",
        )

        @JvmField
        val ISSUE: Issue = A11yIssues.create(
            id = "ComposeClickableContainer",
            briefDescription = "Clickable container that should be a button",
            explanation = """
                A `Box`, `Row` or `Column` that is made clickable and holds only a label is \
                really a button. Without a role, TalkBack announces it as a plain container, so \
                users cannot tell it can be activated, and it also misses the minimum touch \
                target, the focus ring and the disabled state that a real button provides. This \
                relates to WCAG 2.2 success criterion 4.1.2 (Name, Role, Value). Use a Material \
                `Button` or `TextButton`, or pass `role = Role.Button` to the click modifier.
            """,
            priority = Priority.MAJOR,
            detector = O04ClickableContainerDetector::class.java,
        )
    }
}
