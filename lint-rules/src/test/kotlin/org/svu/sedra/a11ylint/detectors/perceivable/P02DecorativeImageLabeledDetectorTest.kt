package org.svu.sedra.a11ylint.detectors.perceivable

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.testing.A11yLintTest

class P02DecorativeImageLabeledDetectorTest : A11yLintTest() {
    override fun getDetector(): Detector = P02DecorativeImageLabeledDetector()
    override fun getIssues(): List<Issue> = listOf(P02DecorativeImageLabeledDetector.ISSUE)

    private fun screen(body: String) = kotlin(
        "src/test/pkg/Screen.kt",
        """
        package test.pkg

        import androidx.compose.foundation.Image
        import androidx.compose.material3.Icon
        import androidx.compose.material3.Text
        import androidx.compose.runtime.Composable
        import androidx.compose.ui.Modifier
        import androidx.compose.ui.res.stringResource

        $body
        """.trimIndent(),
    )

    fun testIconLabelEqualsSiblingText() = expectWarnings(
        """
        src/test/pkg/Screen.kt:16: Error: [P-02] Possible decorative image label repeats visible text "Delete" [ComposeDecorativeImageLabeled]
                Icon(contentDescription = "Delete")
                                             ~~~~~~~~
        1 errors, 0 warnings
        """.trimIndent(),
        screen("""
            @Composable fun Screen() {
                Icon(contentDescription = "Delete")
                Text("Delete")
            }
        """),
    )

    fun testImageLabelContainedInSiblingText() = expectWarnings(
        """
        src/test/pkg/Screen.kt:16: Error: [P-02] Possible decorative image label repeats visible text "Delete" [ComposeDecorativeImageLabeled]
                Image(contentDescription = "Delete")
                                              ~~~~~~~~
        1 errors, 0 warnings
        """.trimIndent(),
        screen("""
            @Composable fun Screen() {
                Image(contentDescription = "Delete")
                Text("Delete draft")
            }
        """),
    )

    fun testDifferentTextIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Icon(contentDescription = "Trash icon")
            Text("Delete draft")
        }
    """))

    fun testInteractiveImageIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Icon(contentDescription = "Delete", modifier = Modifier.clickable { })
            Text("Delete")
        }
    """))

    fun testResourceLabelIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Icon(contentDescription = stringResource(1))
            Text("Delete")
        }
    """))

    fun testNestedTextIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Icon(contentDescription = "Delete")
            Label()
        }
        @Composable fun Label() { Text("Delete") }
    """))
}
