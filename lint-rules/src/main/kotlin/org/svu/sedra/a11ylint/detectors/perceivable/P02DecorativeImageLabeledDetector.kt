package org.svu.sedra.a11ylint.detectors.perceivable

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UReturnExpression
import org.svu.sedra.a11ylint.taxonomy.A11yIssues
import org.svu.sedra.a11ylint.taxonomy.Priority
import org.svu.sedra.a11ylint.util.Clickables
import org.svu.sedra.a11ylint.util.ComposeCalls
import org.svu.sedra.a11ylint.util.Literals

/** Reports a possible decorative image whose literal label repeats nearby visible text. */
class P02DecorativeImageLabeledDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf("Icon", "Image")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!ComposeCalls.isCall(node, Clickables.imageCalls)) return
        if (Clickables.isClickableElement(context, node)) return
        val description = Literals.stringLiteralValue(
            ComposeCalls.argument(context, node, "contentDescription"),
        ) ?: return
        if (description.isBlank()) return
        val normalizedDescription = description.trim().lowercase()
        val siblingText = directSiblingText(context, node).firstOrNull { text ->
            val normalizedText = text.trim().lowercase()
            normalizedText == normalizedDescription ||
                normalizedText.contains(normalizedDescription) ||
                normalizedDescription.contains(normalizedText)
        } ?: return
        context.report(
            ISSUE,
            node,
            context.getLocation(ComposeCalls.argument(context, node, "contentDescription") ?: node),
            A11yIssues.message(
                TAXONOMY_ID,
                "Possible decorative image label repeats visible text \"$siblingText\"",
            ),
        )
    }

    private fun directSiblingText(context: JavaContext, image: UCallExpression): List<String> {
        val lambda = nearestLambda(image) ?: return emptyList()
        val body = lambda.body as? UBlockExpression ?: return emptyList()
        return body.expressions
            .asSequence()
            .mapNotNull { expression ->
                val call = when (expression) {
                    is UCallExpression -> expression
                    is UReturnExpression -> expression.returnExpression as? UCallExpression
                    else -> null
                } ?: return@mapNotNull null
                if (!isDirectSibling(call, image)) return@mapNotNull null
                if (!ComposeCalls.isCall(call, Clickables.textCalls)) return@mapNotNull null
                Literals.stringLiteralValue(ComposeCalls.argument(context, call, "text"))
            }
            .toList()
    }

    private fun isDirectSibling(candidate: UCallExpression, image: UCallExpression): Boolean {
        val imageLambda = nearestLambda(image) ?: return false
        return nearestLambda(candidate) == imageLambda
    }

    private fun nearestLambda(node: UElement): ULambdaExpression? {
        var current = node.uastParent
        while (current != null) {
            if (current is ULambdaExpression) return current
            current = current.uastParent
        }
        return null
    }

    companion object {
        const val TAXONOMY_ID = "P-02"

        @JvmField
        val ISSUE: Issue = A11yIssues.create(
            id = "ComposeDecorativeImageLabeled",
            briefDescription = "Decorative image repeats visible text",
            explanation = "A decorative image has a literal accessibility label that repeats nearby visible text. This may cause screen readers to announce the same information twice and relates to WCAG 2.2 success criterion 1.1.1. Remove the redundant label or make the image meaningful and give it a distinct description.",
            priority = Priority.MAJOR,
            detector = P02DecorativeImageLabeledDetector::class.java,
        )
    }
}
