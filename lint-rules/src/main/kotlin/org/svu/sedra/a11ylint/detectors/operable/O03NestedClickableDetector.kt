package org.svu.sedra.a11ylint.detectors.operable

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UMethod
import org.svu.sedra.a11ylint.taxonomy.A11yIssues
import org.svu.sedra.a11ylint.taxonomy.Priority
import org.svu.sedra.a11ylint.util.Clickables
import org.svu.sedra.a11ylint.util.ComposeCalls

/**
 * O-03: a clickable element inside the content of another clickable element.
 *
 * For every clickable element (see [Clickables.isClickableElement]) the detector looks for the
 * nearest clickable element whose content lambda contains it, in the same function. Layout
 * calls in between (`Row`, `Column`, `Box` without a click modifier) are passed through. The
 * inner element is reported, so each nested clickable gives one warning.
 *
 * A Material selection control with a live callback counts as a nested clickable too, because
 * it installs its own toggle modifier (see [Clickables.isInteractiveSelectionControl]). It is
 * only ever the inner element: a selection control takes no content, so it cannot contain one.
 */
class O03NestedClickableDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler = object : UElementHandler() {
        override fun visitCallExpression(node: UCallExpression) {
            val control = Clickables.isInteractiveSelectionControl(context, node)
            if (!control && !Clickables.isClickableElement(context, node)) return
            val outer = enclosingClickable(context, node) ?: return
            val inner = ComposeCalls.name(node) ?: "element"
            val outerName = ComposeCalls.name(outer)
            val message = if (control) {
                "$inner handles its own clicks inside the clickable $outerName, so the two " +
                    "become separate targets and screen readers announce the label and the " +
                    "state as different elements"
            } else {
                "Clickable $inner is nested inside the clickable $outerName, " +
                    "so screen readers may skip it or announce both as one element"
            }
            context.report(ISSUE, node, context.getNameLocation(node), A11yIssues.message(TAXONOMY_ID, message))
        }
    }

    /** Returns the nearest clickable element whose content lambda contains [call], in the same function. */
    private fun enclosingClickable(context: JavaContext, call: UCallExpression): UCallExpression? {
        var element: UElement? = call.uastParent
        while (element != null && element !is UMethod) {
            if (element is ULambdaExpression) {
                val parent = element.uastParent as? UCallExpression
                if (parent != null && Clickables.isClickableElement(context, parent)) return parent
            }
            element = element.uastParent
        }
        return null
    }

    companion object {
        const val TAXONOMY_ID = "O-03"

        @JvmField
        val ISSUE: Issue = A11yIssues.create(
            id = "ComposeNestedClickable",
            briefDescription = "Clickable element nested inside another clickable element",
            explanation = """
                A clickable element inside another clickable element gives two overlapping \
                touch targets. TalkBack may merge them into one element, skip the inner one, or \
                read them in a confusing order, and switch access users cannot tell which action \
                they will trigger. This relates to WCAG 2.2 success criteria 2.4.3 (Focus Order) \
                and 4.1.2 (Name, Role, Value). Keep one clickable per element: make the outer \
                container non-clickable, keep the outer click and offer the inner action \
                through `semantics { customActions = ... }`, or, for a Checkbox, Switch or \
                RadioButton inside a toggleable row, pass `null` as the control's callback so \
                the row owns the interaction and the two are announced as one control.
            """,
            priority = Priority.CRITICAL,
            detector = O03NestedClickableDetector::class.java,
        )
    }
}
