package org.svu.sedra.a11ylint.detectors.robust

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.testing.A11yLintTest

class R02ClearAndSetSemanticsLossDetectorTest : A11yLintTest() {
    override fun getDetector(): Detector = R02ClearAndSetSemanticsLossDetector()
    override fun getIssues(): List<Issue> = listOf(R02ClearAndSetSemanticsLossDetector.ISSUE)

    private fun screen(body: String) = kotlin(
        "src/test/pkg/Screen.kt",
        """
package test.pkg

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription

$body
        """,
    )

    // Positive tests: must be flagged.

    fun testEmptyBlock() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:23: Warning: [R-02] clearAndSetSemantics is empty, so it hides the name of its content from screen readers [ComposeClearAndSetSemanticsLoss]
                    Row(modifier = Modifier.clearAndSetSemantics { }) {
                                            ~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen() {
                    Row(modifier = Modifier.clearAndSetSemantics { }) {
                        Text("Running shoes")
                    }
                }
            """),
        )
    }

    fun testNameNotReplaced() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:23: Warning: [R-02] clearAndSetSemantics does not put back the name of its content, so screen readers lose it [ComposeClearAndSetSemanticsLoss]
                    Row(modifier = Modifier.clearAndSetSemantics { stateDescription = "Expanded" }) {
                                            ~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen() {
                    Row(modifier = Modifier.clearAndSetSemantics { stateDescription = "Expanded" }) {
                        Text("Running shoes")
                    }
                }
            """),
        )
    }

    fun testRoleNotReplaced() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:26: Warning: [R-02] clearAndSetSemantics does not put back the role and state of its content, so screen readers lose it [ComposeClearAndSetSemanticsLoss]
                            .clearAndSetSemantics { contentDescription = "Running shoes" }
                             ~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onAction: () -> Unit) {
                    Row(
                        modifier = Modifier
                            .clickable(role = Role.Button) { onAction() }
                            .clearAndSetSemantics { contentDescription = "Running shoes" }
                    ) {
                        Text("Running shoes")
                    }
                }
            """),
        )
    }

    fun testStatefulChildNotReplaced() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:23: Warning: [R-02] clearAndSetSemantics does not put back the role and state of its content, so screen readers lose it [ComposeClearAndSetSemanticsLoss]
                    Row(modifier = Modifier.clearAndSetSemantics { contentDescription = "Subscribe" }) {
                                            ~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(checked: Boolean, onChange: (Boolean) -> Unit) {
                    Row(modifier = Modifier.clearAndSetSemantics { contentDescription = "Subscribe" }) {
                        Checkbox(checked = checked, onCheckedChange = onChange)
                        Text("Subscribe")
                    }
                }
            """),
        )
    }

    // Negative tests: must not be flagged.

    fun testNameReplacedIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Row(modifier = Modifier.clearAndSetSemantics { contentDescription = "Running shoes, 42 euro" }) {
                Text("Running shoes")
                Text("42 euro")
            }
        }
    """))

    fun testNameAndRoleReplacedIsClean() = expectClean(screen("""
        @Composable fun Screen(onAction: () -> Unit) {
            Row(
                modifier = Modifier
                    .clickable(role = Role.Button) { onAction() }
                    .clearAndSetSemantics {
                        contentDescription = "Running shoes"
                        role = Role.Button
                    }
            ) {
                Text("Running shoes")
            }
        }
    """))

    fun testStatefulChildWithStateDescriptionIsClean() = expectClean(screen("""
        @Composable fun Screen(checked: Boolean, onChange: (Boolean) -> Unit) {
            Row(
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = "Subscribe"
                    stateDescription = if (checked) "Subscribed" else "Not subscribed"
                }
            ) {
                Checkbox(checked = checked, onCheckedChange = onChange)
                Text("Subscribe")
            }
        }
    """))

    fun testDecorativeContentIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Row(modifier = Modifier.clearAndSetSemantics { contentDescription = "Rating: 4 of 5" }) {
                Icon(Icons.Filled.Delete, contentDescription = null)
                Icon(Icons.Filled.Delete, contentDescription = null)
            }
        }
    """))

    fun testPlainSemanticsIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Row(modifier = Modifier.clearAndSetSemantics { contentDescription = "Running shoes" }) {
                Text("Running shoes")
            }
        }
    """))

    // Hiding a subtree whose name and interaction the rule cannot see is the documented use of
    // an empty block. This is the JetNews BookmarkButton pattern, where the row above offers the
    // action instead, and it was a false positive before (see DEV_APP_RESULTS).
    fun testEmptyBlockOnUnreadableContentIsClean() = expectClean(screen("""
        @Composable fun Screen(onToggle: () -> Unit) {
            BookmarkButton(
                onClick = onToggle,
                // Remove button semantics so the action can be handled at row level
                modifier = Modifier.clearAndSetSemantics { },
            )
        }

        @Composable fun BookmarkButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
            Text("Bookmark", modifier = modifier)
        }
    """))
}
