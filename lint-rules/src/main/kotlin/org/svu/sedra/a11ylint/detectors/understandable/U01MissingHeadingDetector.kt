package org.svu.sedra.a11ylint.detectors.understandable

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
import org.svu.sedra.a11ylint.util.TextStyles

/**
 * U-01 (candidate): a `Text` that looks like a heading but has no `heading()` semantics.
 *
 * A Text looks like a heading when its `style` is a Material display, headline or `titleLarge`
 * style (Material 2: `h1`..`h6`), or when it is at least 20sp and bold. Text inside a Material
 * button or a top app bar is ignored. Whether the text really is a heading is a judgement about
 * intent, so the message says "Possible".
 */
class U01MissingHeadingDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf("Text")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!ComposeCalls.isCall(node, Clickables.textCalls)) return
        val reason = headingReason(context, node) ?: return
        val modifier = ModifierChain.modifierArgument(context, node)
        if ("heading" in ModifierChain.semanticsAssignments(modifier)) return
        if (isInsideIgnoredContainer(context, node)) return

        context.report(
            ISSUE,
            node,
            context.getNameLocation(node),
            A11yIssues.message(
                TAXONOMY_ID,
                "Possible heading: this Text is styled like a heading ($reason) but has no heading() " +
                    "semantics, so screen reader users cannot jump to it",
            ),
        )
    }

    /** Returns why the Text looks like a heading, or null when it does not. */
    private fun headingReason(context: JavaContext, text: UCallExpression): String? {
        val style = TextStyles.typographyStyle(ComposeCalls.argument(context, text, "style"))
        if (style != null && style in HEADING_STYLES) return style
        val size = TextStyles.fontSizeSp(context, text) ?: return null
        if (size < MIN_HEADING_SP || !TextStyles.isBold(context, text)) return null
        val shown = if (size == size.toLong().toFloat()) size.toLong().toString() else size.toString()
        return "${shown}sp bold"
    }

    /** Text inside a Material button or a top app bar, in the same function, is not checked. */
    private fun isInsideIgnoredContainer(context: JavaContext, text: UCallExpression): Boolean {
        var element: UElement? = text.uastParent
        while (element != null && element !is UMethod) {
            if (element is ULambdaExpression) {
                val parent = element.uastParent as? UCallExpression
                if (parent != null &&
                    (ComposeCalls.isCall(parent, Clickables.clickableComponents) || ComposeCalls.isCall(parent, TOP_APP_BARS))
                ) {
                    return true
                }
            }
            element = element.uastParent
        }
        return false
    }

    companion object {
        const val TAXONOMY_ID = "U-01"

        /** Smallest bold font size, in sp, that is treated as a heading. */
        const val MIN_HEADING_SP = 20f

        /** Material 3 heading styles from CLAUDE.md, plus the Material 2 `h1`..`h6` styles. */
        val HEADING_STYLES = setOf(
            "displayLarge", "displayMedium", "displaySmall",
            "headlineLarge", "headlineMedium", "headlineSmall",
            "titleLarge",
            "h1", "h2", "h3", "h4", "h5", "h6",
        )

        private val TOP_APP_BARS = listOf(
            "androidx.compose.material3.TopAppBar",
            "androidx.compose.material3.CenterAlignedTopAppBar",
            "androidx.compose.material3.MediumTopAppBar",
            "androidx.compose.material3.LargeTopAppBar",
            "androidx.compose.material.TopAppBar",
        )

        @JvmField
        val ISSUE: Issue = A11yIssues.create(
            id = "ComposeMissingHeading",
            briefDescription = "Heading-styled Text without heading semantics",
            explanation = """
                Screen reader users move through a screen by jumping from heading to heading. \
                A Text that looks like a heading but is not marked as one is read as plain text, \
                so these users lose the structure that sighted users see. This relates to WCAG 2.2 \
                success criteria 1.3.1 (Info and Relationships) and 2.4.6 (Headings and Labels). \
                If the Text is a section title, add `Modifier.semantics { heading() }`.
            """,
            priority = Priority.CRITICAL,
            detector = U01MissingHeadingDetector::class.java,
        )
    }
}
