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
        import androidx.compose.foundation.clickable
        import androidx.compose.material3.Icon
        import androidx.compose.material3.Text
        import androidx.compose.runtime.Composable
        import androidx.compose.ui.Modifier
        import androidx.compose.ui.graphics.vector.ImageVector
        import androidx.compose.ui.res.stringResource
        import androidx.compose.ui.res.painterResource

        $body
        """.trimIndent(),
    )

    fun testIconLabelEqualsSiblingText() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:15: Warning: [P-02] Possible decorative image label repeats visible text "Delete" [ComposeDecorativeImageLabeled]
                Icon(imageVector = ImageVector(), contentDescription = "Delete")
                                                                       ~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                    @Composable fun Screen() {
                        Icon(imageVector = ImageVector(), contentDescription = "Delete")
                        Text("Delete")
                    }
                """),
        )
    }

    fun testImageLabelContainedInSiblingText() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:15: Warning: [P-02] Possible decorative image label repeats visible text "Delete draft" [ComposeDecorativeImageLabeled]
                Image(imageVector = ImageVector(), contentDescription = "Delete")
                                                                        ~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                    @Composable fun Screen() {
                        Image(imageVector = ImageVector(), contentDescription = "Delete")
                        Text("Delete draft")
                    }
                """),
        )
    }

    fun testDifferentTextIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Icon(ImageVector(), "Trash icon")
            Text("Delete draft")
        }
    """))

    fun testInteractiveImageIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Icon(ImageVector(), "Delete", Modifier.clickable { })
            Text("Delete")
        }
    """))

    fun testResourceLabelIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Icon(ImageVector(), stringResource(1))
            Text("Delete")
        }
    """))

    fun testNestedTextIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Icon(ImageVector(), "Delete")
            Label()
        }
        @Composable fun Label() { Text("Delete") }
    """))
}
