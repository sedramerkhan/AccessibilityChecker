package org.svu.sedra.a11ylint.detectors.operable

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.testing.A11yLintTest

class O02MissingOnClickLabelDetectorTest : A11yLintTest() {
    override fun getDetector(): Detector = O02MissingOnClickLabelDetector()
    override fun getIssues(): List<Issue> = listOf(O02MissingOnClickLabelDetector.ISSUE)

    private fun screen(body: String) = kotlin(
        "src/test/pkg/Screen.kt",
        """
package test.pkg

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

$body
        """,
    )

    // Positive tests: must be flagged.

    fun testRowWithNoLabel() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:21: Warning: [O-02] Clickable Row has no onClickLabel, so screen readers only announce "double tap to activate" with nothing describing what it does [ComposeMissingOnClickLabel]
                    Row(modifier = Modifier.clickable { onAction() }) {
                                            ~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onAction: () -> Unit) {
                    Row(modifier = Modifier.clickable { onAction() }) {
                        Text("Open")
                    }
                }
            """),
        )
    }

    fun testBoxWithNoLabel() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:21: Warning: [O-02] Clickable Box has no onClickLabel, so screen readers only announce "double tap to activate" with nothing describing what it does [ComposeMissingOnClickLabel]
                    Box(modifier = Modifier.clickable { onAction() }) {
                                            ~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onAction: () -> Unit) {
                    Box(modifier = Modifier.clickable { onAction() }) {
                        Text("Open")
                    }
                }
            """),
        )
    }

    fun testCardWithNoLabel() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:21: Warning: [O-02] Clickable Card has no onClickLabel, so screen readers only announce "double tap to activate" with nothing describing what it does [ComposeMissingOnClickLabel]
                    Card(modifier = Modifier.clickable { onAction() }) {
                                             ~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onAction: () -> Unit) {
                    Card(modifier = Modifier.clickable { onAction() }) {
                        Text("Open")
                    }
                }
            """),
        )
    }

    fun testListItemWithNoLabel() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:23: Warning: [O-02] Clickable ListItem has no onClickLabel, so screen readers only announce "double tap to activate" with nothing describing what it does [ComposeMissingOnClickLabel]
                        modifier = Modifier.clickable { onAction() },
                                            ~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onAction: () -> Unit) {
                    ListItem(
                        headlineContent = { Text("Open") },
                        modifier = Modifier.clickable { onAction() },
                    )
                }
            """),
        )
    }

    fun testExplicitNullLabel() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:21: Warning: [O-02] Clickable Row has no onClickLabel, so screen readers only announce "double tap to activate" with nothing describing what it does [ComposeMissingOnClickLabel]
                    Row(modifier = Modifier.clickable(onClickLabel = null, onClick = onAction)) {
                                            ~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onAction: () -> Unit) {
                    Row(modifier = Modifier.clickable(onClickLabel = null, onClick = onAction)) {
                        Text("Open")
                    }
                }
            """),
        )
    }

    // Negative tests: must not be flagged.

    fun testLiteralLabelIsClean() = expectClean(screen("""
        @Composable fun Screen(onAction: () -> Unit) {
            Row(modifier = Modifier.clickable(onClickLabel = "Open details", onClick = onAction)) {
                Text("Open")
            }
        }
    """))

    fun testLabelFromVariableIsClean() = expectClean(screen("""
        @Composable fun Screen(onAction: () -> Unit) {
            val label = "Open details"
            Row(modifier = Modifier.clickable(onClickLabel = label, onClick = onAction)) {
                Text("Open")
            }
        }
    """))

    fun testColumnWithLabelIsClean() = expectClean(screen("""
        @Composable fun Screen(onAction: () -> Unit) {
            Column(modifier = Modifier.clickable(onClickLabel = "Open details", onClick = onAction)) {
                Text("Open")
            }
        }
    """))

    fun testNonContainerElementIsClean() = expectClean(screen("""
        @Composable fun Screen(onAction: () -> Unit) {
            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.clickable { onAction() })
        }
    """))

    fun testMaterialButtonIsClean() = expectClean(screen("""
        @Composable fun Screen(onAction: () -> Unit) {
            Button(onClick = onAction) {
                Text("Open")
            }
        }
    """))
}
