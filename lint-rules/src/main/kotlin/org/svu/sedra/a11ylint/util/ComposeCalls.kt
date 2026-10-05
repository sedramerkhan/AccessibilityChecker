package org.svu.sedra.a11ylint.util

import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiParameter
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULambdaExpression

/** Helpers for resolving Compose calls and their arguments in UAST. */
object ComposeCalls {
    /** Fully qualified name of the Compose `@Composable` annotation. */
    const val COMPOSABLE = "androidx.compose.runtime.Composable"

    /** Name the compiled library gives a parameter whose real name was lost. */
    private val SYNTHETIC_NAME = Regex("""p\d*""")

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
        val name = declaredName(method.name)
        val names = mutableListOf("$ownerName.$name")
        val packageName = packageName(method)
        if (method.hasModifierProperty(PsiModifier.STATIC) && packageName != null) {
            names += if (packageName.isEmpty()) name else "$packageName.$name"
        }
        return names
    }

    /**
     * Returns the Kotlin name for a JVM method name. Kotlin mangles the JVM name of a function
     * that takes a value class parameter by adding a hash, so the compiled Material3 `Icon`
     * (which takes `tint: Color`) is `Icon-ww6aTOc` and `clickable` (which takes `role: Role?`)
     * is mangled too. A Kotlin identifier cannot contain `-`, so everything from the first `-`
     * is removed.
     */
    fun declaredName(jvmName: String): String = jvmName.substringBefore('-')

    /** Returns the package that declares [element], or null when it cannot be found. */
    fun packageName(element: PsiElement): String? =
        (element.containingFile as? PsiClassOwner)?.packageName

    /**
     * Returns the declared name of the called function. This uses the resolved method when it
     * can, so an import alias (`import ...clickable as click`) still gives `clickable`. JVM name
     * mangling is removed (see [declaredName]).
     */
    fun name(call: UCallExpression): String? =
        call.resolve()?.name?.let(::declaredName) ?: call.methodName

    /**
     * Returns the argument expression passed for the parameter [name], whether the argument
     * was written by name or by position. Returns null when the argument is not passed.
     *
     * An argument written by name in the Kotlin source (`name = ...`) is used first, because
     * the Kotlin compiler already matched that name to a parameter of the overload that really
     * applies. Only then is the resolved method's argument mapping used, which handles
     * positional arguments and these two problems of compiled Kotlin libraries:
     *
     * - A parameter whose type is a value class (for example `size: Dp`) loses its name in the
     *   compiled library and appears as `p`, `p0`, ... When the caller passes [kotlinNames], the
     *   Kotlin parameter names of the resolved overload in declaration order (without the
     *   extension receiver), such a parameter is found by its position.
     * - Lint can resolve a call to the wrong overload. Against the compiled Material3 1.4.0
     *   library, `Card(onClick = ...) { }` resolves to the `Card(modifier, ...)` overload, which
     *   has no `onClick` parameter, and a `TextField(value = ..., onValueChange = ...)` call
     *   resolves to the `TextFieldState` overload, whose parameters are shifted by one, so the
     *   mapping returns the wrong argument rather than none. Reading the source name first
     *   avoids both.
     */
    fun argument(
        context: JavaContext,
        call: UCallExpression,
        name: String,
        kotlinNames: List<String>? = null,
    ): UExpression? = namedArgumentInSource(call, name) ?: mappedArgument(context, call, name, kotlinNames)

    /**
     * Returns the value parameters of [method] without the extension receiver (`$this$...`) and
     * without the parameters the Compose compiler adds (`$composer`, `$changed`, `$default`).
     */
    fun valueParameters(method: PsiMethod): List<PsiParameter> =
        method.parameterList.parameters.filterNot { it.name.startsWith("$") }

    private fun mappedArgument(
        context: JavaContext,
        call: UCallExpression,
        name: String,
        kotlinNames: List<String>?,
    ): UExpression? {
        val method = call.resolve() ?: return null
        val parameter = method.parameterList.parameters.firstOrNull { it.name == name }
            ?: positionalParameter(method, name, kotlinNames)
            ?: return null
        return context.evaluator.computeArgumentMapping(call, method)
            .entries
            .firstOrNull { it.value == parameter }
            ?.key
    }

    /** Finds the parameter for [name] by its position, only when its compiled name was lost. */
    private fun positionalParameter(method: PsiMethod, name: String, kotlinNames: List<String>?): PsiParameter? {
        val index = kotlinNames?.indexOf(name)?.takeIf { it >= 0 } ?: return null
        val parameter = valueParameters(method).getOrNull(index) ?: return null
        return parameter.takeIf { SYNTHETIC_NAME.matches(it.name) }
    }

    private fun namedArgumentInSource(call: UCallExpression, name: String): UExpression? {
        val ktCall = call.sourcePsi as? KtCallElement ?: return null
        val expression = ktCall.valueArguments
            .firstOrNull { it.getArgumentName()?.asName?.asString() == name }
            ?.getArgumentExpression() ?: return null
        return call.valueArguments.firstOrNull { it.sourcePsi == expression }
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
