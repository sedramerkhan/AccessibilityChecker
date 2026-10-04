package org.svu.sedra.a11ylint.detectors.perceivable

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.testing.A11yLintTest

class P04TextSizeInDpDetectorTest : A11yLintTest() {
    override fun getDetector(): Detector = P04TextSizeInDpDetector()
    override fun getIssues(): List<Issue> = listOf(P04TextSizeInDpDetector.ISSUE)

    private fun screen(body: String) = kotlin(
        "src/test/pkg/Screen.kt",
        """
package test.pkg

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

$body
        """,
    )

    // Positive tests: must be flagged.

    fun testLiteralDpConvertedInline() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:14: Warning: [P-04] Text size is converted from a dp value, so it will not scale again when the user changes their system font size [ComposeTextSizeInDp]
                    Text("Caption", fontSize = with(LocalDensity.current) { 16.dp.toSp() })
                                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen() {
                    Text("Caption", fontSize = with(LocalDensity.current) { 16.dp.toSp() })
                }
            """),
        )
    }

    fun testDpVariableConverted() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:15: Warning: [P-04] Text size is converted from a dp value, so it will not scale again when the user changes their system font size [ComposeTextSizeInDp]
                    Text("Caption", fontSize = with(LocalDensity.current) { cardPadding.toSp() })
                                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen() {
                    val cardPadding = 16.dp
                    Text("Caption", fontSize = with(LocalDensity.current) { cardPadding.toSp() })
                }
            """),
        )
    }

    fun testTextStyleFontSizeConverted() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:14: Warning: [P-04] Text size is converted from a dp value, so it will not scale again when the user changes their system font size [ComposeTextSizeInDp]
                    Text("Caption", style = TextStyle(fontSize = with(LocalDensity.current) { 16.dp.toSp() }))
                                                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen() {
                    Text("Caption", style = TextStyle(fontSize = with(LocalDensity.current) { 16.dp.toSp() }))
                }
            """),
        )
    }

    fun testCopiedStyleFontSizeConverted() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:17: Warning: [P-04] Text size is converted from a dp value, so it will not scale again when the user changes their system font size [ComposeTextSizeInDp]
                            fontSize = with(LocalDensity.current) { 16.dp.toSp() },
                                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen() {
                    Text(
                        "Caption",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = with(LocalDensity.current) { 16.dp.toSp() },
                        ),
                    )
                }
            """),
        )
    }

    // Negative tests: must not be flagged.

    fun testLiteralSpIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Text("Caption", fontSize = 16.sp)
        }
    """))

    fun testTypographyStyleIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Text("Caption", style = MaterialTheme.typography.bodyLarge)
        }
    """))

    fun testNoFontSizeIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Text("Caption")
        }
    """))

    fun testFontSizeFromVariableIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            val size = 16.sp
            Text("Caption", fontSize = size)
        }
    """))

    fun testTextStyleLiteralSpIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Text("Caption", style = TextStyle(fontSize = 16.sp))
        }
    """))

    fun testUnrelatedWithBlockIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Text("Caption", fontSize = with(LocalDensity.current) { 16.sp })
        }
    """))
}
