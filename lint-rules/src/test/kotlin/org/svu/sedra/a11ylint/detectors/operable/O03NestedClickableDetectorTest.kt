package org.svu.sedra.a11ylint.detectors.operable

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.testing.A11yLintTest

class O03NestedClickableDetectorTest : A11yLintTest() {
    override fun getDetector(): Detector = O03NestedClickableDetector()

    override fun getIssues(): List<Issue> = listOf(O03NestedClickableDetector.ISSUE)

    /** A screen file with the imports every test needs. [body] starts at line 23. */
    private fun screen(body: String): TestFile = kotlin(
        "src/test/pkg/Screen.kt",
        """
package test.pkg

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


""" + body.trimIndent(),
    )

    // Positive tests: must be flagged.

    fun testIconButtonInsideClickableCard() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:27: Warning: [O-03] Clickable IconButton is nested inside the clickable Card, so their touch targets overlap and screen readers announce them as two separate stops [ComposeNestedClickable]
                    IconButton(onClick = onFavorite) {
                    ~~~~~~~~~~
            0 errors, 1 warnings
            """,
            screen(
                """
                @Composable
                fun Article(onOpen: () -> Unit, onFavorite: () -> Unit) {
                    Card(onClick = onOpen) {
                        Text("Title")
                        IconButton(onClick = onFavorite) {
                            Icon(Icons.Filled.Favorite, contentDescription = "Favorite")
                        }
                    }
                }
                """,
            ),
        )
    }

    fun testButtonNestedThroughLayouts() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:28: Warning: [O-03] Clickable Button is nested inside the clickable Row, so their touch targets overlap and screen readers announce them as two separate stops [ComposeNestedClickable]
                        Button(onClick = onBuy) {
                        ~~~~~~
            0 errors, 1 warnings
            """,
            screen(
                """
                @Composable
                fun Product(onOpen: () -> Unit, onBuy: () -> Unit) {
                    Row(modifier = Modifier.clickable { onOpen() }) {
                        Column {
                            Text("Shoes")
                            Button(onClick = onBuy) {
                                Text("Buy")
                            }
                        }
                    }
                }
                """,
            ),
        )
    }

    fun testClickableBoxInsideClickableSurface() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:26: Warning: [O-03] Clickable Box is nested inside the clickable Surface, so their touch targets overlap and screen readers announce them as two separate stops [ComposeNestedClickable]
                    Box(Modifier.clickable { onMore() }) {
                    ~~~
            0 errors, 1 warnings
            """,
            screen(
                """
                @Composable
                fun Item(onOpen: () -> Unit, onMore: () -> Unit) {
                    Surface(onClick = onOpen) {
                        Box(Modifier.clickable { onMore() }) {
                            Text("More")
                        }
                    }
                }
                """,
            ),
        )
    }

    fun testClickableCardInsideToggleableBox() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:26: Warning: [O-03] Clickable Card is nested inside the clickable Box, so their touch targets overlap and screen readers announce them as two separate stops [ComposeNestedClickable]
                    Card(onClick = onOpen) {
                    ~~~~
            0 errors, 1 warnings
            """,
            screen(
                """
                @Composable
                fun Selectable(selected: Boolean, onSelect: (Boolean) -> Unit, onOpen: () -> Unit) {
                    Box(Modifier.toggleable(value = selected, onValueChange = onSelect)) {
                        Card(onClick = onOpen) {
                            Text("Details")
                        }
                    }
                }
                """,
            ),
        )
    }

    fun testEveryNestedClickableIsReported() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:27: Warning: [O-03] Clickable IconButton is nested inside the clickable Card, so their touch targets overlap and screen readers announce them as two separate stops [ComposeNestedClickable]
                        IconButton(onClick = onFavorite) {
                        ~~~~~~~~~~
            src/test/pkg/Screen.kt:30: Warning: [O-03] Clickable IconButton is nested inside the clickable Card, so their touch targets overlap and screen readers announce them as two separate stops [ComposeNestedClickable]
                        IconButton(onClick = onShare) {
                        ~~~~~~~~~~
            src/test/pkg/Screen.kt:36: Warning: [O-03] Clickable Box is nested inside the clickable Box, so their touch targets overlap and screen readers announce them as two separate stops [ComposeNestedClickable]
                    Box(Modifier.clickable { onOpen() }) {
                    ~~~
            src/test/pkg/Screen.kt:37: Warning: [O-03] Clickable Button is nested inside the clickable Box, so their touch targets overlap and screen readers announce them as two separate stops [ComposeNestedClickable]
                        Button(onClick = onShare) { Text("Share") }
                        ~~~~~~
            0 errors, 4 warnings
            """,
            screen(
                """
                @Composable
                fun Article(onOpen: () -> Unit, onFavorite: () -> Unit, onShare: () -> Unit) {
                    Card(onClick = onOpen) {
                        Row {
                            IconButton(onClick = onFavorite) {
                                Icon(Icons.Filled.Favorite, contentDescription = "Favorite")
                            }
                            IconButton(onClick = onShare) {
                                Icon(Icons.Filled.Share, contentDescription = "Share")
                            }
                        }
                    }
                    Box(Modifier.clickable { onOpen() }) {
                        Box(Modifier.clickable { onOpen() }) {
                            Button(onClick = onShare) { Text("Share") }
                        }
                    }
                }
                """,
            ),
        )
    }

    // Negative tests: must not be flagged.

    fun testSiblingClickables() {
        expectClean(
            screen(
                """
                @Composable
                fun Actions(onOpen: () -> Unit, onShare: () -> Unit) {
                    Column {
                        Card(onClick = onOpen) { Text("Open") }
                        Button(onClick = onShare) { Text("Share") }
                        Box(Modifier.clickable { onOpen() }) { Text("More") }
                    }
                }
                """,
            ),
        )
    }

    fun testNonClickableContainers() {
        expectClean(
            screen(
                """
                @Composable
                fun Article(onFavorite: () -> Unit) {
                    Card {
                        Text("Title")
                        IconButton(onClick = onFavorite) {
                            Icon(Icons.Filled.Favorite, contentDescription = "Favorite")
                        }
                    }
                    Surface {
                        Button(onClick = onFavorite) { Text("Like") }
                    }
                }
                """,
            ),
        )
    }

    fun testButtonContentIsNotClickable() {
        expectClean(
            screen(
                """
                @Composable
                fun ShareButton(onShare: () -> Unit) {
                    Button(onClick = onShare) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Text("Share")
                    }
                }
                """,
            ),
        )
    }

    fun testClickableInAnotherComposableIsNotFollowed() {
        expectClean(
            screen(
                """
                @Composable
                fun FavoriteButton(onFavorite: () -> Unit) {
                    IconButton(onClick = onFavorite) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Favorite")
                    }
                }

                @Composable
                fun Article(onOpen: () -> Unit, onFavorite: () -> Unit) {
                    Card(onClick = onOpen) {
                        Text("Title")
                        FavoriteButton(onFavorite)
                    }
                }
                """,
            ),
        )
    }

    fun testClickableItemsInsideNonClickableList() {
        expectClean(
            screen(
                """
                @Composable
                fun Feed(onOpen: () -> Unit) {
                    Column {
                        LazyColumn {
                            item {
                                Card(onClick = onOpen) { Text("First") }
                            }
                        }
                    }
                }
                """,
            ),
        )
    }

    // Material selection controls. A Checkbox, TriStateCheckbox, Switch or RadioButton installs
    // its own toggle modifier only when its callback is non-null, so the callback decides
    // whether it is a second touch target inside a clickable row. These tests use their own
    // screen helper so the import block of the screen helper above, and the line numbers every
    // test there depends on, do not change.

    /** A settings-row file with the selection control imports. [body] starts at line 21. */
    private fun controlScreen(body: String): TestFile = kotlin(
        "src/test/pkg/Settings.kt",
        """
package test.pkg

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState


""" + body.trimIndent(),
    )

    // Positive tests: must be flagged.

    fun testCheckboxWithCallbackInsideToggleableRow() {
        expectWarnings(
            """
            src/test/pkg/Settings.kt:25: Warning: [O-03] Checkbox handles its own clicks inside the clickable Row, so the two become separate targets and screen readers announce the label and the state as different elements [ComposeNestedClickable]
                    Checkbox(checked = checked, onCheckedChange = onCheckedChange)
                    ~~~~~~~~
            0 errors, 1 warnings
            """,
            controlScreen(
                """
                @Composable
                fun NotificationRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
                    Row(Modifier.toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)) {
                        Text("Notifications")
                        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
                    }
                }
                """,
            ),
        )
    }

    fun testSwitchWithCallbackInsideClickableRow() {
        expectWarnings(
            """
            src/test/pkg/Settings.kt:25: Warning: [O-03] Switch handles its own clicks inside the clickable Row, so the two become separate targets and screen readers announce the label and the state as different elements [ComposeNestedClickable]
                    Switch(checked = checked, onCheckedChange = onToggle)
                    ~~~~~~
            0 errors, 1 warnings
            """,
            controlScreen(
                """
                @Composable
                fun WifiRow(checked: Boolean, onToggle: (Boolean) -> Unit) {
                    Row(Modifier.clickable { onToggle(!checked) }) {
                        Text("Wi-Fi")
                        Switch(checked = checked, onCheckedChange = onToggle)
                    }
                }
                """,
            ),
        )
    }

    fun testRadioButtonWithCallbackInsideSelectableRow() {
        expectWarnings(
            """
            src/test/pkg/Settings.kt:25: Warning: [O-03] RadioButton handles its own clicks inside the clickable Row, so the two become separate targets and screen readers announce the label and the state as different elements [ComposeNestedClickable]
                    RadioButton(selected = selected, onClick = onSelect)
                    ~~~~~~~~~~~
            0 errors, 1 warnings
            """,
            controlScreen(
                """
                @Composable
                fun DeliveryRow(selected: Boolean, onSelect: () -> Unit) {
                    Row(Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)) {
                        Text("Standard delivery")
                        RadioButton(selected = selected, onClick = onSelect)
                    }
                }
                """,
            ),
        )
    }

    fun testTriStateCheckboxWithCallbackInsideClickableCard() {
        expectWarnings(
            """
            src/test/pkg/Settings.kt:25: Warning: [O-03] TriStateCheckbox handles its own clicks inside the clickable Card, so the two become separate targets and screen readers announce the label and the state as different elements [ComposeNestedClickable]
                    TriStateCheckbox(state = state, onClick = onToggle)
                    ~~~~~~~~~~~~~~~~
            0 errors, 1 warnings
            """,
            controlScreen(
                """
                @Composable
                fun SelectAllRow(state: ToggleableState, onToggle: () -> Unit) {
                    Card(onClick = onToggle) {
                        Text("Select all")
                        TriStateCheckbox(state = state, onClick = onToggle)
                    }
                }
                """,
            ),
        )
    }

    // Negative tests: must not be flagged.

    fun testCheckboxWithNullCallbackInsideToggleableRow() {
        // The pattern the Compose accessibility guidance recommends: the row owns the toggle
        // and the checkbox only draws the state, so the two merge into one announced control.
        expectClean(
            controlScreen(
                """
                @Composable
                fun NotificationRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
                    Row(Modifier.toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)) {
                        Text("Notifications")
                        Checkbox(checked = checked, onCheckedChange = null)
                    }
                }
                """,
            ),
        )
    }

    fun testSwitchWithNullCallbackInsideClickableRow() {
        expectClean(
            controlScreen(
                """
                @Composable
                fun WifiRow(checked: Boolean, onToggle: (Boolean) -> Unit) {
                    Row(Modifier.clickable { onToggle(!checked) }) {
                        Text("Wi-Fi")
                        Switch(checked = checked, onCheckedChange = null)
                    }
                }
                """,
            ),
        )
    }

    fun testRadioButtonWithNullCallbackInsideSelectableRow() {
        expectClean(
            controlScreen(
                """
                @Composable
                fun DeliveryRow(selected: Boolean, onSelect: () -> Unit) {
                    Row(Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)) {
                        Text("Standard delivery")
                        RadioButton(selected = selected, onClick = null)
                    }
                }
                """,
            ),
        )
    }

    fun testInteractiveControlsOutsideAnyClickable() {
        // A selection control that owns its own interaction is correct when nothing around it
        // is clickable. This is the ordinary standalone case and must stay clean.
        expectClean(
            controlScreen(
                """
                @Composable
                fun Preferences(
                    checked: Boolean,
                    onCheckedChange: (Boolean) -> Unit,
                    selected: Boolean,
                    onSelect: () -> Unit,
                ) {
                    Column {
                        Text("Notifications")
                        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
                        Switch(checked = checked, onCheckedChange = onCheckedChange)
                        RadioButton(selected = selected, onClick = onSelect)
                    }
                }
                """,
            ),
        )
    }
}
