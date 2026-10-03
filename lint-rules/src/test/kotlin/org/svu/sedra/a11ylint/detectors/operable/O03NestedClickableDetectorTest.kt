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
            src/test/pkg/Screen.kt:27: Error: [O-03] Clickable IconButton is nested inside the clickable Card, so screen readers may skip it or announce both as one element [ComposeNestedClickable]
                    IconButton(onClick = onFavorite) {
                    ~~~~~~~~~~
            1 errors, 0 warnings
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
            src/test/pkg/Screen.kt:28: Error: [O-03] Clickable Button is nested inside the clickable Row, so screen readers may skip it or announce both as one element [ComposeNestedClickable]
                        Button(onClick = onBuy) {
                        ~~~~~~
            1 errors, 0 warnings
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
            src/test/pkg/Screen.kt:26: Error: [O-03] Clickable Box is nested inside the clickable Surface, so screen readers may skip it or announce both as one element [ComposeNestedClickable]
                    Box(Modifier.clickable { onMore() }) {
                    ~~~
            1 errors, 0 warnings
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
            src/test/pkg/Screen.kt:26: Error: [O-03] Clickable Card is nested inside the clickable Box, so screen readers may skip it or announce both as one element [ComposeNestedClickable]
                    Card(onClick = onOpen) {
                    ~~~~
            1 errors, 0 warnings
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
            src/test/pkg/Screen.kt:27: Error: [O-03] Clickable IconButton is nested inside the clickable Card, so screen readers may skip it or announce both as one element [ComposeNestedClickable]
                        IconButton(onClick = onFavorite) {
                        ~~~~~~~~~~
            src/test/pkg/Screen.kt:30: Error: [O-03] Clickable IconButton is nested inside the clickable Card, so screen readers may skip it or announce both as one element [ComposeNestedClickable]
                        IconButton(onClick = onShare) {
                        ~~~~~~~~~~
            src/test/pkg/Screen.kt:36: Error: [O-03] Clickable Box is nested inside the clickable Box, so screen readers may skip it or announce both as one element [ComposeNestedClickable]
                    Box(Modifier.clickable { onOpen() }) {
                    ~~~
            src/test/pkg/Screen.kt:37: Error: [O-03] Clickable Button is nested inside the clickable Box, so screen readers may skip it or announce both as one element [ComposeNestedClickable]
                        Button(onClick = onShare) { Text("Share") }
                        ~~~~~~
            4 errors, 0 warnings
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
}
