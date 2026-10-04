package org.svu.sedra.a11ylint.detectors.perceivable

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.testing.A11yLintTest

class P03LowContrastColorsDetectorTest : A11yLintTest() {
    override fun getDetector(): Detector = P03LowContrastColorsDetector()
    override fun getIssues(): List<Issue> = listOf(P03LowContrastColorsDetector.ISSUE)

    private fun screen(body: String) = kotlin(
        "src/test/pkg/Screen.kt",
        """
package test.pkg

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

$body
        """,
    )

    // Positive tests: must be flagged.

    fun testSurfaceBackgroundIsLowContrast() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:18: Warning: [P-03] Text color has a contrast ratio of 2.2:1 against its background, below the required 4.5:1 [ComposeLowContrastColors]
                        Text("Delete draft", color = Color(0xFFB0B0B0))
                                                     ~~~~~~~~~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen() {
                    Surface(color = Color(0xFFFFFFFF)) {
                        Text("Delete draft", color = Color(0xFFB0B0B0))
                    }
                }
            """),
        )
    }

    fun testModifierBackgroundOnTextIsLowContrast() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:19: Warning: [P-03] Text color has a contrast ratio of 2.2:1 against its background, below the required 4.5:1 [ComposeLowContrastColors]
                        color = Color(0xFFB0B0B0),
                                ~~~~~~~~~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen() {
                    Text(
                        "Delete draft",
                        color = Color(0xFFB0B0B0),
                        modifier = Modifier.background(Color(0xFFFFFFFF)),
                    )
                }
            """),
        )
    }

    fun testBoxBackgroundIsLowContrast() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:18: Warning: [P-03] Text color has a contrast ratio of 2.2:1 against its background, below the required 4.5:1 [ComposeLowContrastColors]
                        Text("Delete draft", color = Color(0xFFB0B0B0))
                                                     ~~~~~~~~~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen() {
                    Box(modifier = Modifier.background(Color(0xFFFFFFFF))) {
                        Text("Delete draft", color = Color(0xFFB0B0B0))
                    }
                }
            """),
        )
    }

    fun testLargeTextStillFlaggedBelowThreeToOne() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:18: Warning: [P-03] Text color has a contrast ratio of 2.2:1 against its background, below the required 3.0:1 [ComposeLowContrastColors]
                        Text("Delete draft", color = Color(0xFFB0B0B0), fontSize = 24.sp)
                                                     ~~~~~~~~~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen() {
                    Surface(color = Color(0xFFFFFFFF)) {
                        Text("Delete draft", color = Color(0xFFB0B0B0), fontSize = 24.sp)
                    }
                }
            """),
        )
    }

    // Negative tests: must not be flagged.

    fun testHighContrastIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Surface(color = Color(0xFFFFFFFF)) {
                Text("Delete draft", color = Color(0xFF000000))
            }
        }
    """))

    fun testThemeTextColorIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Surface(color = Color(0xFFFFFFFF)) {
                Text("Delete draft", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    """))

    fun testThemeBackgroundColorIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Text("Delete draft", color = Color(0xFFB0B0B0))
            }
        }
    """))

    fun testNoColorArgumentIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Surface(color = Color(0xFFFFFFFF)) {
                Text("Delete draft")
            }
        }
    """))

    fun testLargeTextAtThreeToOneIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Surface(color = Color(0xFFFFFFFF)) {
                Text("Delete draft", color = Color(0xFF898989), fontSize = 24.sp)
            }
        }
    """))

    fun testNoKnownBackgroundIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Column {
                Text("Delete draft", color = Color(0xFFB0B0B0))
            }
        }
    """))
}
