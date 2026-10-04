package org.svu.sedra.a11ylint.detectors.perceivable

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReturnExpression
import org.svu.sedra.a11ylint.taxonomy.A11yIssues
import org.svu.sedra.a11ylint.taxonomy.Priority
import org.svu.sedra.a11ylint.util.Clickables
import org.svu.sedra.a11ylint.util.ComposeCalls
import org.svu.sedra.a11ylint.util.Literals
import org.svu.sedra.a11ylint.util.TextStyles

/**
 * P-04: a `fontSize` (on `Text` or in a `TextStyle`) computed from a dp value with `.toSp()`.
 *
 * `Dp.toSp()` converts using the current font scale, which is the right thing to do once, but it
 * means the resulting size no longer scales again when the user changes the system font size:
 * the developer has effectively pinned a dp size to text. A literal sp value or a typography
 * style always scales correctly.
 */
class P04TextSizeInDpDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf("Text")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!ComposeCalls.isCall(node, Clickables.textCalls)) return
        val fontSize = ownFontSize(context, node) ?: styleFontSize(context, node) ?: return
        if (!isDpToSpConversion(fontSize)) return
        context.report(
            ISSUE,
            node,
            context.getLocation(fontSize),
            A11yIssues.message(
                TAXONOMY_ID,
                "Text size is converted from a dp value, so it will not scale again when the " +
                    "user changes their system font size",
            ),
        )
    }

    private fun ownFontSize(context: JavaContext, text: UCallExpression): UExpression? =
        ComposeCalls.argument(context, text, "fontSize")

    private fun styleFontSize(context: JavaContext, text: UCallExpression): UExpression? {
        val style = TextStyles.styleCall(context, text) ?: return null
        return ComposeCalls.argument(context, style, "fontSize")
    }

    /**
     * Returns true when [expression] is, or evaluates to, a call to `Dp.toSp()`
     * (`androidx.compose.ui.unit.FontScaling.toSp`, inherited by `Density`), including through a
     * `with(density) { ... }` block. Anything else (a literal `.sp` value, a variable, a
     * typography style) is not recognised.
     */
    private fun isDpToSpConversion(expression: UExpression?): Boolean {
        val call = callOf(expression) ?: return false
        if (ComposeCalls.isCall(call, TO_SP_CALLS)) return true
        if (!ComposeCalls.isCall(call, "kotlin.with")) return false
        val lambda = Literals.unwrap(call.valueArguments.lastOrNull()) as? ULambdaExpression ?: return false
        return isDpToSpConversion(lastExpression(lambda))
    }

    private fun callOf(expression: UExpression?): UCallExpression? =
        when (val value = Literals.unwrap(expression)) {
            is UCallExpression -> value
            is UQualifiedReferenceExpression -> Literals.unwrap(value.selector) as? UCallExpression
            else -> null
        }

    /** The value a lambda body yields when used as an expression: its last statement. */
    private fun lastExpression(lambda: ULambdaExpression): UExpression? {
        val last = (lambda.body as? UBlockExpression)?.expressions?.lastOrNull() ?: lambda.body
        return (last as? UReturnExpression)?.returnExpression ?: last
    }

    companion object {
        const val TAXONOMY_ID = "P-04"

        private val TO_SP_CALLS = setOf(
            "androidx.compose.ui.unit.FontScaling.toSp",
            "androidx.compose.ui.unit.Density.toSp",
        )

        @JvmField
        val ISSUE: Issue = A11yIssues.create(
            id = "ComposeTextSizeInDp",
            briefDescription = "Text size converted from a dp value",
            explanation = """
                Text size set by converting a dp value with `.toSp()` is scaled once at the \
                point of conversion, but the result is a fixed sp value that does not scale \
                again when the user later increases the system font size. This relates to WCAG \
                2.2 success criterion 1.4.4 (Resize Text). Use a literal sp value (for example \
                `16.sp`) or a Material typography style instead.
            """,
            priority = Priority.MAJOR,
            detector = P04TextSizeInDpDetector::class.java,
        )
    }
}
