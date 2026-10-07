package org.svu.sedra.a11ylint.detectors.understandable

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.testing.A11yLintTest

class U07HardcodedA11yTextDetectorTest : A11yLintTest() {
    override fun getDetector(): Detector = U07HardcodedA11yTextDetector()
    override fun getIssues(): List<Issue> = listOf(U07HardcodedA11yTextDetector.ISSUE)

    private fun screen(body: String) = kotlin(
        "src/test/pkg/Screen.kt",
        """
package test.pkg

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

$body
        """,
    )

    // Positive tests: must be flagged.

    fun testContentDescriptionArgument() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:25: Warning: [U-07] contentDescription is set to the hardcoded string "Delete draft", so this text is never translated and screen reader users of other languages hear English [ComposeHardcodedA11yText]
                        Icon(painterResource(1), contentDescription = "Delete draft")
                                                                      ~~~~~~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen() {
                    Box {
                        Icon(painterResource(1), contentDescription = "Delete draft")
                    }
                }
            """),
        )
    }

    fun testOnClickLabelArgument() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:24: Warning: [U-07] onClickLabel is set to the hardcoded string "Open details", so this text is never translated and screen reader users of other languages hear English [ComposeHardcodedA11yText]
                    Row(modifier = Modifier.clickable(onClickLabel = "Open details", role = Role.Button) { }) {
                                                                     ~~~~~~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen() {
                    Row(modifier = Modifier.clickable(onClickLabel = "Open details", role = Role.Button) { }) {
                        Text("Shipping forecast")
                    }
                }
            """),
        )
    }

    fun testSemanticsProperties() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:26: Warning: [U-07] contentDescription is set to the hardcoded string "Volume", so this text is never translated and screen reader users of other languages hear English [ComposeHardcodedA11yText]
                            contentDescription = "Volume"
                                                 ~~~~~~~~
src/test/pkg/Screen.kt:27: Warning: [U-07] stateDescription is set to the hardcoded string "Muted", so this text is never translated and screen reader users of other languages hear English [ComposeHardcodedA11yText]
                            stateDescription = "Muted"
                                               ~~~~~~~
src/test/pkg/Screen.kt:28: Warning: [U-07] paneTitle is set to the hardcoded string "Settings", so this text is never translated and screen reader users of other languages hear English [ComposeHardcodedA11yText]
                            paneTitle = "Settings"
                                        ~~~~~~~~~~
0 errors, 3 warnings
            """,
            screen("""
                @Composable fun Screen() {
                    Box(
                        modifier = Modifier.semantics {
                            contentDescription = "Volume"
                            stateDescription = "Muted"
                            paneTitle = "Settings"
                        }
                    )
                }
            """),
        )
    }

    fun testSemanticErrorMessage() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:26: Warning: [U-07] error is set to the hardcoded string "Enter an email address", so this text is never translated and screen reader users of other languages hear English [ComposeHardcodedA11yText]
                            error("Enter an email address")
                                  ~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen() {
                    Box(
                        modifier = Modifier.semantics {
                            error("Enter an email address")
                        }
                    )
                }
            """),
        )
    }

    fun testCustomActionLabel() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:26: Warning: [U-07] customActions is set to the hardcoded string "Share", so this text is never translated and screen reader users of other languages hear English [ComposeHardcodedA11yText]
                            customActions = listOf(CustomAccessibilityAction("Share") { true })
                                                                             ~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen() {
                    Box(
                        modifier = Modifier.semantics {
                            customActions = listOf(CustomAccessibilityAction("Share") { true })
                        }
                    )
                }
            """),
        )
    }

    // Negative tests: must not be flagged.

    fun testStringResourceIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Box(
                modifier = Modifier
                    .clickable(onClickLabel = stringResource(1), role = Role.Button) { }
                    .semantics { contentDescription = stringResource(2) }
            ) {
                Icon(painterResource(1), contentDescription = stringResource(3))
            }
        }
    """))

    // Visible text is translated through the same resources, and is not this rule's business.
    fun testVisibleTextIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Row {
                Text("Shipping forecast")
            }
        }
    """))

    // A decorative image passes null, which is not text at all.
    fun testNullAndEmptyContentDescriptionAreClean() = expectClean(screen("""
        @Composable fun Screen() {
            Row {
                Icon(painterResource(1), contentDescription = null)
                Icon(painterResource(2), contentDescription = "")
            }
        }
    """))

    // An unrelated local with the same name is not a semantics property.
    fun testUnrelatedLocalNamedLikeAPropertyIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            var contentDescription = "not a semantics property"
            contentDescription = "still not one"
            Text(contentDescription)
        }
    """))

    fun testPreviewIsClean() = expectClean(screen("""
        @androidx.compose.ui.tooling.preview.Preview
        @Composable fun ScreenPreview() {
            Icon(painterResource(1), contentDescription = "Delete draft")
        }
    """))
}
