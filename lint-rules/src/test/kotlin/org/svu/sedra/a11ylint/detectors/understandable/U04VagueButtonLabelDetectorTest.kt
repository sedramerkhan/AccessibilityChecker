package org.svu.sedra.a11ylint.detectors.understandable

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.testing.A11yLintTest

class U04VagueButtonLabelDetectorTest : A11yLintTest() {
    override fun getDetector(): Detector = U04VagueButtonLabelDetector()
    override fun getIssues(): List<Issue> = listOf(U04VagueButtonLabelDetector.ISSUE)

    private fun screen(body: String) = kotlin(
        "src/test/pkg/Screen.kt",
        """
package test.pkg

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

$body
        """,
    )

    // Positive tests: must be flagged.

    fun testOkButton() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:20: Warning: [U-04] Possible vague button label: "OK" does not say what the button does [ComposeVagueButtonLabel]
                        Text("OK")
                             ~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onAction: () -> Unit) {
                    Button(onClick = onAction) {
                        Text("OK")
                    }
                }
            """),
        )
    }

    fun testTextButtonClickHere() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:20: Warning: [U-04] Possible vague button label: "Click here" does not say what the button does [ComposeVagueButtonLabel]
                        Text("Click here")
                             ~~~~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onAction: () -> Unit) {
                    TextButton(onClick = onAction) {
                        Text("Click here")
                    }
                }
            """),
        )
    }

    fun testOutlinedButtonSubmit() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:20: Warning: [U-04] Possible vague button label: "Submit" does not say what the button does [ComposeVagueButtonLabel]
                        Text("Submit")
                             ~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onAction: () -> Unit) {
                    OutlinedButton(onClick = onAction) {
                        Text("Submit")
                    }
                }
            """),
        )
    }

    fun testLabelIsTrimmedAndCaseInsensitive() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:20: Warning: [U-04] Possible vague button label: "DONE" does not say what the button does [ComposeVagueButtonLabel]
                        Text("  DONE  ")
                             ~~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onAction: () -> Unit) {
                    Button(onClick = onAction) {
                        Text("  DONE  ")
                    }
                }
            """),
        )
    }

    // Negative tests: must not be flagged.

    fun testDescriptiveLabelIsClean() = expectClean(screen("""
        @Composable fun Screen(onAction: () -> Unit) {
            Button(onClick = onAction) {
                Text("Delete draft")
            }
        }
    """))

    // "ok" is only vague on its own; as part of a longer label it is fine.
    fun testVagueWordInsideALongerLabelIsClean() = expectClean(screen("""
        @Composable fun Screen(onAction: () -> Unit) {
            Button(onClick = onAction) {
                Text("OK, delete the draft")
            }
        }
    """))

    fun testSemanticsOverrideIsClean() = expectClean(screen("""
        @Composable fun Screen(onAction: () -> Unit) {
            Button(
                onClick = onAction,
                modifier = Modifier.semantics { contentDescription = "Delete draft" }
            ) {
                Text("OK")
            }
        }
    """))

    fun testTextFromResourceIsClean() = expectClean(screen("""
        @Composable fun Screen(onAction: () -> Unit) {
            Button(onClick = onAction) {
                Text(stringResource(1))
            }
        }
    """))

    fun testTwoTextsIsClean() = expectClean(screen("""
        @Composable fun Screen(onAction: () -> Unit) {
            Button(onClick = onAction) {
                Text("OK")
                Text("Delete the draft")
            }
        }
    """))

    fun testNoTextIsClean() = expectClean(screen("""
        @Composable fun Screen(onAction: () -> Unit) {
            Button(onClick = onAction) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete draft")
            }
        }
    """))
}
