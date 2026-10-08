package org.svu.sedra.a11ylint.detectors.robust

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.testing.A11yLintTest

class R05MissingProgressRangeDetectorTest : A11yLintTest() {
    override fun getDetector(): Detector = R05MissingProgressRangeDetector()
    override fun getIssues(): List<Issue> = listOf(R05MissingProgressRangeDetector.ISSUE)

    private fun screen(body: String) = kotlin(
        "src/test/pkg/Screen.kt",
        """
package test.pkg

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics

$body
        """,
    )

    // Positive tests: must be flagged.

    fun testCanvasProgressBar() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:17: Warning: [R-05] ProgressBar draws its own progress from progress but sets no progressBarRangeInfo, so screen readers announce no value [ComposeMissingProgressRange]
                @Composable fun ProgressBar(progress: Float) {
                                ~~~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun ProgressBar(progress: Float) {
                    Canvas(Modifier) { }
                }
            """),
        )
    }

    fun testDrawBehindRing() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:17: Warning: [R-05] Ring draws its own progress from fraction but sets no progressBarRangeInfo, so screen readers announce no value [ComposeMissingProgressRange]
                @Composable fun Ring(fraction: Float) {
                                ~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Ring(fraction: Float) {
                    Box(Modifier.drawBehind { })
                }
            """),
        )
    }

    // The name only has to contain one of the words, so downloadPercent counts.
    fun testNameContainingAProgressWord() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:17: Warning: [R-05] Download draws its own progress from downloadPercent but sets no progressBarRangeInfo, so screen readers announce no value [ComposeMissingProgressRange]
                @Composable fun Download(downloadPercent: Float) {
                                ~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Download(downloadPercent: Float) {
                    Canvas(Modifier) { }
                }
            """),
        )
    }

    // A semantics block that sets something else does not carry the range.
    fun testSemanticsWithoutTheRange() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:17: Warning: [R-05] ProgressBar draws its own progress from progress but sets no progressBarRangeInfo, so screen readers announce no value [ComposeMissingProgressRange]
                @Composable fun ProgressBar(progress: Float) {
                                ~~~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun ProgressBar(progress: Float) {
                    Canvas(Modifier.semantics { contentDescription = "Downloading" }) { }
                }
            """),
        )
    }

    // Negative tests: must not be flagged.

    fun testProgressBarRangeInfoIsClean() = expectClean(screen("""
        @Composable fun ProgressBar(progress: Float) {
            Canvas(
                Modifier.semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                }
            ) { }
        }
    """))

    // The Material indicator sets the range itself.
    fun testMaterialIndicatorIsClean() = expectClean(screen("""
        @Composable fun ProgressBar(progress: Float) {
            Canvas(Modifier) { }
            LinearProgressIndicator(progress = { progress })
        }
    """))

    // Drawing with no progress parameter is not a progress indicator.
    fun testCanvasWithoutAProgressParameterIsClean() = expectClean(screen("""
        @Composable fun Divider(thickness: Float) {
            Canvas(Modifier) { }
        }
    """))

    // A progress parameter that is not drawn by hand is somebody else's problem.
    fun testProgressWithoutDrawingIsClean() = expectClean(screen("""
        @Composable fun ProgressLabel(progress: Float) {
            Text("Downloading")
        }
    """))

    // The parameter must be a Float.
    fun testNonFloatProgressIsClean() = expectClean(screen("""
        @Composable fun Steps(progress: Int) {
            Canvas(Modifier) { }
        }
    """))

    fun testNonComposableFunctionIsClean() = expectClean(screen("""
        fun computeProgress(progress: Float): Float {
            return progress
        }
    """))
}
