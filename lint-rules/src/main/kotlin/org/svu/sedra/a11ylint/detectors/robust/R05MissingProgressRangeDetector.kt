package org.svu.sedra.a11ylint.detectors.robust

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.UastBinaryOperator
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.svu.sedra.a11ylint.taxonomy.A11yIssues
import org.svu.sedra.a11ylint.taxonomy.Priority
import org.svu.sedra.a11ylint.util.ComposeCalls
import org.svu.sedra.a11ylint.util.ModifierChain

/**
 * R-05: a `@Composable` that draws its own progress indicator and sets no
 * `progressBarRangeInfo`.
 *
 * A hand-drawn bar or ring shows how far along something is by its shape alone. Nothing in the
 * drawing reaches accessibility services, so TalkBack announces no value at all and the user
 * cannot tell whether the task is at the start, halfway or nearly done. The Material indicators
 * set the range themselves, which is why a function that uses one is not reported.
 */
class R05MissingProgressRangeDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler = object : UElementHandler() {
        override fun visitMethod(node: UMethod) {
            if (!node.hasAnnotation(ComposeCalls.COMPOSABLE)) return
            val parameter = node.uastParameters.firstOrNull(::isProgressParameter) ?: return

            val calls = bodyCalls(node)
            if (calls.none { ComposeCalls.isCall(it, DRAWING) }) return
            // A Material indicator already carries the range, so the function is not hand-drawn
            // progress in the sense this rule means.
            if (calls.any { ComposeCalls.isCall(it, MATERIAL_INDICATORS) }) return
            if (setsProgressRange(node)) return

            context.report(
                ISSUE,
                node,
                context.getNameLocation(node),
                A11yIssues.message(
                    TAXONOMY_ID,
                    "${node.name} draws its own progress from ${parameter.name} but sets no " +
                        "progressBarRangeInfo, so screen readers announce no value",
                ),
            )
        }
    }

    /** A `Float` parameter whose name says it carries a progress value. */
    private fun isProgressParameter(parameter: UParameter): Boolean {
        if (parameter.type.canonicalText !in FLOAT_TYPES) return false
        val name = parameter.name.lowercase()
        return PROGRESS_NAMES.any { it in name }
    }

    private fun setsProgressRange(method: UMethod): Boolean {
        var found = false
        method.uastBody?.accept(object : AbstractUastVisitor() {
            override fun visitBinaryExpression(node: UBinaryExpression): Boolean {
                if (found) return true
                if (node.operator == UastBinaryOperator.ASSIGN &&
                    ModifierChain.assignedName(node.leftOperand) == PROGRESS_RANGE
                ) {
                    found = true
                }
                return found
            }
        })
        return found
    }

    /** Every call in the body of [method], at any depth. */
    private fun bodyCalls(method: UMethod): List<UCallExpression> {
        val calls = mutableListOf<UCallExpression>()
        method.uastBody?.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                calls += node
                return super.visitCallExpression(node)
            }
        })
        return calls
    }

    companion object {
        const val TAXONOMY_ID = "R-05"

        /** Kotlin `Float` is a JVM primitive; the boxed forms appear for a nullable `Float?`. */
        private val FLOAT_TYPES = setOf("float", "java.lang.Float", "kotlin.Float")

        /** CLAUDE.md 7.4: "named like progress, percent, fraction or value". */
        private val PROGRESS_NAMES = listOf("progress", "percent", "fraction", "value")

        /** Drawing the indicator by hand, rather than composing one. */
        private val DRAWING = setOf(
            "androidx.compose.foundation.Canvas",
            "androidx.compose.ui.draw.drawBehind",
        )

        private val MATERIAL_INDICATORS = setOf("androidx.compose.material3", "androidx.compose.material")
            .flatMap { listOf("$it.LinearProgressIndicator", "$it.CircularProgressIndicator") }
            .toSet()

        private const val PROGRESS_RANGE = "progressBarRangeInfo"

        @JvmField
        val ISSUE: Issue = A11yIssues.create(
            id = "ComposeMissingProgressRange",
            briefDescription = "Hand-drawn progress indicator has no progressBarRangeInfo",
            explanation = """
                A progress bar or ring drawn with `Canvas` or `drawBehind` shows how far along \
                the task is by its shape alone. None of that reaches accessibility services, so \
                TalkBack announces the element with no value: the user cannot tell whether a \
                download is at the start, halfway or nearly finished, and never hears it \
                change. This relates to WCAG 2.2 success criterion 4.1.2 (Name, Role, Value). \
                Add `Modifier.semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress, \
                0f..1f) }`, or use the Material `LinearProgressIndicator`, which sets it itself.
            """,
            priority = Priority.MINOR,
            detector = R05MissingProgressRangeDetector::class.java,
        )
    }
}
