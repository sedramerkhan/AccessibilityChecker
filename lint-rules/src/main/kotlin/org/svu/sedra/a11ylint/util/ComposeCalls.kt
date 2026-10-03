package org.svu.sedra.a11ylint.util

import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULambdaExpression

/** Helpers for resolving Compose calls and their arguments in UAST. */
object ComposeCalls {
    /** Fully qualified name of the Compose `@Composable` annotation. */
    const val COMPOSABLE = "androidx.compose.runtime.Composable"

    /** Returns true when the resolved method has the Compose `@Composable` annotation. */
    fun isComposableCall(call: UCallExpression): Boolean =
        call.resolve()?.hasAnnotation(COMPOSABLE) == true

    /**
     * Returns true when [call] resolves to [fqName].
     *
     * A top-level Kotlin function matches `<package>.<name>` (for example
     * `androidx.compose.material3.Icon`), so the file facade class (`IconKt`) does not matter.
     * A member function matches `<class>.<name>`, and a constructor matches the class name.
     */
    fun isCall(call: UCallExpression, fqName: String): Boolean {
        val method = call.resolve() ?: return false
        return fqName in qualifiedNames(method)
    }

    /** Returns true when [call] resolves to any of [fqNames]. */
    fun isCall(call: UCallExpression, fqNames: Collection<String>): Boolean {
        val method = call.resolve() ?: return false
        return qualifiedNames(method).any { it in fqNames }
    }

    /**
     * Returns the fully qualified names a resolved method can be referred to by. See [isCall]
     * for the naming scheme.
     */
    fun qualifiedNames(method: PsiMethod): List<String> {
        val owner = method.containingClass ?: return emptyList()
        val ownerName = owner.qualifiedName ?: return emptyList()
        if (method.isConstructor) return listOf(ownerName)
        val names = mutableListOf("$ownerName.${method.name}")
        val packageName = packageName(method)
        if (method.hasModifierProperty(PsiModifier.STATIC) && packageName != null) {
            names += if (packageName.isEmpty()) method.name else "$packageName.${method.name}"
        }
        return names
    }

    /** Returns the package that declares [element], or null when it cannot be found. */
    fun packageName(element: PsiElement): String? =
        (element.containingFile as? PsiClassOwner)?.packageName

    /**
     * Returns the declared name of the called function. This uses the resolved method when it
     * can, so an import alias (`import ...clickable as click`) still gives `clickable`.
     */
    fun name(call: UCallExpression): String? = call.resolve()?.name ?: call.methodName

    /**
     * Returns the argument expression passed for the parameter [name], whether the argument
     * was written by name or by position. Returns null when the argument is not passed or the
     * call cannot be resolved.
     */
    fun argument(context: JavaContext, call: UCallExpression, name: String): UExpression? {
        val method = call.resolve() ?: return null
        val parameter = method.parameterList.parameters.firstOrNull { it.name == name } ?: return null
        return context.evaluator.computeArgumentMapping(call, method)
            .entries
            .firstOrNull { it.value == parameter }
            ?.key
    }

    /**
     * Returns the lambda passed for the composable content parameter [parameterName]
     * (`content` by default), whether it is a trailing lambda, a named argument or a
     * positional argument. Returns null when the argument is not a lambda literal.
     */
    fun contentLambda(
        context: JavaContext,
        call: UCallExpression,
        parameterName: String = "content",
    ): ULambdaExpression? =
        Literals.unwrap(argument(context, call, parameterName)) as? ULambdaExpression
}
