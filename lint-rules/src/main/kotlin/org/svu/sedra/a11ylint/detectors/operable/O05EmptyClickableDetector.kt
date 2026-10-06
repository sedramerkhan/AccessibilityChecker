package org.svu.sedra.a11ylint.detectors.operable

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.svu.sedra.a11ylint.taxonomy.A11yIssues
import org.svu.sedra.a11ylint.taxonomy.Priority
import org.svu.sedra.a11ylint.util.Clickables
import org.svu.sedra.a11ylint.util.ComposeCalls
import org.svu.sedra.a11ylint.util.Literals

/**
 * O-05: a clickable element with nothing readable inside.
 *
 * Per the CLAUDE.md overlap policy (7.1), O-05 only reports a clickable that holds no Icon or
 * Image at all and no Text: an Icon or Image with a missing label inside a clickable is P-01's
 * defect, not this one. A clickable whose content calls a composable this rule cannot see into
 * is left alone, because that composable may well provide the name.
 */
class O05EmptyClickableDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler = object : UElementHandler() {
        override fun visitCallExpression(node: UCallExpression) {
            if (!Clickables.isClickableElement(context, node)) return
            // A clickable Icon, Image or Text is its own content: labelled is fine, unlabelled is P-01.
            if (ComposeCalls.isCall(node, Clickables.imageCalls)) return
            if (ComposeCalls.isCall(node, Clickables.textCalls)) return
            if (Clickables.hasAccessibleName(context, node)) return
            // The element is itself a composable this rule cannot see into, so it may well
            // render its own name (found on the development apps, see DEV_APP_RESULTS).
            if (emitsUnknownUi(node)) return

            val content = contentCalls(node)
            // An unlabelled Icon or Image inside the clickable is P-01 (overlap policy 7.1).
            if (content.any { ComposeCalls.isCall(it, Clickables.imageCalls) }) return
            if (content.any { emitsUnknownUi(it) }) return

            val name = ComposeCalls.name(node) ?: "element"
            context.report(
                ISSUE,
                node,
                context.getNameLocation(node),
                A11yIssues.message(
                    TAXONOMY_ID,
                    "Clickable $name has nothing to read: no text, no labelled icon and no " +
                        "contentDescription, so screen readers announce it without a name",
                ),
            )
        }
    }

    /** Every call inside the lambdas passed to [call], at any depth. */
    private fun contentCalls(call: UCallExpression): List<UCallExpression> {
        val calls = mutableListOf<UCallExpression>()
        val visitor = object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                calls += node
                return super.visitCallExpression(node)
            }
        }
        call.valueArguments
            .mapNotNull { Literals.unwrap(it) as? ULambdaExpression }
            .forEach { it.body.accept(visitor) }
        return calls
    }

    /**
     * True when [call] emits UI this rule cannot read: a `@Composable` function that returns
     * Unit and is not one of the composables whose content is read directly. A `@Composable`
     * that returns a value (`remember`, `stringResource`) emits nothing and does not count.
     */
    private fun emitsUnknownUi(call: UCallExpression): Boolean {
        if (ComposeCalls.isCall(call, READABLE_COMPOSABLES)) return false
        val method = call.resolve() ?: return false
        if (!method.hasAnnotation(ComposeCalls.COMPOSABLE)) return false
        return method.returnType?.canonicalText == "void"
    }

    companion object {
        const val TAXONOMY_ID = "O-05"

        /**
         * Composables whose content this rule can read, either because it walks their content
         * lambdas or because they have none. Anything else is a composable whose output is
         * unknown: it may render its own name, so neither it nor its content is judged here.
         * The Material components are included because their content lambdas are read like any
         * other, so an empty `IconButton` is still reported.
         */
        private val READABLE_COMPOSABLES = Clickables.layoutContainers +
            Clickables.textCalls +
            Clickables.imageCalls +
            Clickables.clickableComponents +
            Clickables.clickableContainers +
            setOf(
                "androidx.compose.foundation.layout.Spacer",
                "androidx.compose.foundation.Canvas",
            )

        @JvmField
        val ISSUE: Issue = A11yIssues.create(
            id = "ComposeEmptyClickable",
            briefDescription = "Clickable element with nothing to announce",
            explanation = """
                A clickable element with nothing readable inside gives screen readers no name to \
                announce. TalkBack says only "button", so the user hears that something can be \
                activated but never what it does. This relates to WCAG 2.2 success criterion \
                4.1.2 (Name, Role, Value). Add a visible `Text`, give an `Icon` a \
                `contentDescription`, or set `contentDescription` in `Modifier.semantics { }`.
            """,
            priority = Priority.MAJOR,
            detector = O05EmptyClickableDetector::class.java,
        )
    }
}
