package org.svu.sedra.a11ylint.detectors.understandable

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.testing.A11yLintTest

class U02MissingStateDescriptionDetectorTest : A11yLintTest() {
    override fun getDetector(): Detector = U02MissingStateDescriptionDetector()

    override fun getIssues(): List<Issue> = listOf(U02MissingStateDescriptionDetector.ISSUE)

    /** A screen file with the imports every test needs. */
    private fun screen(body: String): TestFile = kotlin(
        "src/test/pkg/Screen.kt",
        """
package test.pkg

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription


""" + body.trimIndent(),
    )

    private fun message(state: String) =
        "[U-02] Clickable element toggles $state but exposes no state, so screen readers do not " +
            "announce whether it is on or off [ComposeMissingStateDescription]"

    // Positive tests: must be flagged.

    fun testDelegatedStateFlip() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:25: Error: ${message("expanded")}
                Row(modifier = Modifier.clickable { expanded = !expanded }) {
                                        ~~~~~~~~~
            1 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun Section() {
                    var expanded by remember { mutableStateOf(false) }
                    Row(modifier = Modifier.clickable { expanded = !expanded }) {
                        Text("Details")
                    }
                }
                """,
            ),
        )
    }

    fun testMutableStateValueFlip() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:25: Error: ${message("favorite.value")}
                Box(Modifier.clickable { favorite.value = !favorite.value }) {
                             ~~~~~~~~~
            1 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun Favorite() {
                    val favorite = remember { mutableStateOf(false) }
                    Box(Modifier.clickable { favorite.value = !favorite.value }) {
                        Text("Favorite")
                    }
                }
                """,
            ),
        )
    }

    fun testCallbackWithNegatedValue() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:24: Error: ${message("checked")}
                Row(Modifier.clickable { onCheckedChange(!checked) }) {
                             ~~~~~~~~~
            1 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun Option(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
                    Row(Modifier.clickable { onCheckedChange(!checked) }) {
                        Text("Wi-Fi")
                    }
                }
                """,
            ),
        )
    }

    fun testCombinedClickableWithOtherSemantics() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:28: Error: ${message("muted")}
                        .combinedClickable(onLongClick = {}) {
                         ~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun Mute(log: (String) -> Unit) {
                    var muted by remember { mutableStateOf(false) }
                    Box(
                        Modifier
                            .semantics { contentDescription = "Mute" }
                            .combinedClickable(onLongClick = {}) {
                                log("toggle")
                                muted = !(muted)
                            },
                    )
                }
                """,
            ),
        )
    }

    // Negative tests: must not be flagged.

    fun testStateDescriptionInSemantics() {
        expectClean(
            screen(
                """
                @Composable
                fun Section() {
                    var expanded by remember { mutableStateOf(false) }
                    Row(
                        Modifier
                            .clickable { expanded = !expanded }
                            .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                    ) {
                        Text("Details")
                    }
                }
                """,
            ),
        )
    }

    fun testToggleableModifier() {
        expectClean(
            screen(
                """
                @Composable
                fun Option() {
                    var checked by remember { mutableStateOf(false) }
                    Row(Modifier.toggleable(value = checked, onValueChange = { checked = it })) {
                        Text("Wi-Fi")
                    }
                }
                """,
            ),
        )
    }

    fun testClickWithoutBooleanFlip() {
        expectClean(
            screen(
                """
                @Composable
                fun Item(open: () -> Unit, onCountChange: (Int) -> Unit) {
                    var count by remember { mutableStateOf(0) }
                    var shown by remember { mutableStateOf(false) }
                    Row(Modifier.clickable { open() }) { Text("Open") }
                    Row(Modifier.clickable { count = count + 1 }) { Text("Add") }
                    Row(Modifier.clickable { shown = true }) { Text("Show") }
                    Row(Modifier.clickable { onCountChange(-count) }) { Text("Negate") }
                }
                """,
            ),
        )
    }

    fun testMaterialSwitchExposesState() {
        expectClean(
            screen(
                """
                @Composable
                fun Setting() {
                    var enabled by remember { mutableStateOf(false) }
                    Row {
                        Text("Notifications")
                        Switch(checked = enabled, onCheckedChange = { enabled = !enabled })
                    }
                }
                """,
            ),
        )
    }
}
