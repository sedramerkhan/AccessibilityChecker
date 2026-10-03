package org.svu.sedra.a11ylint.testing

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.svu.sedra.a11ylint.util.ComposeCalls
import org.svu.sedra.a11ylint.util.Literals
import org.svu.sedra.a11ylint.util.ModifierChain
import org.svu.sedra.a11ylint.util.UiScope

/**
 * Test-only detector that runs the `util` helpers on real UAST and reports what they return.
 *
 * Test code calls `probe(label, kind, value)` (declared in [probeFile]). For every call the
 * detector applies the helper named by `kind` to `value` (or to the probe call itself) and
 * reports `"<label>: <result>"`. The report has a file-only location, so the expected output
 * is one short line per probe. Lint orders the lines by message, so labels are single letters.
 */
class UtilityProbeDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf("probe")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val label = Literals.stringLiteralValue(ComposeCalls.argument(context, node, "label")) ?: return
        val kind = Literals.stringLiteralValue(ComposeCalls.argument(context, node, "kind")) ?: return
        val value = Literals.unwrap(ComposeCalls.argument(context, node, "value"))
        val result = evaluate(context, node, kind, value)
        context.report(ISSUE, node, Location.create(context.file), "$label: $result")
    }

    private fun evaluate(context: JavaContext, probe: UCallExpression, kind: String, value: UExpression?): String {
        val argumentName = kind.substringAfter(':', "")
        return when (kind.substringBefore(':')) {
            "chain" -> ModifierChain.calls(value).map { ComposeCalls.name(it) }.toString()
            "has" -> ModifierChain.hasModifier(value, argumentName).toString()
            "semantics" -> ModifierChain.semanticsAssignments(value).sorted().toString()
            "clear" -> ModifierChain.hasClearAndSetSemantics(value).toString()
            "modifier" -> ModifierChain.calls(callOf(value)?.let { ModifierChain.modifierArgument(context, it) })
                .map { ComposeCalls.name(it) }
                .toString()
            "null" -> Literals.isNullLiteral(value).toString()
            "empty" -> Literals.isEmptyStringLiteral(value).toString()
            "string" -> Literals.stringLiteralValue(value)?.let { "\"$it\"" } ?: "null"
            "resource" -> Literals.isStringResourceCall(value).toString()
            "dp" -> Literals.dpValue(value).toString()
            "sp" -> Literals.spValue(value).toString()
            "scope" -> UiScope.isInUiScope(probe).toString()
            "preview" -> UiScope.isPreview(probe).toString()
            "composable" -> (callOf(value)?.let(ComposeCalls::isComposableCall) ?: false).toString()
            "isCall" -> (callOf(value)?.let { ComposeCalls.isCall(it, argumentName) } ?: false).toString()
            "arg" -> describe(callOf(value)?.let { ComposeCalls.argument(context, it, argumentName) })
            "content" -> callOf(value)
                ?.let { ComposeCalls.contentLambda(context, it) }
                ?.let { "lambda with ${firstCallName(it)}" }
                ?: "none"
            else -> "unknown kind $kind"
        }
    }

    /** Returns the call itself, or the selector call of `a.b.call()`. */
    private fun callOf(value: UExpression?): UCallExpression? =
        when (value) {
            is UCallExpression -> value
            is UQualifiedReferenceExpression -> Literals.unwrap(value.selector) as? UCallExpression
            else -> null
        }

    private fun describe(argument: UExpression?): String {
        val value = Literals.unwrap(argument)
        return when {
            argument == null -> "none"
            Literals.isNullLiteral(value) -> "null"
            Literals.stringLiteralValue(value) != null -> "\"${Literals.stringLiteralValue(value)}\""
            value is ULambdaExpression -> "lambda"
            else -> "expression"
        }
    }

    private fun firstCallName(lambda: ULambdaExpression): String? {
        var name: String? = null
        lambda.body.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                if (name == null) name = ComposeCalls.name(node)
                return super.visitCallExpression(node)
            }
        })
        return name
    }

    companion object {
        val ISSUE: Issue = Issue.create(
            id = "UtilityProbe",
            briefDescription = "Utility probe",
            explanation = "Test-only issue that reports the result of the util helpers.",
            category = Category.A11Y,
            priority = 1,
            severity = Severity.WARNING,
            implementation = Implementation(UtilityProbeDetector::class.java, Scope.JAVA_FILE_SCOPE),
        )

        /** Declares the `probe` function that the detector looks for. */
        val probeFile: TestFile = kotlin(
            "src/test/pkg/Probe.kt",
            """
            package test.pkg

            fun probe(label: String, kind: String, value: Any? = null) {}
            """,
        ).indented()
    }
}

/** Base class for utility tests that use [UtilityProbeDetector]. */
abstract class UtilityProbeTest : A11yLintTest() {
    override fun getDetector(): Detector = UtilityProbeDetector()

    override fun getIssues(): List<Issue> = listOf(UtilityProbeDetector.ISSUE)

    /**
     * Runs the probes in [source] and asserts the reported lines, one `"label: result"` per
     * line. [extraFiles] are added to the project, for example a look-alike API in another
     * package.
     */
    protected fun expectProbes(source: String, vararg results: String, extraFiles: List<TestFile> = emptyList()) {
        val file = "src/test/pkg/Probes.kt"
        val expected = results.joinToString("\n") { "$file: Warning: $it [UtilityProbe]" } +
            "\n0 errors, ${results.size} warnings"
        expectWarnings(
            expected,
            UtilityProbeDetector.probeFile,
            kotlin(file, source).indented(),
            *extraFiles.toTypedArray(),
        )
    }
}
