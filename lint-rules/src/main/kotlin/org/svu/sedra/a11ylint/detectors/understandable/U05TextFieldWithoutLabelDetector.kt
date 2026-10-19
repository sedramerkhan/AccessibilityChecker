package org.svu.sedra.a11ylint.detectors.understandable

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.svu.sedra.a11ylint.taxonomy.A11yIssues
import org.svu.sedra.a11ylint.taxonomy.Priority
import org.svu.sedra.a11ylint.util.ComposeCalls
import org.svu.sedra.a11ylint.util.Literals
import org.svu.sedra.a11ylint.util.ModifierChain

/**
 * U-05: a Material `TextField` or `OutlinedTextField` without a `label`.
 *
 * A placeholder does not count, because it disappears as soon as the user starts typing.
 *
 * Two levels. A field with no name at all is an error: screen readers only say "edit box". A
 * field that has a `contentDescription` in its semantics has a name for screen readers but still
 * no visible label (WCAG 3.3.2), so it is reported as a warning.
 */
class U05TextFieldWithoutLabelDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf("TextField", "OutlinedTextField")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!ComposeCalls.isCall(node, TEXT_FIELDS)) return
        val label = ComposeCalls.argument(context, node, "label")
        if (label != null && !Literals.isNullLiteral(label)) return

        val hasPlaceholder = ComposeCalls.argument(context, node, "placeholder")
            ?.let { !Literals.isNullLiteral(it) } == true
        val placeholderNote = if (hasPlaceholder) " (the placeholder disappears when the user types)" else ""
        val name = ComposeCalls.name(node) ?: "TextField"
        val hasScreenReaderName = CONTENT_DESCRIPTION in
            ModifierChain.semanticsAssignments(ModifierChain.modifierArgument(context, node))
        val incident = Incident(
            ISSUE,
            node,
            context.getNameLocation(node),
            A11yIssues.message(
                TAXONOMY_ID,
                if (hasScreenReaderName) {
                    "$name has a contentDescription but no visible label$placeholderNote, so users " +
                        "who do not use a screen reader are not told what to enter"
                } else {
                    "$name has no label$placeholderNote, so screen reader users are not told what to enter"
                },
            ),
        )
        // The issue's default severity is the case with no name at all; a field that screen
        // readers can name only lacks the visible label, which is lowered to a warning.
        if (hasScreenReaderName) incident.overrideSeverity(Severity.WARNING)
        context.report(incident)
    }

    companion object {
        const val TAXONOMY_ID = "U-05"

        private const val CONTENT_DESCRIPTION = "contentDescription"

        private val TEXT_FIELDS = listOf(
            "androidx.compose.material3.TextField",
            "androidx.compose.material3.OutlinedTextField",
            "androidx.compose.material.TextField",
            "androidx.compose.material.OutlinedTextField",
        )

        @JvmField
        val ISSUE: Issue = A11yIssues.create(
            id = "ComposeTextFieldWithoutLabel",
            briefDescription = "Text field without a label",
            explanation = """
                Every text field needs a label that says what to enter. Without one, TalkBack \
                only says "edit box", and a placeholder does not help because it disappears as \
                soon as the user types, which also hurts users with memory or cognitive \
                impairments. This relates to WCAG 2.2 success criteria 1.3.1 (Info and \
                Relationships) and 3.3.2 (Labels or Instructions). A field with no name at all is \
                an error; a field named only through `contentDescription` in its semantics is a \
                warning, because screen readers can name it but nothing on screen says what to \
                enter. Pass a label, for example `label = { Text("Email address") }`.
            """,
            priority = Priority.CRITICAL,
            detector = U05TextFieldWithoutLabelDetector::class.java,
        )
    }
}
