package org.svu.sedra.a11ylint.util

import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UParenthesizedExpression
import org.jetbrains.uast.UPolyadicExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.UastBinaryOperator

/**
 * Helpers for reading literal values from Compose call arguments.
 *
 * Only values written directly in the source count as literals. Constants and local
 * variables are not followed, so a value that may change at run time is never treated as
 * known.
 */
object Literals {
    private const val UNIT_PACKAGE = "androidx.compose.ui.unit"
    private const val RESOURCE_PACKAGE = "androidx.compose.ui.res"

    /** Removes any parentheses around an expression, so `((x))` gives `x`. */
    fun unwrap(expression: UExpression?): UExpression? {
        var current = expression
        while (current is UParenthesizedExpression) current = current.expression
        return current
    }

    /** Returns true when an expression is the Kotlin `null` literal. */
    fun isNullLiteral(expression: UExpression?): Boolean {
        val literal = unwrap(expression) as? ULiteralExpression ?: return false
        return literal.isNull
    }

    /** Returns true when an expression is an empty string literal (`""`). */
    fun isEmptyStringLiteral(expression: UExpression?): Boolean = stringLiteralValue(expression) == ""

    /**
     * Returns the value of a string literal. A Kotlin string template counts only when every
     * part is literal text, so `"Hello"` gives `Hello` and `"Hi $name"` gives null.
     */
    fun stringLiteralValue(expression: UExpression?): String? =
        when (val value = unwrap(expression)) {
            is ULiteralExpression -> value.value as? String
            is UPolyadicExpression -> polyadicStringValue(value)
            else -> null
        }

    /** Returns true for calls to Compose `stringResource(...)` or `pluralStringResource(...)`. */
    fun isStringResourceCall(expression: UExpression?): Boolean {
        val call = selectorCall(expression) ?: return false
        return ComposeCalls.isCall(
            call,
            listOf("$RESOURCE_PACKAGE.stringResource", "$RESOURCE_PACKAGE.pluralStringResource"),
        )
    }

    /** Reads a numeric dp literal, so `20.dp` gives `20f`. Returns null for anything else. */
    fun dpValue(expression: UExpression?): Float? = dimensionValue(expression, "dp")

    /** Reads a numeric sp literal, so `16.sp` gives `16f`. Returns null for anything else. */
    fun spValue(expression: UExpression?): Float? = dimensionValue(expression, "sp")

    private fun polyadicStringValue(expression: UPolyadicExpression): String? {
        if (expression.operator != UastBinaryOperator.PLUS) return null
        val parts = expression.operands.map { stringLiteralValue(it) ?: return null }
        if (parts.isEmpty() && expression.getExpressionType()?.canonicalText != "java.lang.String") {
            return null
        }
        return parts.joinToString("")
    }

    private fun selectorCall(expression: UExpression?): UCallExpression? =
        when (val value = unwrap(expression)) {
            is UCallExpression -> value
            is UQualifiedReferenceExpression -> value.selector as? UCallExpression
            else -> null
        }

    /**
     * `20.dp` is a property access in Kotlin, not a call: a qualified expression whose receiver
     * is the number and whose selector is a reference to the `dp` extension property.
     */
    private fun dimensionValue(expression: UExpression?, unit: String): Float? {
        val qualified = unwrap(expression) as? UQualifiedReferenceExpression ?: return null
        val selector = qualified.selector as? USimpleNameReferenceExpression ?: return null
        if (!isUnitProperty(selector, unit)) return null
        val number = (unwrap(qualified.receiver) as? ULiteralExpression)?.value as? Number ?: return null
        return number.toFloat()
    }

    private fun isUnitProperty(selector: USimpleNameReferenceExpression, unit: String): Boolean {
        val resolved = selector.resolve() as? PsiNamedElement ?: return selector.identifier == unit
        val name = if (resolved is PsiMethod) propertyName(resolved.name) else resolved.name
        return name == unit && ComposeCalls.packageName(resolved) == UNIT_PACKAGE
    }

    /**
     * Turns a getter or setter name such as `getDp` into the property name `dp`. A mangled JVM
     * name such as `setRole-kuIjeqM` gives `role` (see [ComposeCalls.declaredName]).
     */
    internal fun propertyName(jvmAccessorName: String): String {
        val accessorName = ComposeCalls.declaredName(jvmAccessorName)
        val prefix = listOf("get", "set").firstOrNull {
            accessorName.length > it.length &&
                accessorName.startsWith(it) &&
                accessorName[it.length].isUpperCase()
        } ?: return accessorName
        return accessorName.substring(prefix.length).replaceFirstChar { it.lowercaseChar() }
    }
}
