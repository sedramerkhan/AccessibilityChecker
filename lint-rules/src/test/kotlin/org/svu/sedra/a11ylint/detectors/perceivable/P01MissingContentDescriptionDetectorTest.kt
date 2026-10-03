package org.svu.sedra.a11ylint.detectors.perceivable

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.testing.A11yLintTest

class P01MissingContentDescriptionDetectorTest : A11yLintTest() {
    override fun getDetector(): Detector = P01MissingContentDescriptionDetector()

    override fun getIssues(): List<Issue> = listOf(P01MissingContentDescriptionDetector.ISSUE)

    /** A screen file with the imports every test needs. [body] starts at line 26. */
    private fun screen(body: String): TestFile = kotlin(
        "src/test/pkg/Screen.kt",
        """
package test.pkg

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics


""" + body.trimIndent(),
    )

    // Positive tests: must be flagged.

    fun testNullDescriptionInsideIconButton() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:29: Error: [P-01] Interactive Icon has no contentDescription, so screen readers announce it without a name [ComposeMissingContentDescription]
                    Icon(Icons.Filled.Delete, contentDescription = null)
                                                                   ~~~~
            1 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun DeleteButton(onDelete: () -> Unit) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                    }
                }
                """,
            ),
        )
    }

    fun testEmptyDescriptionCountsAsMissing() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:29: Error: [P-01] Interactive Icon has no contentDescription, so screen readers announce it without a name [ComposeMissingContentDescription]
                    Icon(Icons.Filled.Delete, "")
                                              ~~
            1 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun DeleteButton(onDelete: () -> Unit) {
                    IconButton(onDelete) {
                        Icon(Icons.Filled.Delete, "")
                    }
                }
                """,
            ),
        )
    }

    fun testClickableModifierOnTheIconItself() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:30: Error: [P-01] Interactive Icon has no contentDescription, so screen readers announce it without a name [ComposeMissingContentDescription]
                    contentDescription = null,
                                         ~~~~
            1 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun CloseIcon(onClose: () -> Unit) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.clickable { onClose() },
                    )
                }
                """,
            ),
        )
    }

    fun testToggleableImage() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:30: Error: [P-01] Interactive Image has no contentDescription, so screen readers announce it without a name [ComposeMissingContentDescription]
                    contentDescription = null,
                                         ~~~~
            1 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun Favorite(checked: Boolean, onChange: (Boolean) -> Unit) {
                    Image(
                        painter = painterResource(1),
                        contentDescription = null,
                        modifier = Modifier.toggleable(value = checked, onValueChange = onChange),
                    )
                }
                """,
            ),
        )
    }

    fun testNestedInsideClickableBoxAndCard() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:30: Error: [P-01] Interactive Icon has no contentDescription, so screen readers announce it without a name [ComposeMissingContentDescription]
                        Icon(Icons.Filled.Delete, null)
                                                  ~~~~
            src/test/pkg/Screen.kt:34: Error: [P-01] Interactive Icon has no contentDescription, so screen readers announce it without a name [ComposeMissingContentDescription]
                    Icon(Icons.Filled.Close, null)
                                             ~~~~
            2 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun Actions(onDelete: () -> Unit, onClose: () -> Unit) {
                    Box(modifier = Modifier.clickable { onDelete() }) {
                        Row {
                            Icon(Icons.Filled.Delete, null)
                        }
                    }
                    Card(onClick = onClose) {
                        Icon(Icons.Filled.Close, null)
                    }
                }
                """,
            ),
        )
    }

    fun testFloatingActionButtonWithUnlabeledIcon() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:29: Error: [P-01] Interactive Icon has no contentDescription, so screen readers announce it without a name [ComposeMissingContentDescription]
                    Icon(Icons.Filled.Delete, contentDescription = null)
                                                                   ~~~~
            1 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun DeleteFab(onDelete: () -> Unit) {
                    FloatingActionButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                    }
                }
                """,
            ),
        )
    }

    // Negative tests: must not be flagged.

    fun testDecorativeIconInButtonWithText() {
        expectClean(
            screen(
                """
                @Composable
                fun DeleteButton(onDelete: () -> Unit) {
                    Button(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Text("Delete")
                    }
                }
                """,
            ),
        )
    }

    fun testNonInteractiveDecorativeImage() {
        expectClean(
            screen(
                """
                @Composable
                fun Header() {
                    Row {
                        Image(painterResource(1), contentDescription = null)
                        Icon(Icons.Filled.Delete, "")
                        Text("Trash")
                    }
                    Card {
                        Icon(Icons.Filled.Close, null)
                    }
                }
                """,
            ),
        )
    }

    fun testDescriptionFromResourceOrExpression() {
        expectClean(
            screen(
                """
                @Composable
                fun Buttons(label: String?, onClick: () -> Unit) {
                    IconButton(onClick = onClick) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(1))
                    }
                    IconButton(onClick = onClick) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete draft")
                    }
                    IconButton(onClick = onClick) {
                        Icon(Icons.Filled.Delete, contentDescription = label)
                    }
                }
                """,
            ),
        )
    }

    fun testLabelFromSemanticsOnParentOrIcon() {
        expectClean(
            screen(
                """
                @Composable
                fun Buttons(onClick: () -> Unit) {
                    IconButton(
                        onClick = onClick,
                        modifier = Modifier.semantics { contentDescription = "Delete draft" }
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                    }
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.clickable { onClick() }.semantics { contentDescription = "Close" },
                    )
                }
                """,
            ),
        )
    }

    fun testOtherLabeledContentInClickableParent() {
        expectClean(
            screen(
                """
                @Composable
                fun Rows(onClick: () -> Unit) {
                    IconButton(onClick = onClick) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        Icon(Icons.Filled.Close, contentDescription = null)
                    }
                    Box(Modifier.clickable { onClick() }) {
                        Row {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                            Text(text = "Delete")
                        }
                    }
                }
                """,
            ),
        )
    }

    fun testClickableParentInAnotherFunctionIsNotFollowed() {
        expectClean(
            screen(
                """
                @Composable
                fun DeleteIcon() {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                }

                @Composable
                fun DeleteButton(onDelete: () -> Unit) {
                    IconButton(onClick = onDelete) {
                        DeleteIcon()
                    }
                }
                """,
            ),
        )
    }
}
