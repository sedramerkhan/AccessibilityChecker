package org.svu.sedra.a11ylint.detectors.perceivable

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.testing.A11yLintTest

class P07MissingPaneTitleDetectorTest : A11yLintTest() {
    override fun getDetector(): Detector = P07MissingPaneTitleDetector()
    override fun getIssues(): List<Issue> = listOf(P07MissingPaneTitleDetector.ISSUE)

    private fun screen(body: String) = kotlin(
        "src/test/pkg/Screen.kt",
        """
package test.pkg

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup

$body
        """,
    )

    // Positive tests: must be flagged.

    fun testPopupWithNoPaneTitle() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:17: Warning: [P-07] Popup has no paneTitle, so screen readers do not announce what opened [ComposeMissingPaneTitle]
                    Popup(onDismissRequest = onDismiss) {
                    ~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onDismiss: () -> Unit) {
                    Popup(onDismissRequest = onDismiss) {
                        Column {
                            Text("Sort by date")
                            Text("Sort by name")
                        }
                    }
                }
            """),
        )
    }

    fun testDialogWithNoPaneTitle() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:17: Warning: [P-07] Dialog has no paneTitle, so screen readers do not announce what opened [ComposeMissingPaneTitle]
                    Dialog(onDismissRequest = onDismiss) {
                    ~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onDismiss: () -> Unit) {
                    Dialog(onDismissRequest = onDismiss) {
                        Box {
                            Text("Delete this draft?")
                        }
                    }
                }
            """),
        )
    }

    // Negative tests: must not be flagged.

    fun testPopupWithPaneTitleIsClean() = expectClean(screen("""
        @Composable fun Screen(onDismiss: () -> Unit) {
            Popup(onDismissRequest = onDismiss) {
                Column(modifier = Modifier.semantics { paneTitle = "Sort options" }) {
                    Text("Sort by date")
                    Text("Sort by name")
                }
            }
        }
    """))

    fun testDialogWithPaneTitleIsClean() = expectClean(screen("""
        @Composable fun Screen(onDismiss: () -> Unit) {
            Dialog(onDismissRequest = onDismiss) {
                Box(modifier = Modifier.semantics { paneTitle = "Delete draft" }) {
                    Text("Delete this draft?")
                }
            }
        }
    """))

    // The pane title may be set deeper inside the overlay, not on its first child.
    fun testPaneTitleNestedDeeperIsClean() = expectClean(screen("""
        @Composable fun Screen(onDismiss: () -> Unit) {
            Dialog(onDismissRequest = onDismiss) {
                Box {
                    Column(modifier = Modifier.semantics { paneTitle = "Delete draft" }) {
                        Text("Delete this draft?")
                    }
                }
            }
        }
    """))

    // Material overlays set their own paneTitle, checked in the Material3 sources (see DECISIONS).
    fun testMaterialOverlayIsClean() = expectClean(screen("""
        @Composable fun Screen(onDismiss: () -> Unit) {
            ModalBottomSheet(onDismissRequest = onDismiss) {
                Text("Sort by date")
            }
        }
    """))

    fun testOrdinaryLayoutIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Column {
                Text("Sort by date")
            }
        }
    """))
}
