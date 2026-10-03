package org.svu.sedra.a11ylint.util

import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.ULocalVariable
import org.jetbrains.uast.UParenthesizedExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.UastBinaryOperator
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * Reads the modifier calls and semantics properties of a Compose modifier expression.
 *
 * `Modifier.a().b()` is a tree of nested qualified expressions in UAST. The chain is read
 * from the innermost receiver outwards, so calls are returned in source order.
 * `Modifier.then(x)` is expanded into the calls of `x`. A reference to a local `val` declared
 * in the same function is followed to its initializer. A modifier that comes from a function
 * parameter (for example the `modifier` parameter of a composable) is not followed.
 */
object ModifierChain {
    private const val SEMANTICS_PACKAGE = "androidx.compose.ui.semantics"
    private const val SEMANTICS_RECEIVER = "$SEMANTICS_PACKAGE.SemanticsPropertyReceiver"
    private const val MAX_LOCAL_DEPTH = 8
    private val semanticsBlockNames = setOf(
        "$SEMANTICS_PACKAGE.semantics",
        "$SEMANTICS_PACKAGE.clearAndSetSemantics",
    )

    /** Returns the expression passed as the `modifier` argument of a composable call. */
    fun modifierArgument(context: JavaContext, call: UCallExpression): UExpression? =
        ComposeCalls.argument(context, call, "modifier")

    /**
     * Returns the whole modifier expression that contains the modifier call [call]: walks up
     * through qualified expressions, parentheses and `then(...)` arguments. For
     * `Modifier.size(20.dp).clickable { }` and the `clickable` call, this is the full chain.
     */
    fun outermostExpression(call: UCallExpression): UExpression {
        var top: UExpression = call
        while (true) {
            val parent = top.uastParent
            top = when {
                parent is UQualifiedReferenceExpression -> parent
                parent is UParenthesizedExpression -> parent
                parent is UCallExpression && ComposeCalls.name(parent) == "then" &&
                    parent.valueArguments.any { it.sourcePsi == top.sourcePsi } -> parent
                else -> return top
            }
        }
    }

    /**
     * Returns the element that receives the modifier expression [expression] (usually the
     * composable call it is passed to), skipping parentheses.
     */
    fun receivingElement(expression: UExpression): UElement? {
        var parent = expression.uastParent
        while (parent is UParenthesizedExpression) parent = parent.uastParent
        return parent
    }

    /** Returns the modifier calls in source order, for example `[size, clickable, semantics]`. */
    fun calls(expression: UExpression?): List<UCallExpression> {
        val result = mutableListOf<UCallExpression>()
        collect(expression, result, depth = 0)
        return result
    }

    /**
     * Returns the first modifier call matching [name]. A name with a dot is compared with the
     * fully qualified name of the resolved function (see [ComposeCalls.isCall]). A simple name is
     * compared with the declared function name.
     */
    fun findModifier(expression: UExpression?, name: String): UCallExpression? =
        calls(expression).firstOrNull { matches(it, name) }

    /** Returns true when the chain contains a modifier call matching [name] (see [findModifier]). */
    fun hasModifier(expression: UExpression?, name: String): Boolean =
        findModifier(expression, name) != null

    /** Returns the `semantics { }` and `clearAndSetSemantics { }` calls in chain order. */
    fun semanticsBlocks(expression: UExpression?): List<UCallExpression> =
        calls(expression).filter { ComposeCalls.isCall(it, semanticsBlockNames) }

    /** Returns true when the chain contains `clearAndSetSemantics { }`. */
    fun hasClearAndSetSemantics(expression: UExpression?): Boolean =
        semanticsBlocks(expression).any { ComposeCalls.name(it) == "clearAndSetSemantics" }

    /**
     * Returns the semantics properties set inside every semantics block of the chain.
     * See [blockAssignments] for what counts.
     */
    fun semanticsAssignments(expression: UExpression?): Set<String> =
        semanticsBlocks(expression).flatMapTo(linkedSetOf()) { blockAssignments(it) }

    /**
     * Returns the semantics properties set inside one semantics block: property assignments
     * such as `contentDescription = "..."` or `role = Role.Button` give the property name, and
     * calls to semantics functions such as `heading()` or `error("...")` give the function name.
     * Assignments inside an `if` in the block count. Nested lambdas (for example the action of a
     * `CustomAccessibilityAction`) are not searched.
     */
    fun blockAssignments(block: UCallExpression): Set<String> {
        val lambda = block.valueArguments
            .mapNotNull { Literals.unwrap(it) as? ULambdaExpression }
            .lastOrNull() ?: return emptySet()
        val names = linkedSetOf<String>()
        lambda.body.accept(object : AbstractUastVisitor() {
            override fun visitLambdaExpression(node: ULambdaExpression): Boolean = true

            override fun visitBinaryExpression(node: UBinaryExpression): Boolean {
                if (node.operator == UastBinaryOperator.ASSIGN) {
                    assignedName(node.leftOperand)?.let(names::add)
                }
                return super.visitBinaryExpression(node)
            }

            override fun visitCallExpression(node: UCallExpression): Boolean {
                if (isSemanticsFunction(node)) ComposeCalls.name(node)?.let(names::add)
                return super.visitCallExpression(node)
            }
        })
        return names
    }

    private fun matches(call: UCallExpression, name: String): Boolean =
        if ('.' in name) ComposeCalls.isCall(call, name) else ComposeCalls.name(call) == name

    private fun collect(expression: UExpression?, result: MutableList<UCallExpression>, depth: Int) {
        when (val value = Literals.unwrap(expression)) {
            is UQualifiedReferenceExpression -> {
                collect(value.receiver, result, depth)
                (Literals.unwrap(value.selector) as? UCallExpression)?.let { add(it, result, depth) }
            }
            is UCallExpression -> add(value, result, depth)
            is USimpleNameReferenceExpression -> followLocal(value, result, depth)
            else -> Unit
        }
    }

    private fun add(call: UCallExpression, result: MutableList<UCallExpression>, depth: Int) {
        if (ComposeCalls.name(call) == "then") {
            collect(call.valueArguments.firstOrNull(), result, depth)
        } else {
            result += call
        }
    }

    /** Follows `val m = Modifier...` declared in the same function. `var` is not followed. */
    private fun followLocal(
        reference: USimpleNameReferenceExpression,
        result: MutableList<UCallExpression>,
        depth: Int,
    ) {
        if (depth >= MAX_LOCAL_DEPTH) return
        val variable = reference.resolve().toUElement() as? ULocalVariable ?: return
        if (!isReadOnly(variable)) return
        collect(variable.uastInitializer, result, depth + 1)
    }

    /**
     * UAST reports a Kotlin local `val` as not final, so the Kotlin declaration is checked
     * directly. A Java local counts when it is declared `final`.
     */
    private fun isReadOnly(variable: ULocalVariable): Boolean {
        val property = variable.sourcePsi as? KtProperty ?: return variable.isFinal
        return !property.isVar
    }

    private fun assignedName(target: UExpression): String? {
        val reference = when (val value = Literals.unwrap(target)) {
            is USimpleNameReferenceExpression -> value
            is UQualifiedReferenceExpression -> value.selector as? USimpleNameReferenceExpression
            else -> null
        } ?: return null
        return when (val resolved = reference.resolve()) {
            is PsiMethod -> Literals.propertyName(resolved.name)
            is PsiNamedElement -> resolved.name
            else -> reference.identifier
        }
    }

    private fun isSemanticsFunction(call: UCallExpression): Boolean {
        val method = call.resolve() ?: return false
        if (method.containingClass?.qualifiedName == SEMANTICS_RECEIVER) return true
        val receiverType = method.parameterList.parameters.firstOrNull()?.type?.canonicalText
        return receiverType == SEMANTICS_RECEIVER
    }
}
