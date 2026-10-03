package org.svu.sedra.a11ylint.testing

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintResult
import org.svu.sedra.a11ylint.stubs.composeStubs

/**
 * Base class for detector tests. Subclasses override `getDetector()` and `getIssues()`.
 *
 * Every run adds the Compose stubs automatically. Compilation errors are not allowed: if a
 * test file or a stub does not compile, calls do not resolve and a detector would silently
 * match nothing, so the test must fail instead. Lint's default test modes stay on.
 */
abstract class A11yLintTest : LintDetectorTest() {
    /** Runs lint on [files] together with the Compose stubs. */
    protected fun lintWithCompose(vararg files: TestFile): TestLintResult =
        lint()
            .files(*composeStubs, *files)
            .allowMissingSdk()
            .run()

    /** Asserts that lint reports exactly [expected] (Lint's text output format) for [files]. */
    protected fun expectWarnings(expected: String, vararg files: TestFile) {
        lintWithCompose(*files).expect(expected)
    }

    /** Asserts that lint reports nothing for [files]. */
    protected fun expectClean(vararg files: TestFile) {
        lintWithCompose(*files).expectClean()
    }
}
