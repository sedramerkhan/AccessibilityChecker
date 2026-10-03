package org.svu.sedra.a11ylint.util

import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression

/**
 * Reads the text style of a `Text(...)` call: the Material typography style it uses, its font
 * size in sp and whether its font weight is bold. Only values written in the call are read.
 */
object TextStyles {
    private const val FONT_PACKAGE = "androidx.compose.ui.text.font"
    private val typographyPackages = setOf("androidx.compose.material3", "androidx.compose.material")

    /** Font weights of 700 and above, the CSS and WCAG meaning of "bold". */
    val boldWeights = setOf("Bold", "ExtraBold", "Black", "W700", "W800", "W900")

    /**
     * Returns the name of the Material typography style an expression reads, for example
     * `headlineSmall` for `MaterialTheme.typography.headlineSmall` or for
     * `MaterialTheme.typography.headlineSmall.copy(color = ...)`. Returns null for anything else,
     * including a style stored in a variable.
     */
    fun typographyStyle(expression: UExpression?): String? {
        val value = Literals.unwrap(expression) as? UQualifiedReferenceExpression ?: return null
        val selector = Literals.unwrap(value.selector)
        if (selector is UCallExpression) {
            return if (ComposeCalls.name(selector) == "copy") typographyStyle(value.receiver) else null
        }
        val reference = selector as? USimpleNameReferenceExpression ?: return null
        val resolved = reference.resolve() as? PsiNamedElement ?: return null
        if (ComposeCalls.packageName(resolved) !in typographyPackages) return null
        val name = if (resolved is PsiMethod) Literals.propertyName(resolved.name) else resolved.name
        val receiver = Literals.unwrap(value.receiver) as? UQualifiedReferenceExpression ?: return null
        val receiverName = (Literals.unwrap(receiver.selector) as? USimpleNameReferenceExpression)?.identifier
        return name.takeIf { receiverName == "typography" }
    }

    /**
     * Returns the font size in sp set on a `Text` call: its `fontSize` argument, or else the
     * `fontSize` of a `TextStyle(...)` or `.copy(...)` passed as `style`. Returns null when no
     * literal size is written.
     */
    fun fontSizeSp(context: JavaContext, text: UCallExpression): Float? =
        Literals.spValue(ComposeCalls.argument(context, text, "fontSize"))
            ?: styleCall(context, text)?.let { Literals.spValue(ComposeCalls.argument(context, it, "fontSize")) }

    /**
     * Returns true when a `Text` call sets a bold font weight (700 or more) through its
     * `fontWeight` argument, or through a `TextStyle(...)` or `.copy(...)` passed as `style`.
     */
    fun isBold(context: JavaContext, text: UCallExpression): Boolean {
        val weight = ComposeCalls.argument(context, text, "fontWeight")
            ?: styleCall(context, text)?.let { ComposeCalls.argument(context, it, "fontWeight") }
        return isBoldWeight(weight)
    }

    /** Returns true for `FontWeight.Bold`, `ExtraBold`, `Black`, `W700`..`W900` or `FontWeight(700+)`. */
    fun isBoldWeight(expression: UExpression?): Boolean {
        when (val value = Literals.unwrap(expression)) {
            is UQualifiedReferenceExpression -> {
                val selector = Literals.unwrap(value.selector)
                if (selector is UCallExpression) return isBoldWeight(selector)
                val reference = selector as? USimpleNameReferenceExpression ?: return false
                val resolved = reference.resolve() as? PsiNamedElement ?: return false
                val name = if (resolved is PsiMethod) Literals.propertyName(resolved.name) else resolved.name
                // A getter name gives a lowercase property name (`getBold` gives `bold`).
                return boldWeights.any { it.equals(name, ignoreCase = true) } &&
                    ComposeCalls.packageName(resolved) == FONT_PACKAGE
            }
            is UCallExpression -> {
                if (!ComposeCalls.isCall(value, "$FONT_PACKAGE.FontWeight")) return false
                val weight = (Literals.unwrap(value.valueArguments.firstOrNull()) as? ULiteralExpression)?.value
                return (weight as? Number)?.toInt()?.let { it >= 700 } == true
            }
            else -> return false
        }
    }

    /** The `TextStyle(...)` constructor call or `.copy(...)` call passed as `style`, if any. */
    private fun styleCall(context: JavaContext, text: UCallExpression): UCallExpression? {
        val style = Literals.unwrap(ComposeCalls.argument(context, text, "style"))
        val call = when (style) {
            is UCallExpression -> style
            is UQualifiedReferenceExpression -> Literals.unwrap(style.selector) as? UCallExpression
            else -> null
        } ?: return null
        val isStyle = ComposeCalls.isCall(call, "androidx.compose.ui.text.TextStyle") ||
            ComposeCalls.isCall(call, "androidx.compose.ui.text.TextStyle.copy")
        return call.takeIf { isStyle }
    }
}
