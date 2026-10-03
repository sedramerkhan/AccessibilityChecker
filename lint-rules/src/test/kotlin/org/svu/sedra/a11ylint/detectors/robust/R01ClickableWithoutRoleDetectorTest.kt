package org.svu.sedra.a11ylint.detectors.robust

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.testing.A11yLintTest

class R01ClickableWithoutRoleDetectorTest : A11yLintTest() {
    override fun getDetector(): Detector = R01ClickableWithoutRoleDetector()

    override fun getIssues(): List<Issue> = listOf(R01ClickableWithoutRoleDetector.ISSUE)

    /** A screen file with the imports every test needs. [body] starts at line 25. */
    private fun screen(body: String): TestFile = kotlin(
        "src/test/pkg/Screen.kt",
        """
package test.pkg

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics


""" + body.trimIndent(),
    )

    private fun message(element: String) =
        "[R-01] Possible missing role: $element has no role, so screen readers do not say what " +
            "kind of control it is [ComposeClickableWithoutRole]"

    // Positive tests: must be flagged.

    fun testClickableBoxWithOnlyAnIcon() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:27: Error: ${message("clickable Box")}
                Box(Modifier.clickable { onShare() }) {
                             ~~~~~~~~~
            1 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun Share(onShare: () -> Unit) {
                    Box(Modifier.clickable { onShare() }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                }
                """,
            ),
        )
    }

    fun testClickableRowWithRichContent() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:27: Error: ${message("clickable Row")}
                Row(modifier = Modifier.clickable { onOpen() }) {
                                        ~~~~~~~~~
            1 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun Contact(onOpen: () -> Unit) {
                    Row(modifier = Modifier.clickable { onOpen() }) {
                        Text("Sedra")
                        Text("Online")
                    }
                }
                """,
            ),
        )
    }

    fun testOtherCustomElements() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:27: Error: ${message("clickable Card")}
                Card(modifier = Modifier.clickable { onOpen() }) {
                                         ~~~~~~~~~
            src/test/pkg/Screen.kt:30: Error: ${message("clickable Image")}
                Image(painterResource(1), "Avatar", Modifier.combinedClickable(onLongClick = {}) { onOpen() })
                                                             ~~~~~~~~~~~~~~~~~
            2 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun Elements(onOpen: () -> Unit) {
                    Card(modifier = Modifier.clickable { onOpen() }) {
                        Text("Details")
                    }
                    Image(painterResource(1), "Avatar", Modifier.combinedClickable(onLongClick = {}) { onOpen() })
                }
                """,
            ),
        )
    }

    fun testNullRoleAndModifierInLocalValue() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:27: Error: ${message("clickable element")}
                val tappable = Modifier.clickable(role = null) { onOpen() }
                                        ~~~~~~~~~
            1 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun Tile(onOpen: () -> Unit) {
                    val tappable = Modifier.clickable(role = null) { onOpen() }
                    Column(tappable) {
                        Text("Title")
                        Text("Subtitle")
                    }
                }
                """,
            ),
        )
    }

    // Negative tests: must not be flagged.

    fun testRoleArgument() {
        expectClean(
            screen(
                """
                @Composable
                fun Share(onShare: () -> Unit) {
                    Box(Modifier.clickable(role = Role.Button) { onShare() }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                }
                """,
            ),
        )
    }

    fun testRoleInSemantics() {
        expectClean(
            screen(
                """
                @Composable
                fun Tab(onSelect: () -> Unit) {
                    Row(Modifier.semantics { role = Role.Tab }.clickable { onSelect() }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                        Text("Shared")
                        Text("12")
                    }
                }
                """,
            ),
        )
    }

    fun testButtonLikeContainerIsLeftToO04() {
        expectClean(
            screen(
                """
                @Composable
                fun Actions(onShare: () -> Unit) {
                    Row(Modifier.clickable { onShare() }) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Spacer(Modifier)
                        Text("Share")
                    }
                    Box(modifier = Modifier.clickable { onShare() }) {
                        Text("Share")
                    }
                }
                """,
            ),
        )
    }

    fun testMaterialClickableComponents() {
        expectClean(
            screen(
                """
                @Composable
                fun Actions(onShare: () -> Unit) {
                    Button(onClick = onShare, modifier = Modifier.clickable { onShare() }) {
                        Text("Share")
                    }
                    Card(onClick = onShare, modifier = Modifier.clickable { onShare() }) {
                        Text("Details")
                        Text("More")
                    }
                }
                """,
            ),
        )
    }
}
