package org.svu.sedra.a11ylint.detectors.perceivable

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.testing.A11yLintTest

class P05TextOverImageDetectorTest : A11yLintTest() {
    override fun getDetector(): Detector = P05TextOverImageDetector()
    override fun getIssues(): List<Issue> = listOf(P05TextOverImageDetector.ISSUE)

    private fun screen(body: String) = kotlin(
        "src/test/pkg/Screen.kt",
        """
package test.pkg

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

$body
        """,
    )

    // Positive tests: must be flagged.

    fun testTextStraightOverAnImage() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:20: Warning: [P-05] Possible text over an image: this Text is drawn on top of an Image with no background or scrim, so its contrast depends on the picture [ComposeTextOverImage]
                        Text("Summer sale")
                        ~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen() {
                    Box {
                        Image(Icons.Filled.Delete, contentDescription = null)
                        Text("Summer sale")
                    }
                }
            """),
        )
    }

    // The first Text above the Image is the one reported, so one Box gives one warning.
    fun testFirstTextAboveTheImageIsReported() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:20: Warning: [P-05] Possible text over an image: this Text is drawn on top of an Image with no background or scrim, so its contrast depends on the picture [ComposeTextOverImage]
                        Text("Shop now")
                        ~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen() {
                    Box {
                        Image(Icons.Filled.Delete, contentDescription = null)
                        Text("Shop now")
                        Text("Summer sale")
                    }
                }
            """),
        )
    }

    // Negative tests: must not be flagged.

    fun testTextWithItsOwnBackgroundIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Box {
                Image(Icons.Filled.Delete, contentDescription = null)
                Text("Summer sale", modifier = Modifier.background(Color(0x99000000)))
            }
        }
    """))

    fun testScrimLayerBetweenIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Box {
                Image(Icons.Filled.Delete, contentDescription = null)
                Box(modifier = Modifier.fillMaxSize().background(Color(0x99000000)))
                Text("Summer sale")
            }
        }
    """))

    // The Text is drawn first, so the Image is on top of it, not the other way round.
    fun testTextBeforeImageIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Box {
                Text("Summer sale")
                Image(Icons.Filled.Delete, contentDescription = null)
            }
        }
    """))

    fun testNoImageIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Box {
                Text("Summer sale")
            }
        }
    """))

    // A Column stacks its children, it does not overlay them.
    fun testColumnIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Column {
                Image(Icons.Filled.Delete, contentDescription = null)
                Text("Summer sale")
            }
        }
    """))
}
