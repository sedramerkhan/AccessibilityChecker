package org.svu.sedra.a11ylint.util

import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULambdaExpression

/** Helpers for resolving Compose calls and their arguments in UAST. */
object ComposeCalls {
    /** Returns true when the resolved method has the Compose @Composable annotation. */
    fun isComposableCall(call: UCallExpression): Boolean =
        call.resolve()?.annotations?.any {
            it.qualifiedName == "androidx.compose.runtime.Composable"
        } == true

    /** Returns true when the resolved method belongs to the requested fully qualified class. */
    fun isCall(call: UCallExpression, fqName: String): Boolean {
        val method = call.resolve() ?: return false
        return method.containingClass?.qualifiedName == fqName ||
            method.containingFile?.name?.removeSuffix(".kt") == fqName.substringAfterLast('.')
    }

    /** Returns the expression mapped to a named or positional method parameter. */
    fun argument(context: JavaContext, call: UCallExpression, name: String): UExpression? {
        val method = call.resolve() ?: return null
        val parameter = method.parameterList.parameters.firstOrNull { it.name == name } ?: return null
        return context.evaluator.computeArgumentMapping(call, method)
            .entries
            .firstOrNull { it.value == parameter }
            ?.key
    }

    /** Returns the trailing lambda supplied to a call, when one exists. */
    fun contentLambda(call: UCallExpression): ULambdaExpression? =
        call.valueArguments.lastOrNull() as? ULambdaExpression
}
