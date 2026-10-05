package org.svu.sedra.a11ylint.detectors.understandable

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.svu.sedra.a11ylint.taxonomy.A11yIssues
import org.svu.sedra.a11ylint.taxonomy.Priority
import org.svu.sedra.a11ylint.util.ComposeCalls
import org.svu.sedra.a11ylint.util.Literals
import org.svu.sedra.a11ylint.util.ModifierChain

/**
 * U-03: a text field in its error state with no message saying what is wrong.
 *
 * Material already sets an `error(...)` semantic when `isError` is true, but only with its
 * generic default message ("Error"), checked in the Material3 1.4.0 and Material 1.10.4 sources
 * (see DECISIONS). A specific message has to come from `supportingText` or from `error(...)` in
 * the field's own semantics, so the rule reports an error state that has neither.
 */
class U03MissingSemanticErrorDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf("TextField", "OutlinedTextField")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!ComposeCalls.isCall(node, TEXT_FIELDS)) return
        val isError = ComposeCalls.argument(context, node, "isError") ?: return
        if (isFalseLiteral(isError)) return

        val supportingText = ComposeCalls.argument(context, node, "supportingText")
        if (supportingText != null && !Literals.isNullLiteral(supportingText)) return
        if ("error" in ModifierChain.semanticsAssignments(ModifierChain.modifierArgument(context, node))) return

        val name = ComposeCalls.name(node) ?: "TextField"
        context.report(
            ISSUE,
            node,
            context.getLocation(isError),
            A11yIssues.message(
                TAXONOMY_ID,
                "$name is in its error state but gives no message, so screen readers only " +
                    "announce the generic \"Error\" and the user is not told what is wrong",
            ),
        )
    }

    private fun isFalseLiteral(expression: UExpression?): Boolean =
        (Literals.unwrap(expression) as? ULiteralExpression)?.value == false

    companion object {
        const val TAXONOMY_ID = "U-03"

        private val TEXT_FIELDS = listOf(
            "androidx.compose.material3.TextField",
            "androidx.compose.material3.OutlinedTextField",
            "androidx.compose.material.TextField",
            "androidx.compose.material.OutlinedTextField",
        )

        @JvmField
        val ISSUE: Issue = A11yIssues.create(
            id = "ComposeMissingSemanticError",
            briefDescription = "Text field in an error state with no message",
            explanation = """
                A field marked `isError` with no message tells the user that something is wrong \
                but not what. Material only adds a generic "Error" announcement, so TalkBack \
                users hear that the field is invalid and never why, and sighted users see only a \
                coloured outline. This relates to WCAG 2.2 success criterion 3.3.1 (Error \
                Identification). Add `supportingText = { Text("Enter a valid email address") }`, \
                or set a specific message with `Modifier.semantics { error("...") }`.
            """,
            priority = Priority.MAJOR,
            detector = U03MissingSemanticErrorDetector::class.java,
        )
    }
}
