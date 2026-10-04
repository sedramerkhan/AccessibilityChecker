package org.svu.sedra.a11ylint.detectors.perceivable

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UMethod
import org.svu.sedra.a11ylint.taxonomy.A11yIssues
import org.svu.sedra.a11ylint.taxonomy.Priority
import org.svu.sedra.a11ylint.util.Clickables
import org.svu.sedra.a11ylint.util.ComposeCalls
import org.svu.sedra.a11ylint.util.Contrast
import org.svu.sedra.a11ylint.util.Literals
import org.svu.sedra.a11ylint.util.ModifierChain
import org.svu.sedra.a11ylint.util.TextStyles

/**
 * P-03: a `Text` whose literal `color` has too little contrast against a literal background.
 *
 * The background is read from the Text's own `Modifier.background(...)`, or else from the
 * single composable call whose content lambda directly contains the Text: a `Surface(color =
 * ...)` or a `Box`/`Row`/`Column` with `Modifier.background(...)`. Only literal colors are
 * compared; a theme color or a variable makes the background (or the text color) unknown, and
 * the rule stays silent rather than guess.
 */
class P03LowContrastColorsDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf("Text")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!ComposeCalls.isCall(node, Clickables.textCalls)) return
        val textColor = Literals.colorLiteralValue(ComposeCalls.argument(context, node, "color")) ?: return
        val backgroundColor = backgroundColor(context, node) ?: return
        val threshold = if (isLargeText(context, node)) LARGE_TEXT_THRESHOLD else NORMAL_TEXT_THRESHOLD
        val ratio = Contrast.ratio(textColor, backgroundColor)
        if (ratio >= threshold) return
        context.report(
            ISSUE,
            node,
            context.getLocation(ComposeCalls.argument(context, node, "color") ?: node),
            A11yIssues.message(
                TAXONOMY_ID,
                "Text color has a contrast ratio of ${oneDecimal(ratio)}:1 against its background, " +
                    "below the required ${oneDecimal(threshold)}:1",
            ),
        )
    }

    /** Formats a ratio with one decimal place, independent of the platform locale. */
    private fun oneDecimal(value: Double): String {
        val tenths = Math.round(value * 10)
        return "${tenths / 10}.${tenths % 10}"
    }

    private fun isLargeText(context: JavaContext, text: UCallExpression): Boolean {
        val sizeSp = TextStyles.fontSizeSp(context, text) ?: return false
        return sizeSp >= 18f || (sizeSp >= 14f && TextStyles.isBold(context, text))
    }

    /** The literal background color behind [text]: its own modifier, or its direct container's. */
    private fun backgroundColor(context: JavaContext, text: UCallExpression): Int? {
        modifierBackgroundColor(context, ModifierChain.modifierArgument(context, text))?.let { return it }
        val container = directParentCall(text) ?: return null
        return when {
            ComposeCalls.isCall(container, SURFACE_CALLS) ->
                Literals.colorLiteralValue(ComposeCalls.argument(context, container, "color"))
            ComposeCalls.isCall(container, Clickables.layoutContainers) ->
                modifierBackgroundColor(context, ModifierChain.modifierArgument(context, container))
            else -> null
        }
    }

    private fun modifierBackgroundColor(context: JavaContext, modifier: UExpression?): Int? {
        val background = ModifierChain.findModifier(modifier, "background") ?: return null
        val color = ComposeCalls.argument(context, background, "color", BACKGROUND_PARAMETER_NAMES)
        return Literals.colorLiteralValue(color)
    }

    /** The composable call whose content lambda directly contains [node], if any. */
    private fun directParentCall(node: UElement): UCallExpression? {
        var element: UElement? = node.uastParent
        while (element != null && element !is UMethod) {
            if (element is ULambdaExpression) return element.uastParent as? UCallExpression
            element = element.uastParent
        }
        return null
    }

    companion object {
        const val TAXONOMY_ID = "P-03"
        private const val NORMAL_TEXT_THRESHOLD = 4.5
        private const val LARGE_TEXT_THRESHOLD = 3.0

        private val SURFACE_CALLS = setOf("androidx.compose.material3.Surface", "androidx.compose.material.Surface")

        /**
         * Kotlin parameter names of `Modifier.background(color: Color, shape: Shape = ...)`.
         * `color`'s compiled name is lost because `Color` is a value class (see
         * `ComposeCalls.argument`), so a positional call such as `Modifier.background(myColor)`
         * needs this to find the argument.
         */
        private val BACKGROUND_PARAMETER_NAMES = listOf("color", "shape")

        @JvmField
        val ISSUE: Issue = A11yIssues.create(
            id = "ComposeLowContrastColors",
            briefDescription = "Text color has too little contrast against its background",
            explanation = """
                Text with low contrast against its background is hard to read for users with \
                low vision or color vision deficiencies. This relates to WCAG 2.2 success \
                criterion 1.4.3 (Contrast (Minimum)), which requires a ratio of at least 4.5:1 \
                for normal text and 3:1 for large text (18sp and above, or 14sp and above when \
                bold). Use a darker or lighter literal color, or a theme color already checked \
                for contrast.
            """,
            priority = Priority.MAJOR,
            detector = P03LowContrastColorsDetector::class.java,
        )
    }
}
