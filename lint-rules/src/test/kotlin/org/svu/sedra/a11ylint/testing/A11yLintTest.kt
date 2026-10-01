package org.svu.sedra.a11ylint.testing

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import org.svu.sedra.a11ylint.stubs.composeStubs

/** Base class that supplies Compose stubs to detector tests. */
abstract class A11yLintTest : LintDetectorTest() {
    protected abstract val testDetector: Detector
    protected abstract val issue: Issue

    override fun getDetector(): Detector = testDetector

    override fun getIssues(): List<Issue> = listOf(issue)

    /** Runs lint with the shared Compose API stubs and the supplied source files. */
    protected fun lintWithCompose(vararg files: TestFile): TestLintResult =
        lint()
            .files(*(composeStubs + files))
            .issues(issue)
            .allowMissingSdk()
            .run()

    /** Creates a registry containing the test issue for fixture-level use. */
    protected fun testRegistry(): IssueRegistry = object : IssueRegistry() {
        override val issues: List<Issue> = listOf(issue)
        override val api: Int = CURRENT_API
        override val vendor: Vendor = Vendor("Compose A11y Lint Test")
    }
}
