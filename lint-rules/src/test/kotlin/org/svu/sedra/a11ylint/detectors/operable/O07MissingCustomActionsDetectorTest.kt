package org.svu.sedra.a11ylint.detectors.operable

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.testing.A11yLintTest

class O07MissingCustomActionsDetectorTest : A11yLintTest() {
    override fun getDetector(): Detector = O07MissingCustomActionsDetector()
    override fun getIssues(): List<Issue> = listOf(O07MissingCustomActionsDetector.ISSUE)

    private fun screen(body: String) = kotlin(
        "src/test/pkg/Screen.kt",
        """
package test.pkg

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics

$body
        """,
    )

    // Positive tests: must be flagged.

    fun testClickableCardWithTwoIconButtons() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:21: Warning: [O-07] Possible missing custom actions: clickable Card holds 2 clickable children but offers no customActions, so screen readers announce one element with only its own action [ComposeMissingCustomActions]
                    Card(onClick = onOpen) {
                    ~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onOpen: () -> Unit, onFavorite: () -> Unit, onShare: () -> Unit) {
                    Card(onClick = onOpen) {
                        Text("Shipping forecast")
                        IconButton(onClick = onFavorite) { Text("Favorite") }
                        IconButton(onClick = onShare) { Text("Share") }
                    }
                }
            """),
        )
    }

    // The buttons are usually wrapped in a layout, so descendants are counted, not direct children.
    fun testClickableRowWithButtonsInsideALayout() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:21: Warning: [O-07] Possible missing custom actions: clickable Row holds 2 clickable children but offers no customActions, so screen readers announce one element with only its own action [ComposeMissingCustomActions]
                    Row(modifier = Modifier.clickable(onClickLabel = "Open", role = Role.Button) { onOpen() }) {
                    ~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onOpen: () -> Unit, onFavorite: () -> Unit, onShare: () -> Unit) {
                    Row(modifier = Modifier.clickable(onClickLabel = "Open", role = Role.Button) { onOpen() }) {
                        Text("Shipping forecast")
                        Column {
                            IconButton(onClick = onFavorite) { Text("Favorite") }
                            IconButton(onClick = onShare) { Text("Share") }
                        }
                    }
                }
            """),
        )
    }

    fun testClickableSurfaceWithThreeChildren() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:21: Warning: [O-07] Possible missing custom actions: clickable Surface holds 3 clickable children but offers no customActions, so screen readers announce one element with only its own action [ComposeMissingCustomActions]
                    Surface(onClick = onOpen) {
                    ~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onOpen: () -> Unit, onFavorite: () -> Unit, onShare: () -> Unit) {
                    Surface(onClick = onOpen) {
                        IconButton(onClick = onFavorite) { Text("Favorite") }
                        IconButton(onClick = onShare) { Text("Share") }
                        IconButton(onClick = onOpen) { Text("Open") }
                    }
                }
            """),
        )
    }

    // A semantics block that sets something else does not count as offering the actions.
    fun testSemanticsWithoutCustomActions() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:21: Warning: [O-07] Possible missing custom actions: clickable Card holds 2 clickable children but offers no customActions, so screen readers announce one element with only its own action [ComposeMissingCustomActions]
                    Card(onClick = onOpen, modifier = Modifier.semantics { role = Role.Button }) {
                    ~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onOpen: () -> Unit, onFavorite: () -> Unit, onShare: () -> Unit) {
                    Card(onClick = onOpen, modifier = Modifier.semantics { role = Role.Button }) {
                        IconButton(onClick = onFavorite) { Text("Favorite") }
                        IconButton(onClick = onShare) { Text("Share") }
                    }
                }
            """),
        )
    }

    // Negative tests: must not be flagged.

    fun testCustomActionsAreOfferedIsClean() = expectClean(screen("""
        @Composable fun Screen(onOpen: () -> Unit, onFavorite: () -> Unit, onShare: () -> Unit) {
            Card(
                onClick = onOpen,
                modifier = Modifier.semantics {
                    customActions = listOf(
                        CustomAccessibilityAction("Add to favourites") { onFavorite(); true },
                        CustomAccessibilityAction("Share") { onShare(); true }
                    )
                }
            ) {
                IconButton(onClick = onFavorite) { Text("Favorite") }
                IconButton(onClick = onShare) { Text("Share") }
            }
        }
    """))

    // One clickable child is not the defect this rule describes.
    fun testOnlyOneClickableChildIsClean() = expectClean(screen("""
        @Composable fun Screen(onOpen: () -> Unit, onFavorite: () -> Unit) {
            Card(onClick = onOpen) {
                Text("Shipping forecast")
                IconButton(onClick = onFavorite) { Text("Favorite") }
            }
        }
    """))

    // The container itself is not clickable, so there is one action per child already.
    fun testNonClickableContainerIsClean() = expectClean(screen("""
        @Composable fun Screen(onFavorite: () -> Unit, onShare: () -> Unit) {
            Card {
                IconButton(onClick = onFavorite) { Text("Favorite") }
                IconButton(onClick = onShare) { Text("Share") }
            }
        }
    """))

    // A Box is not one of the containers CLAUDE.md names for this rule.
    fun testClickableBoxIsClean() = expectClean(screen("""
        @Composable fun Screen(onOpen: () -> Unit, onFavorite: () -> Unit, onShare: () -> Unit) {
            Box(modifier = Modifier.clickable(onClickLabel = "Open", role = Role.Button) { onOpen() }) {
                IconButton(onClick = onFavorite) { Text("Favorite") }
                IconButton(onClick = onShare) { Text("Share") }
            }
        }
    """))

    // The nested clickable Row owns the two buttons, so only the Row is reported: the Card sees
    // one clickable child, the Row itself, and stops there.
    fun testOnlyTheNearestClickableContainerIsReported() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:22: Warning: [O-07] Possible missing custom actions: clickable Row holds 2 clickable children but offers no customActions, so screen readers announce one element with only its own action [ComposeMissingCustomActions]
                        Row(modifier = Modifier.clickable(onClickLabel = "Open", role = Role.Button) { onOpen() }) {
                        ~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(onOpen: () -> Unit, onFavorite: () -> Unit, onShare: () -> Unit) {
                    Card(onClick = onOpen) {
                        Row(modifier = Modifier.clickable(onClickLabel = "Open", role = Role.Button) { onOpen() }) {
                            IconButton(onClick = onFavorite) { Text("Favorite") }
                            IconButton(onClick = onShare) { Text("Share") }
                        }
                    }
                }
            """),
        )
    }
}
