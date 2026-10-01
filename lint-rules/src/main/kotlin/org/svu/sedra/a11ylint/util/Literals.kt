package org.svu.sedra.a11ylint.util

import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UQualifiedReferenceExpression

/** Helpers for reading literal values from Compose call arguments. */
object Literals {
    /** Returns true when an expression is the Kotlin null literal. */
    fun isNullLiteral(expression: UExpression?): Boolean =
        expression is ULiteralExpression && expression.value == null

    /** Returns true when an expression is an empty string literal. */
    fun isEmptyStringLiteral(expression: UExpression?): Boolean =
        expression is ULiteralExpression && expression.value == ""

    /** Returns the string value when an expression is a string literal. */
    fun stringLiteralValue(expression: UExpression?): String? =
        (expression as? ULiteralExpression)?.value as? String

    /** Returns true for calls to stringResource or pluralStringResource. */
    fun isStringResourceCall(expression: UExpression?): Boolean {
        val call = when (expression) {
            is UCallExpression -> expression
            is UQualifiedReferenceExpression -> expression.selector as? UCallExpression
            else -> null
        }
        return call?.methodName == "stringResource" || call?.methodName == "pluralStringResource"
    }

    /** Reads a numeric dp literal from expressions such as 20.dp. */
    fun dpValue(expression: UExpression?): Float? = dimensionValue(expression, "dp")

    /** Reads a numeric sp literal from expressions such as 16.sp. */
    fun spValue(expression: UExpression?): Float? = dimensionValue(expression, "sp")

    private fun dimensionValue(expression: UExpression?, unit: String): Float? {
        val call = when (expression) {
            is UCallExpression -> expression
            is UQualifiedReferenceExpression -> expression.selector as? UCallExpression
            else -> null
        } ?: return null
        if (call.methodName != unit) return null
        return ((call.receiver as? ULiteralExpression)?.value as? Number)?.toFloat()
    }
}
