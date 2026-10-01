package org.svu.sedra.a11ylint.util

import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor

/** Extracts modifier calls and semantics information from Compose modifier expressions. */
object ModifierChain {
    /** Returns modifier calls in receiver-to-selector order. */
    fun calls(expression: UExpression?): List<UCallExpression> {
        val result = mutableListOf<UCallExpression>()
        collectCalls(expression, result)
        return result
    }

    /** Returns true when a modifier chain contains a call with the supplied name. */
    fun hasModifier(expression: UExpression?, name: String): Boolean =
        calls(expression).any { it.methodName == name }

    /** Returns semantics and clearAndSetSemantics blocks in chain order. */
    fun semanticsBlocks(expression: UExpression?): List<UCallExpression> =
        calls(expression).filter {
            it.methodName == "semantics" || it.methodName == "clearAndSetSemantics"
        }

    /** Returns property and function names assigned inside semantics blocks. */
    fun semanticsAssignments(expression: UExpression?): Set<String> =
        semanticsBlocks(expression)
            .flatMap { block ->
                val lambda = block.valueArguments.filterIsInstance<ULambdaExpression>().firstOrNull()
                    ?: return@flatMap emptyList()
                val names = mutableSetOf<String>()
                lambda.accept(object : AbstractUastVisitor() {
                    override fun visitCallExpression(node: UCallExpression): Boolean {
                        node.methodName?.let(names::add)
                        return super.visitCallExpression(node)
                    }
                })
                names
            }
            .toSet()

    private fun collectCalls(expression: UExpression?, result: MutableList<UCallExpression>) {
        when (expression) {
            is UCallExpression -> {
                expression.receiver?.let { collectCalls(it, result) }
                result += expression
            }
            is UQualifiedReferenceExpression -> {
                collectCalls(expression.receiver, result)
                collectCalls(expression.selector, result)
            }
        }
    }
}
