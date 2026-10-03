package org.svu.sedra.a11ylint.detectors.robust

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
 * R-01 (candidate): `Modifier.clickable` on a custom element without a role.
 *
 * The click modifier has no `role` argument (or `role = null`) and no `role` is set in the
 * semantics of the same chain. Chains passed to Material clickable components are ignored
 * because those components set their own role. A clickable `Box`, `Row` or `Column` with
 * button-like content is left to O-04 (CLAUDE.md overlap policy: O-04 over R-01).
 */
class R01ClickableWithoutRoleDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf("clickable", "combinedClickable")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!ComposeCalls.isCall(node, CLICK_MODIFIERS)) return
        val role = ComposeCalls.argument(context, node, "role")
        if (role != null && !Literals.isNullLiteral(role)) return

        val chain = ModifierChain.outermostExpression(node)
        if ("role" in ModifierChain.semanticsAssignments(chain)) return
        val receiver = ModifierChain.receivingElement(chain) as? UCallExpression
        if (receiver != null) {
            if (Clickables.isMaterialClickable(context, receiver)) return
            if (Clickables.isButtonLikeContainer(context, receiver)) return
        }

        val element = receiver?.let { ComposeCalls.name(it) }?.let { "clickable $it" } ?: "clickable element"
        context.report(
            ISSUE,
            node,
            context.getCallLocation(node, includeReceiver = false, includeArguments = false),
            A11yIssues.message(
                TAXONOMY_ID,
                "Possible missing role: $element has no role, so screen readers do not say what " +
                    "kind of control it is",
            ),
        )
    }

    companion object {
        const val TAXONOMY_ID = "R-01"

        private val CLICK_MODIFIERS = listOf(
            "androidx.compose.foundation.clickable",
            "androidx.compose.foundation.combinedClickable",
        )

        @JvmField
        val ISSUE: Issue = A11yIssues.create(
            id = "ComposeClickableWithoutRole",
            briefDescription = "Clickable custom element without a role",
            explanation = """
                A custom clickable element should tell accessibility services what kind of \
                control it is. Without a role, TalkBack only says "double tap to activate", and \
                users cannot tell whether the element is a button, a tab, a checkbox or an \
                image. This relates to WCAG 2.2 success criterion 4.1.2 (Name, Role, Value). \
                Pass the role to the click modifier, for example \
                `Modifier.clickable(role = Role.Button) { ... }`, or set `role` in \
                `Modifier.semantics { }`.
            """,
            priority = Priority.CRITICAL,
            detector = R01ClickableWithoutRoleDetector::class.java,
        )
    }
}
