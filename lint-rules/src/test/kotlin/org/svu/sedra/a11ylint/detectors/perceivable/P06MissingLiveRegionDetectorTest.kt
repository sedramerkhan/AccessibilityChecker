package org.svu.sedra.a11ylint.detectors.perceivable

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.testing.A11yLintTest

class P06MissingLiveRegionDetectorTest : A11yLintTest() {
    override fun getDetector(): Detector = P06MissingLiveRegionDetector()
    override fun getIssues(): List<Issue> = listOf(P06MissingLiveRegionDetector.ISSUE)

    private fun screen(body: String) = kotlin(
        "src/test/pkg/Screen.kt",
        """
package test.pkg

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch

$body
        """,
    )

    // Positive tests: must be flagged.

    fun testAssignedInLaunchedEffect() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:25: Warning: [P-06] Possible live region: this text may change without a direct click, but screen readers are not told to announce the change [ComposeMissingLiveRegion]
                    Text(text = status)
                    ~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen() {
                    var status by remember { mutableStateOf("Loading") }
                    LaunchedEffect(Unit) {
                        status = "Ready"
                    }
                    Text(text = status)
                }
            """),
        )
    }

    fun testAssignedInCoroutineLaunchedFromAClick() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:24: Warning: [P-06] Possible live region: this text may change without a direct click, but screen readers are not told to announce the change [ComposeMissingLiveRegion]
                    Text(text = status)
                    ~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen() {
                    var status by remember { mutableStateOf("Loading") }
                    val scope = rememberCoroutineScope()
                    Button(onClick = { scope.launch { status = "Done" } }) { Text("Go") }
                    Text(text = status)
                }
            """),
        )
    }

    fun testValuePropertyAssignedInLaunchedEffect() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:25: Warning: [P-06] Possible live region: this text may change without a direct click, but screen readers are not told to announce the change [ComposeMissingLiveRegion]
                    Text(text = status.value)
                    ~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen() {
                    val status = remember { mutableStateOf("Loading") }
                    LaunchedEffect(Unit) {
                        status.value = "Ready"
                    }
                    Text(text = status.value)
                }
            """),
        )
    }

    fun testAssignedInNonClickCallback() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:24: Warning: [P-06] Possible live region: this text may change without a direct click, but screen readers are not told to announce the change [ComposeMissingLiveRegion]
                    Text(text = status)
                    ~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen() {
                    var status by remember { mutableStateOf("Loading") }
                    fun refresh(callback: (String) -> Unit) { callback("Ready") }
                    refresh { status = it }
                    Text(text = status)
                }
            """),
        )
    }

    // Negative tests: must not be flagged.

    fun testAssignedOnlyInsideClickHandlerIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            var status by remember { mutableStateOf("Idle") }
            Button(onClick = { status = "Clicked" }) { Text("Go") }
            Text(text = status)
        }
    """))

    fun testValuePropertyAssignedOnlyInsideClickHandlerIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            val status = remember { mutableStateOf("Idle") }
            Button(onClick = { status.value = "Clicked" }) { Text("Go") }
            Text(text = status.value)
        }
    """))

    fun testTextWithLiveRegionIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            var status by remember { mutableStateOf("Loading") }
            LaunchedEffect(Unit) { status = "Ready" }
            Text(text = status, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
    """))

    fun testNoAssignmentFoundIsClean() = expectClean(screen("""
        @Composable fun Screen(status: String) {
            Text(text = status)
        }
    """))

    fun testNotAMutableStateVariableIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            var status = "Loading"
            LaunchedEffect(Unit) { status = "Ready" }
            Text(text = status)
        }
    """))

    fun testLiteralTextIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Text(text = "Static label")
        }
    """))
}
