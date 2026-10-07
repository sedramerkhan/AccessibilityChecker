package org.svu.sedra.a11ylint.detectors.perceivable

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
import org.svu.sedra.a11ylint.util.ComposeCalls
import org.svu.sedra.a11ylint.util.Literals
import org.svu.sedra.a11ylint.util.ModifierChain

/**
 * P-07: a custom overlay built with `Popup` or `Dialog` that sets no `paneTitle`.
 *
 * The Material overlays are not reported: `AlertDialog`, `ModalBottomSheet` and the modal
 * drawers all set a `paneTitle` of their own, checked in the Material3 1.4.0 sources (see
 * DECISIONS). `androidx.compose.ui.window.Popup` and `Dialog` are the raw primitives and set
 * none, so an overlay built on them arrives with nothing for a screen reader to announce.
 */
class P07MissingPaneTitleDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf("Popup", "Dialog")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!ComposeCalls.isCall(node, OVERLAYS)) return
        if (setsPaneTitle(context, node)) return

        val name = ComposeCalls.name(node) ?: "overlay"
        context.report(
            ISSUE,
            node,
            context.getNameLocation(node),
            A11yIssues.message(
                TAXONOMY_ID,
                "$name has no paneTitle, so screen readers do not announce what opened",
            ),
        )
    }

    /** True when the overlay, or anything inside it, sets `paneTitle` in semantics. */
    private fun setsPaneTitle(context: JavaContext, overlay: UCallExpression): Boolean {
        if (PANE_TITLE in ModifierChain.semanticsAssignments(ModifierChain.modifierArgument(context, overlay))) {
            return true
        }
        var found = false
        val visitor = object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                if (!found && PANE_TITLE in ModifierChain.semanticsAssignments(
                        ModifierChain.modifierArgument(context, node),
                    )
                ) {
                    found = true
                }
                return found
            }
        }
        overlay.valueArguments
            .mapNotNull { Literals.unwrap(it) as? ULambdaExpression }
            .forEach { it.body.accept(visitor) }
        return found
    }

    companion object {
        const val TAXONOMY_ID = "P-07"

        private const val PANE_TITLE = "paneTitle"

        /**
         * The raw overlay primitives. `AlertDialog`, `ModalBottomSheet`, `ModalNavigationDrawer`
         * and the other Material overlays set their own `paneTitle` and are deliberately absent.
         */
        private val OVERLAYS = setOf(
            "androidx.compose.ui.window.Popup",
            "androidx.compose.ui.window.Dialog",
        )

        @JvmField
        val ISSUE: Issue = A11yIssues.create(
            id = "ComposeMissingPaneTitle",
            briefDescription = "Custom overlay without a paneTitle",
            explanation = """
                When an overlay opens, screen readers announce its pane title so the user knows \
                what appeared and where they now are. A `Popup` or `Dialog` sets none of its \
                own, unlike the Material overlays, so a custom one opens silently and the user \
                is left to work out what changed. This relates to WCAG 2.2 success criterion \
                1.3.1 (Info and Relationships). Set it on the overlay's content, for example \
                `Modifier.semantics { paneTitle = "Filter options" }`.
            """,
            priority = Priority.MINOR,
            detector = P07MissingPaneTitleDetector::class.java,
        )
    }
}
