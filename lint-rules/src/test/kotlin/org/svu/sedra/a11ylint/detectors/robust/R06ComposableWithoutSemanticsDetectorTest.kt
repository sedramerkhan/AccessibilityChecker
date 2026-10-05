package org.svu.sedra.a11ylint.detectors.robust

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.testing.A11yLintTest

class R06ComposableWithoutSemanticsDetectorTest : A11yLintTest() {
    override fun getDetector(): Detector = R06ComposableWithoutSemanticsDetector()
    override fun getIssues(): List<Issue> = listOf(R06ComposableWithoutSemanticsDetector.ISSUE)

    private fun screen(body: String) = kotlin(
        "src/test/pkg/Screen.kt",
        """
package test.pkg

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

$body
        """,
    )

    // Positive tests: must be flagged.

    fun testPointerInputWithNoSemantics() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:23: Warning: [R-06] Possible missing semantics: ColourSwatch handles input with a custom gesture but sets no semantics, so screen readers have no action to offer [ComposeComposableWithoutSemantics]
                fun ColourSwatch(onPick: () -> Unit) {
                    ~~~~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable
                fun ColourSwatch(onPick: () -> Unit) {
                    Box(modifier = Modifier.pointerInput(Unit) { detectTapGestures(onTap = { onPick() }) })
                }
            """),
        )
    }

    fun testDraggableWithNoSemantics() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:23: Warning: [R-06] Possible missing semantics: VolumeSlider handles input with a custom gesture but sets no semantics, so screen readers have no action to offer [ComposeComposableWithoutSemantics]
                fun VolumeSlider(state: DraggableState) {
                    ~~~~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable
                fun VolumeSlider(state: DraggableState) {
                    Box(modifier = Modifier.draggable(state, Orientation.Horizontal))
                }
            """),
        )
    }

    fun testGestureNestedInALayout() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:23: Warning: [R-06] Possible missing semantics: RatingStars handles input with a custom gesture but sets no semantics, so screen readers have no action to offer [ComposeComposableWithoutSemantics]
                fun RatingStars(onRate: () -> Unit) {
                    ~~~~~~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable
                fun RatingStars(onRate: () -> Unit) {
                    Column {
                        Text("Rate this")
                        Box(modifier = Modifier.pointerInput(Unit) { detectTapGestures(onTap = { onRate() }) })
                    }
                }
            """),
        )
    }

    // Negative tests: must not be flagged.

    fun testGestureWithSemanticsIsClean() = expectClean(screen("""
        @Composable
        fun ColourSwatch(onPick: () -> Unit) {
            Box(
                modifier = Modifier
                    .pointerInput(Unit) { detectTapGestures(onTap = { onPick() }) }
                    .semantics { contentDescription = "Pick this colour" }
            )
        }
    """))

    // A clickable already carries an action, and is covered by R-01, O-02 and U-02.
    fun testGestureWithClickableIsClean() = expectClean(screen("""
        @Composable
        fun ColourSwatch(onPick: () -> Unit) {
            Box(
                modifier = Modifier
                    .pointerInput(Unit) { detectTapGestures(onTap = { onPick() }) }
                    .clickable(role = Role.Button) { onPick() }
            )
        }
    """))

    fun testGestureWithToggleableIsClean() = expectClean(screen("""
        @Composable
        fun ColourSwatch(picked: Boolean, onPick: (Boolean) -> Unit) {
            Box(
                modifier = Modifier
                    .pointerInput(Unit) { detectTapGestures(onTap = { onPick(!picked) }) }
                    .toggleable(value = picked, onValueChange = onPick)
            )
        }
    """))

    fun testNoGestureIsClean() = expectClean(screen("""
        @Composable
        fun ColourSwatch(onPick: () -> Unit) {
            Box(modifier = Modifier.clickable(role = Role.Button) { onPick() }) {
                Text("Pick this colour")
            }
        }
    """))

    fun testNonComposableFunctionIsClean() = expectClean(screen("""
        fun attachGesture(modifier: Modifier, onPick: () -> Unit): Modifier =
            modifier.pointerInput(Unit) { detectTapGestures(onTap = { onPick() }) }
    """))
}
