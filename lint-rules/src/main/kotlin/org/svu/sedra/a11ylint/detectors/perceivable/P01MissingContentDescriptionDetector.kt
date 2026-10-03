package org.svu.sedra.a11ylint.detectors.perceivable

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UMethod
import org.svu.sedra.a11ylint.taxonomy.A11yIssues
import org.svu.sedra.a11ylint.taxonomy.Priority
import org.svu.sedra.a11ylint.util.Clickables
import org.svu.sedra.a11ylint.util.ComposeCalls
import org.svu.sedra.a11ylint.util.ModifierChain

/**
 * P-01: an Icon or Image with a `null` or `""` contentDescription that is the only possible
 * label of a clickable element.
 *
 * The Icon or Image is interactive when its own modifier has a click or toggle modifier, or
 * when the nearest clickable element around it (an IconButton, a Material button, a Card or
 * Surface with `onClick`, or any composable with a clickable modifier) has no other name: no
 * Text, no labelled Icon or Image, and no `contentDescription` in semantics.
 */
class P01MissingContentDescriptionDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf("Icon", "Image")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!ComposeCalls.isCall(node, Clickables.imageCalls)) return
        if (Clickables.isLabeledImage(context, node)) return
        val description = ComposeCalls.argument(context, node, "contentDescription") ?: return

        val ownModifier = ModifierChain.modifierArgument(context, node)
        val interactive = Clickables.hasClickableModifier(ownModifier) || isOnlyLabelOfParent(context, node)
        if (!interactive) return

        val name = ComposeCalls.name(node) ?: "Icon"
        context.report(
            ISSUE,
            node,
            context.getLocation(description),
            A11yIssues.message(
                TAXONOMY_ID,
                "Interactive $name has no contentDescription, so screen readers announce it without a name",
            ),
        )
    }

    /**
     * Finds the nearest clickable element whose content lambda contains [image] and returns
     * true when that element has no name other than [image]. The search stops at the enclosing
     * function, so a clickable parent in another composable function is not seen.
     */
    private fun isOnlyLabelOfParent(context: JavaContext, image: UCallExpression): Boolean {
        var element: UElement? = image.uastParent
        while (element != null && element !is UMethod) {
            if (element is ULambdaExpression) {
                val parent = element.uastParent as? UCallExpression
                if (parent != null && Clickables.isClickableElement(context, parent)) {
                    return !Clickables.hasAccessibleName(context, parent, ignored = image)
                }
            }
            element = element.uastParent
        }
        return false
    }

    companion object {
        const val TAXONOMY_ID = "P-01"

        @JvmField
        val ISSUE: Issue = A11yIssues.create(
            id = "ComposeMissingContentDescription",
            briefDescription = "Interactive Icon or Image without contentDescription",
            explanation = """
                Icons and images that respond to clicks need a text label. Without one, TalkBack \
                only says "button" and the user cannot tell what it does. This relates to WCAG 2.2 \
                success criterion 1.1.1 (Non-text Content). Give the Icon a `contentDescription` \
                that describes the action, for example "Delete draft", or add a visible Text to \
                the button.
            """,
            priority = Priority.CRITICAL,
            detector = P01MissingContentDescriptionDetector::class.java,
        )
    }
}
