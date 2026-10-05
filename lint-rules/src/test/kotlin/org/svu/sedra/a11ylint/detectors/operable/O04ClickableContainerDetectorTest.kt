package org.svu.sedra.a11ylint.detectors.operable

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.testing.A11yLintTest

class O04ClickableContainerDetectorTest : A11yLintTest() {
    override fun getDetector(): Detector = O04ClickableContainerDetector()
    override fun getIssues(): List<Issue> = listOf(O04ClickableContainerDetector.ISSUE)

    private fun screen(body: String) = kotlin(
        "src/test/pkg/Screen.kt",
        """
package test.pkg

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

$body
        """,
    )

    // Positive tests: must be flagged.

    fun testRowWithOneText() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:24: Warning: [O-04] Possible button: clickable Row holds only a label, so screen readers announce a plain container instead of a button [ComposeClickableContainer]
                    Row(modifier = Modifier.clickable { onAction() }) {
                                            ~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onAction: () -> Unit) {
                    Row(modifier = Modifier.clickable { onAction() }) {
                        Text("Buy now")
                    }
                }
            """),
        )
    }

    fun testBoxWithIconAndText() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:24: Warning: [O-04] Possible button: clickable Box holds only a label, so screen readers announce a plain container instead of a button [ComposeClickableContainer]
                    Box(modifier = Modifier.clickable { onAction() }) {
                                            ~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onAction: () -> Unit) {
                    Box(modifier = Modifier.clickable { onAction() }) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Text("Delete")
                    }
                }
            """),
        )
    }

    fun testColumnWithSpacerIgnored() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:24: Warning: [O-04] Possible button: clickable Column holds only a label, so screen readers announce a plain container instead of a button [ComposeClickableContainer]
                    Column(modifier = Modifier.clickable { onAction() }) {
                                               ~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onAction: () -> Unit) {
                    Column(modifier = Modifier.clickable { onAction() }) {
                        Spacer(Modifier)
                        Text("Buy now")
                    }
                }
            """),
        )
    }

    fun testCombinedClickable() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:24: Warning: [O-04] Possible button: clickable Row holds only a label, so screen readers announce a plain container instead of a button [ComposeClickableContainer]
                    Row(modifier = Modifier.combinedClickable(onLongClick = onMenu) { onAction() }) {
                                            ~~~~~~~~~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onAction: () -> Unit, onMenu: () -> Unit) {
                    Row(modifier = Modifier.combinedClickable(onLongClick = onMenu) { onAction() }) {
                        Text("Buy now")
                    }
                }
            """),
        )
    }

    fun testExplicitNullRole() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:24: Warning: [O-04] Possible button: clickable Row holds only a label, so screen readers announce a plain container instead of a button [ComposeClickableContainer]
                    Row(modifier = Modifier.clickable(role = null, onClick = onAction)) {
                                            ~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onAction: () -> Unit) {
                    Row(modifier = Modifier.clickable(role = null, onClick = onAction)) {
                        Text("Buy now")
                    }
                }
            """),
        )
    }

    // Negative tests: must not be flagged.

    fun testRoleOnClickModifierIsClean() = expectClean(screen("""
        @Composable fun Screen(onAction: () -> Unit) {
            Row(modifier = Modifier.clickable(role = Role.Button) { onAction() }) {
                Text("Buy now")
            }
        }
    """))

    // No trailing comma before the trailing lambda: Lint's REORDER_ARGUMENTS test mode cannot
    // rewrite that shape (see DECISIONS, the same note as P-01).
    fun testRoleInSemanticsIsClean() = expectClean(screen("""
        @Composable fun Screen(onAction: () -> Unit) {
            Row(
                modifier = Modifier
                    .clickable { onAction() }
                    .semantics { role = Role.Tab }
            ) {
                Text("Buy now")
            }
        }
    """))

    fun testRichContentIsClean() = expectClean(screen("""
        @Composable fun Screen(onAction: () -> Unit) {
            Row(modifier = Modifier.clickable { onAction() }) {
                Text("Sedra Merkhan")
                Text("Last seen today")
            }
        }
    """))

    fun testIconOnlyIsClean() = expectClean(screen("""
        @Composable fun Screen(onAction: () -> Unit) {
            Box(modifier = Modifier.clickable { onAction() }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    """))

    fun testCardIsClean() = expectClean(screen("""
        @Composable fun Screen(onAction: () -> Unit) {
            Card(modifier = Modifier.clickable { onAction() }) {
                Text("Order #1024")
            }
        }
    """))

    fun testMaterialButtonIsClean() = expectClean(screen("""
        @Composable fun Screen(onAction: () -> Unit) {
            Button(onClick = onAction) {
                Text("Buy now")
            }
        }
    """))
}
