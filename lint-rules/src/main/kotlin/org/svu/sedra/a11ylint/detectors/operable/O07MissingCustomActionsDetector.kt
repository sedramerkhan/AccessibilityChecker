package org.svu.sedra.a11ylint.detectors.operable

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.svu.sedra.a11ylint.taxonomy.A11yIssues
import org.svu.sedra.a11ylint.taxonomy.Priority
import org.svu.sedra.a11ylint.util.Clickables
import org.svu.sedra.a11ylint.util.ComposeCalls
import org.svu.sedra.a11ylint.util.Literals
import org.svu.sedra.a11ylint.util.ModifierChain

/**
 * O-07: a clickable `Card`, `Surface` or `Row` that holds two or more clickable children and
 * offers no `customActions`.
 *
 * The usual shape is a list row that opens a detail screen and also carries a favourite and a
 * share button. A screen reader user reaches the row as one element and is offered only its own
 * action; the inner buttons are either skipped or hard to reach. `customActions` is the Compose
 * answer: it puts the inner actions in TalkBack's actions menu.
 *
 * Only the nearest clickable container is reported: a clickable child is counted but not
 * searched, so a clickable row inside a clickable card is reported on its own.
 */
class O07MissingCustomActionsDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler = object : UElementHandler() {
        override fun visitCallExpression(node: UCallExpression) {
            if (!ComposeCalls.isCall(node, CONTAINERS)) return
            if (!Clickables.isClickableElement(context, node)) return
            val modifier = ModifierChain.modifierArgument(context, node)
            if (CUSTOM_ACTIONS in ModifierChain.semanticsAssignments(modifier)) return

            val children = clickableChildren(context, node)
            if (children < MINIMUM_CHILDREN) return

            val name = ComposeCalls.name(node) ?: "container"
            context.report(
                ISSUE,
                node,
                context.getNameLocation(node),
                A11yIssues.message(
                    TAXONOMY_ID,
                    "Possible missing custom actions: clickable $name holds $children clickable " +
                        "children but offers no customActions, so screen readers announce one " +
                        "element with only its own action",
                ),
            )
        }
    }

    /**
     * Counts the clickable elements inside the content of [container]. A clickable element is
     * counted and then not searched any further, so the children of a nested clickable belong to
     * that nested clickable and not to this container.
     */
    private fun clickableChildren(context: JavaContext, container: UCallExpression): Int {
        val content = content(context, container) ?: return 0
        var count = 0
        content.body.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                if (!Clickables.isClickableElement(context, node)) return false
                count++
                return true
            }
        })
        return count
    }

    /** The content lambda, falling back to the last lambda argument when it is not named `content`. */
    private fun content(context: JavaContext, container: UCallExpression): ULambdaExpression? =
        ComposeCalls.contentLambda(context, container)
            ?: container.valueArguments.mapNotNull { Literals.unwrap(it) as? ULambdaExpression }.lastOrNull()

    companion object {
        const val TAXONOMY_ID = "O-07"

        /** CLAUDE.md 7.4 names `Card`, `Surface` and `Row`. */
        private val CONTAINERS = setOf(
            "androidx.compose.material3.Card",
            "androidx.compose.material3.ElevatedCard",
            "androidx.compose.material3.OutlinedCard",
            "androidx.compose.material3.Surface",
            "androidx.compose.material.Card",
            "androidx.compose.material.Surface",
            "androidx.compose.foundation.layout.Row",
        )

        private const val CUSTOM_ACTIONS = "customActions"

        /** "two or more other clickable children" (CLAUDE.md 7.4). */
        private const val MINIMUM_CHILDREN = 2

        @JvmField
        val ISSUE: Issue = A11yIssues.create(
            id = "ComposeMissingCustomActions",
            briefDescription = "Clickable container with several clickable children has no customActions",
            explanation = """
                A clickable container that also holds several clickable children, such as a list \
                row with favourite and share buttons, is announced by TalkBack as one element \
                with one action. The inner buttons are then skipped or can only be reached by \
                exploring the screen by touch, which switch access and keyboard users cannot do \
                at all. This relates to WCAG 2.2 success criterion 2.1.1 (Keyboard). Offer the \
                inner actions through `Modifier.semantics { customActions = \
                listOf(CustomAccessibilityAction("Add to favourites") { ... }) }`, which puts \
                them in the accessibility actions menu.
            """,
            priority = Priority.MINOR,
            detector = O07MissingCustomActionsDetector::class.java,
        )
    }
}
