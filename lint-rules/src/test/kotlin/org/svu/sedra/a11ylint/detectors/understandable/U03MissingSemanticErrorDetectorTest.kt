package org.svu.sedra.a11ylint.detectors.understandable

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.testing.A11yLintTest

class U03MissingSemanticErrorDetectorTest : A11yLintTest() {
    override fun getDetector(): Detector = U03MissingSemanticErrorDetector()
    override fun getIssues(): List<Issue> = listOf(U03MissingSemanticErrorDetector.ISSUE)

    private fun screen(body: String) = kotlin(
        "src/test/pkg/Screen.kt",
        """
package test.pkg

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics

$body
        """,
    )

    // Positive tests: must be flagged.

    fun testErrorStateWithNoMessage() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:18: Warning: [U-03] TextField is in its error state but gives no message, so screen readers only announce the generic "Error" and the user is not told what is wrong [ComposeMissingSemanticError]
                        isError = true,
                                  ~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(email: String, onChange: (String) -> Unit) {
                    TextField(
                        value = email,
                        onValueChange = onChange,
                        label = { Text("Email") },
                        isError = true,
                    )
                }
            """),
        )
    }

    fun testOutlinedFieldWithVariableErrorState() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:18: Warning: [U-03] OutlinedTextField is in its error state but gives no message, so screen readers only announce the generic "Error" and the user is not told what is wrong [ComposeMissingSemanticError]
                        isError = hasError,
                                  ~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(email: String, onChange: (String) -> Unit, hasError: Boolean) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = onChange,
                        label = { Text("Email") },
                        isError = hasError,
                    )
                }
            """),
        )
    }

    fun testExplicitNullSupportingText() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:19: Warning: [U-03] TextField is in its error state but gives no message, so screen readers only announce the generic "Error" and the user is not told what is wrong [ComposeMissingSemanticError]
                        isError = true,
                                  ~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(email: String, onChange: (String) -> Unit) {
                    TextField(
                        value = email,
                        onValueChange = onChange,
                        label = { Text("Email") },
                        supportingText = null,
                        isError = true,
                    )
                }
            """),
        )
    }

    fun testErrorStateFromAnExpression() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:18: Warning: [U-03] TextField is in its error state but gives no message, so screen readers only announce the generic "Error" and the user is not told what is wrong [ComposeMissingSemanticError]
                        isError = email.isEmpty(),
                                  ~~~~~~~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(email: String, onChange: (String) -> Unit) {
                    TextField(
                        value = email,
                        onValueChange = onChange,
                        label = { Text("Email") },
                        isError = email.isEmpty(),
                    )
                }
            """),
        )
    }

    // Negative tests: must not be flagged.

    fun testSupportingTextIsClean() = expectClean(screen("""
        @Composable fun Screen(email: String, onChange: (String) -> Unit) {
            TextField(
                value = email,
                onValueChange = onChange,
                label = { Text("Email") },
                supportingText = { Text("Enter a valid email address") },
                isError = true,
            )
        }
    """))

    fun testSemanticsErrorIsClean() = expectClean(screen("""
        @Composable fun Screen(email: String, onChange: (String) -> Unit) {
            TextField(
                value = email,
                onValueChange = onChange,
                modifier = Modifier.semantics { error("Enter a valid email address") },
                label = { Text("Email") },
                isError = true,
            )
        }
    """))

    fun testErrorFalseIsClean() = expectClean(screen("""
        @Composable fun Screen(email: String, onChange: (String) -> Unit) {
            TextField(
                value = email,
                onValueChange = onChange,
                label = { Text("Email") },
                isError = false,
            )
        }
    """))

    fun testNoErrorArgumentIsClean() = expectClean(screen("""
        @Composable fun Screen(email: String, onChange: (String) -> Unit) {
            TextField(
                value = email,
                onValueChange = onChange,
                label = { Text("Email") },
            )
        }
    """))

    fun testOutlinedFieldWithSupportingTextIsClean() = expectClean(screen("""
        @Composable fun Screen(email: String, onChange: (String) -> Unit, hasError: Boolean) {
            OutlinedTextField(
                value = email,
                onValueChange = onChange,
                label = { Text("Email") },
                supportingText = { Text("Enter a valid email address") },
                isError = hasError,
            )
        }
    """))
}
