package org.svu.sedra.a11ylint.detectors.operable

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.testing.A11yLintTest

class O05EmptyClickableDetectorTest : A11yLintTest() {
    override fun getDetector(): Detector = O05EmptyClickableDetector()
    override fun getIssues(): List<Issue> = listOf(O05EmptyClickableDetector.ISSUE)

    private fun screen(body: String) = kotlin(
        "src/test/pkg/Screen.kt",
        """
package test.pkg

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

$body
        """,
    )

    // Positive tests: must be flagged.

    fun testClickableWithNoContent() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:23: Warning: [O-05] Clickable Box has nothing to read: no text, no labelled icon and no contentDescription, so screen readers announce it without a name [ComposeEmptyClickable]
                    Box(modifier = Modifier.size(48.dp).clickable { onAction() })
                    ~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onAction: () -> Unit) {
                    Box(modifier = Modifier.size(48.dp).clickable { onAction() })
                }
            """),
        )
    }

    fun testClickableWithEmptyContent() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:23: Warning: [O-05] Clickable Box has nothing to read: no text, no labelled icon and no contentDescription, so screen readers announce it without a name [ComposeEmptyClickable]
                    Box(modifier = Modifier.clickable { onAction() }) {
                    ~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onAction: () -> Unit) {
                    Box(modifier = Modifier.clickable { onAction() }) {
                    }
                }
            """),
        )
    }

    fun testClickableSpacer() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:23: Warning: [O-05] Clickable Spacer has nothing to read: no text, no labelled icon and no contentDescription, so screen readers announce it without a name [ComposeEmptyClickable]
                    Spacer(Modifier.size(48.dp).clickable { onAction() })
                    ~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onAction: () -> Unit) {
                    Spacer(Modifier.size(48.dp).clickable { onAction() })
                }
            """),
        )
    }

    fun testEmptyIconButton() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:23: Warning: [O-05] Clickable IconButton has nothing to read: no text, no labelled icon and no contentDescription, so screen readers announce it without a name [ComposeEmptyClickable]
                    IconButton(onClick = onAction) {
                    ~~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onAction: () -> Unit) {
                    IconButton(onClick = onAction) {
                    }
                }
            """),
        )
    }

    fun testClickableCanvas() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:23: Warning: [O-05] Clickable Canvas has nothing to read: no text, no labelled icon and no contentDescription, so screen readers announce it without a name [ComposeEmptyClickable]
                    Canvas(Modifier.size(48.dp).clickable { onAction() }) {
                    ~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onAction: () -> Unit) {
                    Canvas(Modifier.size(48.dp).clickable { onAction() }) {
                    }
                }
            """),
        )
    }

    fun testOnlySpacerInside() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:23: Warning: [O-05] Clickable Box has nothing to read: no text, no labelled icon and no contentDescription, so screen readers announce it without a name [ComposeEmptyClickable]
                    Box(modifier = Modifier.clickable { onAction() }) {
                    ~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onAction: () -> Unit) {
                    Box(modifier = Modifier.clickable { onAction() }) {
                        Spacer(Modifier.size(8.dp))
                    }
                }
            """),
        )
    }

    // Negative tests: must not be flagged.

    fun testTextInsideIsClean() = expectClean(screen("""
        @Composable fun Screen(onAction: () -> Unit) {
            Box(modifier = Modifier.clickable { onAction() }) {
                Text("Open")
            }
        }
    """))

    fun testLabelledIconInsideIsClean() = expectClean(screen("""
        @Composable fun Screen(onAction: () -> Unit) {
            Box(modifier = Modifier.clickable { onAction() }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    """))

    // An unlabelled Icon inside a clickable is P-01's defect, not O-05's (overlap policy 7.1).
    fun testUnlabelledIconInsideIsLeftToP01() = expectClean(screen("""
        @Composable fun Screen(onAction: () -> Unit) {
            Box(modifier = Modifier.clickable { onAction() }) {
                Icon(Icons.Filled.Delete, contentDescription = null)
            }
        }
    """))

    fun testSemanticsLabelIsClean() = expectClean(screen("""
        @Composable fun Screen(onAction: () -> Unit) {
            Box(
                modifier = Modifier
                    .clickable { onAction() }
                    .semantics { contentDescription = "Open" }
            )
        }
    """))

    // The content calls a composable this rule cannot see into, so it may well provide the name.
    fun testUnknownComposableInsideIsClean() = expectClean(screen("""
        @Composable fun Screen(onAction: () -> Unit) {
            Box(modifier = Modifier.clickable { onAction() }) {
                ItemRow()
            }
        }

        @Composable fun ItemRow() {
            Text("Running shoes")
        }
    """))

    fun testClickableIconIsClean() = expectClean(screen("""
        @Composable fun Screen(onAction: () -> Unit) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete",
                modifier = Modifier.clickable { onAction() },
            )
        }
    """))

    fun testNonClickableEmptyBoxIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Column {
                Box(modifier = Modifier.size(48.dp))
            }
        }
    """))
}
