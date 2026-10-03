package org.svu.sedra.a11ylint.detectors.understandable

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.testing.A11yLintTest

class U01MissingHeadingDetectorTest : A11yLintTest() {
    override fun getDetector(): Detector = U01MissingHeadingDetector()

    override fun getIssues(): List<Issue> = listOf(U01MissingHeadingDetector.ISSUE)

    /** A screen file with the imports every test needs. */
    private fun screen(body: String): TestFile = kotlin(
        "src/test/pkg/Screen.kt",
        """
package test.pkg

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp


""" + body.trimIndent(),
    )

    private fun message(reason: String) =
        "[U-01] Possible heading: this Text is styled like a heading ($reason) but has no heading() " +
            "semantics, so screen reader users cannot jump to it [ComposeMissingHeading]"

    // Positive tests: must be flagged.

    fun testHeadlineStyle() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:21: Error: ${message("headlineSmall")}
                Text("Settings", style = MaterialTheme.typography.headlineSmall)
                ~~~~
            1 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun Header() {
                    Text("Settings", style = MaterialTheme.typography.headlineSmall)
                }
                """,
            ),
        )
    }

    fun testDisplayAndTitleLargeStylesWithCopy() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:22: Error: ${message("displayMedium")}
                    Text(text = "Welcome", style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Normal))
                    ~~~~
            src/test/pkg/Screen.kt:23: Error: ${message("titleLarge")}
                    Text(style = MaterialTheme.typography.titleLarge, text = "Recent")
                    ~~~~
            2 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun Header() {
                    Column {
                        Text(text = "Welcome", style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Normal))
                        Text(style = MaterialTheme.typography.titleLarge, text = "Recent")
                    }
                }
                """,
            ),
        )
    }

    fun testLargeBoldFontSize() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:21: Error: ${message("24sp bold")}
                Text("Orders", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                ~~~~
            1 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun Header() {
                    Text("Orders", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                """,
            ),
        )
    }

    fun testLargeBoldTextStyle() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:21: Error: ${message("20sp bold")}
                Text(
                ~~~~
            1 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun Header() {
                    Text(
                        text = "Profile",
                        style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold),
                    )
                }
                """,
            ),
        )
    }

    fun testMaterial2HeadingStyleAndHeadingOnParentOnly() {
        // heading() on the parent Column does not make the Text itself a heading.
        expectWarnings(
            """
            src/test/pkg/Screen.kt:22: Error: ${message("h3")}
                    androidx.compose.material.Text("Old style", style = androidx.compose.material.MaterialTheme.typography.h3)
                                              ~~~~
            1 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun Header() {
                    Column(Modifier.semantics { heading() }) {
                        androidx.compose.material.Text("Old style", style = androidx.compose.material.MaterialTheme.typography.h3)
                    }
                }
                """,
            ),
        )
    }

    // Negative tests: must not be flagged.

    fun testTextWithHeadingSemantics() {
        expectClean(
            screen(
                """
                @Composable
                fun Header() {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                """,
            ),
        )
    }

    fun testBodyStylesAndSmallOrRegularText() {
        expectClean(
            screen(
                """
                @Composable
                fun Body() {
                    Column {
                        Text("Body", style = MaterialTheme.typography.bodyLarge)
                        Text("Title medium", style = MaterialTheme.typography.titleMedium)
                        Text("Big but regular", fontSize = 28.sp)
                        Text("Bold but small", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Semi bold", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                """,
            ),
        )
    }

    fun testTextInsideButton() {
        expectClean(
            screen(
                """
                @Composable
                fun Action(onClick: () -> Unit) {
                    Button(onClick = onClick) {
                        Text("Continue", style = MaterialTheme.typography.titleLarge)
                    }
                }
                """,
            ),
        )
    }

    fun testTopAppBarTitles() {
        expectClean(
            screen(
                """
                @Composable
                fun Bars() {
                    TopAppBar(title = { Text("Inbox", style = MaterialTheme.typography.titleLarge) })
                    CenterAlignedTopAppBar(title = { Text("Inbox", fontSize = 22.sp, fontWeight = FontWeight.Bold) })
                }
                """,
            ),
        )
    }

    fun testStyleFromVariableIsNotKnown() {
        expectClean(
            screen(
                """
                @Composable
                fun Header(style: TextStyle) {
                    val headline = MaterialTheme.typography.headlineSmall
                    Text("Settings", style = headline)
                    Text("Settings", style = style)
                }
                """,
            ),
        )
    }
}
