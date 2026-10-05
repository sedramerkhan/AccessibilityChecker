package org.svu.sedra.a11ylint.detectors.understandable

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.svu.sedra.a11ylint.taxonomy.A11yIssues
import org.svu.sedra.a11ylint.taxonomy.Priority
import org.svu.sedra.a11ylint.util.Clickables
import org.svu.sedra.a11ylint.util.ComposeCalls
import org.svu.sedra.a11ylint.util.Literals
import org.svu.sedra.a11ylint.util.ModifierChain

/**
 * U-04 (candidate): a `Button`, `TextButton` or `OutlinedButton` whose only text is one of the
 * vague labels in [VAGUE_LABELS].
 *
 * The button must hold exactly one Text, and its text must be written as a literal. A second
 * Text, or a text that comes from a variable or a string resource, means the label cannot be
 * judged here and the button is left alone.
 */
class U04VagueButtonLabelDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf("Button", "TextButton", "OutlinedButton")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!ComposeCalls.isCall(node, BUTTONS)) return
        if (Clickables.hasSemanticsLabel(ModifierChain.modifierArgument(context, node))) return

        val text = textCalls(node).singleOrNull() ?: return
        val textArgument = ComposeCalls.argument(context, text, "text") ?: return
        val label = Literals.stringLiteralValue(textArgument) ?: return
        if (label.trim().lowercase() !in VAGUE_LABELS) return

        context.report(
            ISSUE,
            text,
            context.getLocation(textArgument),
            A11yIssues.message(
                TAXONOMY_ID,
                "Possible vague button label: \"${label.trim()}\" does not say what the button does",
            ),
        )
    }

    /** The Text calls inside the lambdas passed to [call], at any depth. */
    private fun textCalls(call: UCallExpression): List<UCallExpression> {
        val texts = mutableListOf<UCallExpression>()
        val visitor = object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                if (ComposeCalls.isCall(node, Clickables.textCalls)) texts += node
                return super.visitCallExpression(node)
            }
        }
        call.valueArguments
            .mapNotNull { Literals.unwrap(it) as? ULambdaExpression }
            .forEach { it.body.accept(visitor) }
        return texts
    }

    companion object {
        const val TAXONOMY_ID = "U-04"

        private val BUTTONS = listOf("androidx.compose.material3", "androidx.compose.material")
            .flatMap { pkg -> listOf("Button", "TextButton", "OutlinedButton").map { "$pkg.$it" } }

        /**
         * The vague labels from CLAUDE.md 7.3, compared case-insensitively after trimming.
         * Each says that something happens but not what, so it is useless out of context.
         */
        val VAGUE_LABELS = setOf(
            "ok", "click", "click here", "here", "tap", "tap here",
            "go", "submit", "done", "more", "yes", "no",
        )

        @JvmField
        val ISSUE: Issue = A11yIssues.create(
            id = "ComposeVagueButtonLabel",
            briefDescription = "Button label does not say what the button does",
            explanation = """
                A button labelled only "OK" or "Submit" says that something will happen but not \
                what. Screen reader users often move from control to control and hear the label \
                on its own, with none of the surrounding text a sighted user reads first, so a \
                vague label leaves them guessing. This relates to WCAG 2.2 success criterion \
                2.4.6 (Headings and Labels). Name the action in the label, for example "Delete \
                draft" instead of "OK".
            """,
            priority = Priority.MAJOR,
            detector = U04VagueButtonLabelDetector::class.java,
        )
    }
}
