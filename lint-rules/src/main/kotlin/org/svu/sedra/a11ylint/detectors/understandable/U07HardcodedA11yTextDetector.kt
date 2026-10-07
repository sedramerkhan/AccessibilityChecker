package org.svu.sedra.a11ylint.detectors.understandable

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.UastBinaryOperator
import org.svu.sedra.a11ylint.taxonomy.A11yIssues
import org.svu.sedra.a11ylint.taxonomy.Priority
import org.svu.sedra.a11ylint.util.ComposeCalls
import org.svu.sedra.a11ylint.util.Literals
import org.svu.sedra.a11ylint.util.ModifierChain
import org.svu.sedra.a11ylint.util.UiScope

/**
 * U-07: accessibility text written as a string literal instead of a string resource.
 *
 * Visible text is translated because it goes through `stringResource(...)`, but the text that
 * only a screen reader hears is easy to forget. A hardcoded `contentDescription` stays English in
 * every language the app ships.
 *
 * Two shapes are read: an argument of a call (`Icon(contentDescription = "Delete")`,
 * `Modifier.clickable(onClickLabel = "Open")`, `CustomAccessibilityAction("Share") { }`), and a
 * property set inside a semantics block (`semantics { contentDescription = "Delete" }`). Previews
 * and test sources are skipped, as CLAUDE.md 7.4 asks.
 */
class U07HardcodedA11yTextDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(UCallExpression::class.java, UBinaryExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler = object : UElementHandler() {
        override fun visitCallExpression(node: UCallExpression) {
            // Cheap first: most calls in a file carry no string literal and cannot be reported,
            // so this avoids resolving every call in the file twice.
            if (node.valueArguments.none { Literals.stringLiteralValue(it) != null }) return
            if (skip(context, node)) return
            when {
                ComposeCalls.isCall(node, SEMANTICS_ERROR) ->
                    report(context, node.valueArguments.firstOrNull(), "error")
                ComposeCalls.isCall(node, CUSTOM_ACTION) ->
                    report(context, ComposeCalls.argument(context, node, "label"), "customActions")
                else -> ARGUMENT_NAMES.forEach { name ->
                    report(context, ComposeCalls.argument(context, node, name), name)
                }
            }
        }

        override fun visitBinaryExpression(node: UBinaryExpression) {
            if (node.operator != UastBinaryOperator.ASSIGN) return
            if (skip(context, node)) return
            val name = ModifierChain.assignedName(node.leftOperand) ?: return
            if (name !in PROPERTY_NAMES) return
            if (!isSemanticsProperty(node.leftOperand)) return
            report(context, node.rightOperand, name)
        }
    }

    private fun skip(context: JavaContext, node: UElement): Boolean =
        context.isTestSource || UiScope.isPreview(node)

    /**
     * Confirms the assignment really sets a semantics property, so an unrelated local named
     * `contentDescription` is not reported.
     */
    private fun isSemanticsProperty(target: UExpression): Boolean {
        val reference = when (val value = Literals.unwrap(target)) {
            is USimpleNameReferenceExpression -> value
            is UQualifiedReferenceExpression -> value.selector as? USimpleNameReferenceExpression
            else -> null
        } ?: return false
        val method = reference.resolve() as? PsiMethod ?: return false
        return ModifierChain.isSemanticsDeclaration(method)
    }

    private fun report(context: JavaContext, argument: UExpression?, name: String) {
        val expression = Literals.unwrap(argument) ?: return
        val text = Literals.stringLiteralValue(expression) ?: return
        if (text.isEmpty()) return
        context.report(
            ISSUE,
            expression,
            context.getLocation(expression),
            A11yIssues.message(
                TAXONOMY_ID,
                "$name is set to the hardcoded string \"$text\", so this text is never " +
                    "translated and screen reader users of other languages hear English",
            ),
        )
    }

    companion object {
        const val TAXONOMY_ID = "U-07"

        private const val SEMANTICS = "androidx.compose.ui.semantics"

        private const val SEMANTICS_ERROR = "$SEMANTICS.error"
        private const val CUSTOM_ACTION = "$SEMANTICS.CustomAccessibilityAction"

        /** Accessibility text passed as a call argument. */
        private val ARGUMENT_NAMES = listOf("contentDescription", "onClickLabel")

        /** Accessibility text assigned inside a semantics block. */
        private val PROPERTY_NAMES = setOf("contentDescription", "stateDescription", "paneTitle")

        @JvmField
        val ISSUE: Issue = A11yIssues.create(
            id = "ComposeHardcodedA11yText",
            briefDescription = "Accessibility text is a hardcoded string",
            explanation = """
                Text that only a screen reader hears is easy to forget when an app is \
                translated, because nobody sees it on screen. A hardcoded `contentDescription`, \
                `onClickLabel`, `stateDescription`, `paneTitle`, error message or custom action \
                label stays in the original language, so users of every other language hear \
                English in the middle of their own interface. This is a localization defect \
                rather than a WCAG failure. Move the text to `strings.xml` and read it with \
                `stringResource(R.string.delete_draft)`, exactly as visible text is handled.
            """,
            priority = Priority.MINOR,
            detector = U07HardcodedA11yTextDetector::class.java,
        )
    }
}
