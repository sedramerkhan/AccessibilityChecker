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
 * O-02: `Modifier.clickable` with no `onClickLabel`, on a container (`Card`, `Surface`, `Row`,
 * `Box`, `Column` or `ListItem`).
 *
 * Material buttons already announce their own role and are not in the container list, so they
 * are never reported. CLAUDE.md flags this rule as possibly noisy; it is not tuned ahead of the
 * development app run.
 */
class O02MissingOnClickLabelDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf("clickable")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!ComposeCalls.isCall(node, CLICKABLE)) return
        val onClickLabel = ComposeCalls.argument(context, node, "onClickLabel")
        if (onClickLabel != null && !Literals.isNullLiteral(onClickLabel)) return

        val chain = ModifierChain.outermostExpression(node)
        val container = ModifierChain.receivingElement(chain) as? UCallExpression ?: return
        if (!ComposeCalls.isCall(container, CONTAINERS)) return

        val name = ComposeCalls.name(container) ?: "container"
        context.report(
            ISSUE,
            node,
            context.getCallLocation(node, includeReceiver = false, includeArguments = false),
            A11yIssues.message(
                TAXONOMY_ID,
                "Clickable $name has no onClickLabel, so screen readers only announce " +
                    "\"double tap to activate\" with nothing describing what it does",
            ),
        )
    }

    companion object {
        const val TAXONOMY_ID = "O-02"

        private const val CLICKABLE = "androidx.compose.foundation.clickable"

        private val CONTAINERS = Clickables.layoutContainers + setOf(
            "androidx.compose.material3.Card",
            "androidx.compose.material3.Surface",
            "androidx.compose.material3.ListItem",
            "androidx.compose.material.Card",
            "androidx.compose.material.Surface",
            "androidx.compose.material.ListItem",
        )

        @JvmField
        val ISSUE: Issue = A11yIssues.create(
            id = "ComposeMissingOnClickLabel",
            briefDescription = "Clickable container has no onClickLabel",
            explanation = """
                A clickable container with no `onClickLabel` makes TalkBack announce only a \
                generic "double tap to activate" prompt, with nothing describing what tapping \
                it will do. Sighted users see the surrounding content and infer the action, but \
                screen reader users cannot. This relates to WCAG 2.2 success criterion 4.1.2 \
                (Name, Role, Value). Add `Modifier.clickable(onClickLabel = "...", onClick = \
                ...)` describing the action, for example "Open details".
            """,
            priority = Priority.MAJOR,
            detector = O02MissingOnClickLabelDetector::class.java,
        )
    }
}
