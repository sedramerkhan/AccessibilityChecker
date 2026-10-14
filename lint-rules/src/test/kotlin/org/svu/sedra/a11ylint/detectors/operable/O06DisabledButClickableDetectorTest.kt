package org.svu.sedra.a11ylint.detectors.operable

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.testing.A11yLintTest

class O06DisabledButClickableDetectorTest : A11yLintTest() {
    override fun getDetector(): Detector = O06DisabledButClickableDetector()
    override fun getIssues(): List<Issue> = listOf(O06DisabledButClickableDetector.ISSUE)

    private fun screen(body: String) = kotlin(
        "src/test/pkg/Screen.kt",
        """
package test.pkg

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

$body
        """,
    )

    /** A screen for the other click and toggle modifiers. [body] starts at line 15. */
    private fun selectionScreen(body: String) = kotlin(
        "src/test/pkg/Screen.kt",
        """
package test.pkg

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState

$body
        """,
    )

    // Positive tests: must be flagged.
    // No trailing comma before a trailing lambda: Lint's REORDER_ARGUMENTS test mode cannot
    // rewrite that shape (the same note as P-01 and O-04 in DECISIONS).

    fun testGuardReturnsEarly() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:16: Warning: [O-06] The handler checks whether it is enabled but enabled is not passed to clickable, so screen readers still announce the element as enabled [ComposeDisabledButClickable]
                        modifier = Modifier.clickable(onClickLabel = "Send", role = Role.Button) {
                                            ~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(enabled: Boolean, onAction: () -> Unit) {
                    Row(
                        modifier = Modifier.clickable(onClickLabel = "Send", role = Role.Button) {
                            if (!enabled) return@clickable
                            onAction()
                        }
                    ) {
                        Text("Send")
                    }
                }
            """),
        )
    }

    fun testWholeHandlerWrappedInACondition() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:16: Warning: [O-06] The handler checks whether it is enabled but enabled is not passed to clickable, so screen readers still announce the element as enabled [ComposeDisabledButClickable]
                        modifier = Modifier.clickable(onClickLabel = "Send", role = Role.Button) {
                                            ~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(enabled: Boolean, onAction: () -> Unit) {
                    Row(
                        modifier = Modifier.clickable(onClickLabel = "Send", role = Role.Button) {
                            if (enabled) {
                                onAction()
                            }
                        }
                    ) {
                        Text("Send")
                    }
                }
            """),
        )
    }

    fun testGuardWithBraces() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:16: Warning: [O-06] The handler checks whether it is enabled but enabled is not passed to clickable, so screen readers still announce the element as enabled [ComposeDisabledButClickable]
                        modifier = Modifier.clickable(onClickLabel = "Send", role = Role.Button) {
                                            ~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(enabled: Boolean, onAction: () -> Unit) {
                    Row(
                        modifier = Modifier.clickable(onClickLabel = "Send", role = Role.Button) {
                            if (!enabled) {
                                return@clickable
                            }
                            onAction()
                        }
                    ) {
                        Text("Send")
                    }
                }
            """),
        )
    }

    // The guard does not have to be the first statement.
    fun testGuardAfterAnotherStatement() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:16: Warning: [O-06] The handler checks whether it is enabled but enabled is not passed to clickable, so screen readers still announce the element as enabled [ComposeDisabledButClickable]
                        modifier = Modifier.clickable(onClickLabel = "Send", role = Role.Button) {
                                            ~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(enabled: Boolean, onAction: () -> Unit, onTrack: () -> Unit) {
                    Row(
                        modifier = Modifier.clickable(onClickLabel = "Send", role = Role.Button) {
                            onTrack()
                            if (!enabled) return@clickable
                            onAction()
                        }
                    ) {
                        Text("Send")
                    }
                }
            """),
        )
    }


    fun testCombinedClickableIsCheckedToo() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:17: Warning: [O-06] The handler checks whether it is enabled but enabled is not passed to combinedClickable, so screen readers still announce the element as enabled [ComposeDisabledButClickable]
                        modifier = Modifier.combinedClickable(onLongClick = onAction) {
                                            ~~~~~~~~~~~~~~~~~
0 errors, 1 warning
            """,
            selectionScreen("""
                @Composable fun Screen(enabled: Boolean, onAction: () -> Unit) {
                    Row(
                        modifier = Modifier.combinedClickable(onLongClick = onAction) {
                            if (!enabled) return@combinedClickable
                            onAction()
                        }
                    ) {
                        Text("Send")
                    }
                }
            """),
        )
    }

    fun testToggleableIsCheckedToo() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:17: Warning: [O-06] The handler checks whether it is enabled but enabled is not passed to toggleable, so screen readers still announce the element as enabled [ComposeDisabledButClickable]
                        modifier = Modifier.toggleable(value = checked) {
                                            ~~~~~~~~~~
0 errors, 1 warning
            """,
            selectionScreen("""
                @Composable fun Screen(enabled: Boolean, checked: Boolean, onChange: (Boolean) -> Unit) {
                    Row(
                        modifier = Modifier.toggleable(value = checked) {
                            if (!enabled) return@toggleable
                            onChange(it)
                        }
                    ) {
                        Text("Notifications")
                    }
                }
            """),
        )
    }

    fun testSelectableIsCheckedToo() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:17: Warning: [O-06] The handler checks whether it is enabled but enabled is not passed to selectable, so screen readers still announce the element as enabled [ComposeDisabledButClickable]
                        modifier = Modifier.selectable(selected = chosen) {
                                            ~~~~~~~~~~
0 errors, 1 warning
            """,
            selectionScreen("""
                @Composable fun Screen(enabled: Boolean, chosen: Boolean, onPick: () -> Unit) {
                    Row(
                        modifier = Modifier.selectable(selected = chosen) {
                            if (enabled) {
                                onPick()
                            }
                        }
                    ) {
                        Text("Standard delivery")
                    }
                }
            """),
        )
    }

    fun testTriStateToggleableIsCheckedToo() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:17: Warning: [O-06] The handler checks whether it is enabled but enabled is not passed to triStateToggleable, so screen readers still announce the element as enabled [ComposeDisabledButClickable]
                        modifier = Modifier.triStateToggleable(state = state) {
                                            ~~~~~~~~~~~~~~~~~~
0 errors, 1 warning
            """,
            selectionScreen("""
                @Composable fun Screen(enabled: Boolean, state: ToggleableState, onPick: () -> Unit) {
                    Row(
                        modifier = Modifier.triStateToggleable(state = state) {
                            if (!enabled) return@triStateToggleable
                            onPick()
                        }
                    ) {
                        Text("Select all")
                    }
                }
            """),
        )
    }

    // Negative tests: must not be flagged.

    fun testEnabledPassedToClickableIsClean() = expectClean(screen("""
        @Composable fun Screen(enabled: Boolean, onAction: () -> Unit) {
            Row(
                modifier = Modifier.clickable(
                    enabled = enabled,
                    onClickLabel = "Send",
                    role = Role.Button,
                    onClick = onAction
                )
            ) {
                Text("Send")
            }
        }
    """))

    // The guard is about something else, so the element really is enabled.
    fun testUnconditionalHandlerIsClean() = expectClean(screen("""
        @Composable fun Screen(onAction: () -> Unit) {
            Row(
                modifier = Modifier.clickable(onClickLabel = "Send", role = Role.Button) {
                    onAction()
                }
            ) {
                Text("Send")
            }
        }
    """))

    // Several statements, none of them a condition, so there is nothing being guarded.
    fun testSeveralStatementsWithNoConditionIsClean() = expectClean(screen("""
        @Composable fun Screen(onAction: () -> Unit) {
            Row(
                modifier = Modifier.clickable(onClickLabel = "Send", role = Role.Button) {
                    onAction()
                    Unit
                }
            ) {
                Text("Send")
            }
        }
    """))

    // A choice between two actions, so the element is never disabled.
    fun testConditionWithAnElseBranchIsClean() = expectClean(screen("""
        @Composable fun Screen(expanded: Boolean, onOpen: () -> Unit, onClose: () -> Unit) {
            Row(
                modifier = Modifier.clickable(onClickLabel = "Send", role = Role.Button) {
                    if (expanded) {
                        onClose()
                    } else {
                        onOpen()
                    }
                }
            ) {
                Text("Send")
            }
        }
    """))

    fun testEmptyHandlerIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Row(
                modifier = Modifier.clickable(onClickLabel = "Send", role = Role.Button) { }
            ) {
                Text("Send")
            }
        }
    """))

    fun testToggleableWithEnabledIsClean() {
        expectClean(
            selectionScreen("""
                @Composable fun Screen(enabled: Boolean, checked: Boolean, onChange: (Boolean) -> Unit) {
                    Row(
                        modifier = Modifier.toggleable(value = checked, enabled = enabled) {
                            onChange(it)
                        }
                    ) {
                        Text("Notifications")
                    }
                }
            """),
        )
    }

    fun testSelectableWithEnabledIsClean() {
        expectClean(
            selectionScreen("""
                @Composable fun Screen(enabled: Boolean, chosen: Boolean, onPick: () -> Unit) {
                    Row(
                        modifier = Modifier.selectable(selected = chosen, enabled = enabled) {
                            if (enabled) {
                                onPick()
                            }
                        }
                    ) {
                        Text("Standard delivery")
                    }
                }
            """),
        )
    }

    fun testUnguardedSelectionModifiersAreClean() {
        expectClean(
            selectionScreen("""
                @Composable fun Screen(checked: Boolean, onChange: (Boolean) -> Unit, onAction: () -> Unit) {
                    Row(
                        modifier = Modifier.toggleable(value = checked) {
                            onChange(it)
                        }
                    ) {
                        Text("Notifications")
                    }
                    Row(
                        modifier = Modifier.combinedClickable(onLongClick = onAction) {
                            onAction()
                        }
                    ) {
                        Text("Send")
                    }
                }
            """),
        )
    }
}
