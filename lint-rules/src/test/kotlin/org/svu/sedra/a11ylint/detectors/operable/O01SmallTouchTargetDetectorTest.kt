package org.svu.sedra.a11ylint.detectors.operable

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.testing.A11yLintTest

class O01SmallTouchTargetDetectorTest : A11yLintTest() {
    override fun getDetector(): Detector = O01SmallTouchTargetDetector()

    override fun getIssues(): List<Issue> = listOf(O01SmallTouchTargetDetector.ISSUE)

    /** A screen file with the imports every test needs. [body] starts at line 28. */
    private fun screen(body: String): TestFile = kotlin(
        "src/test/pkg/Screen.kt",
        """
package test.pkg

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


""" + body.trimIndent(),
    )

    // Positive tests: must be flagged.

    fun testSizeBeforeClickable() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:30: Error: [O-01] Clickable element is only 20dp wide and 20dp high, smaller than the 48dp minimum touch target [ComposeSmallTouchTarget]
                Box(modifier = Modifier.size(20.dp).clickable { onClose() })
                                        ~~~~~~~~~~~
            1 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun Close(onClose: () -> Unit) {
                    Box(modifier = Modifier.size(20.dp).clickable { onClose() })
                }
                """,
            ),
        )
    }

    fun testSizeAfterClickable() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:30: Error: [O-01] Clickable element is only 32dp wide and 32dp high, smaller than the 48dp minimum touch target [ComposeSmallTouchTarget]
                Box(Modifier.combinedClickable(onLongClick = {}) { onClose() }.size(32.dp))
                                                                               ~~~~~~~~~~~
            1 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun Close(onClose: () -> Unit) {
                    Box(Modifier.combinedClickable(onLongClick = {}) { onClose() }.size(32.dp))
                }
                """,
            ),
        )
    }

    fun testOnlyOneAxisTooSmall() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:34: Error: [O-01] Clickable element is only 40dp wide, smaller than the 48dp minimum touch target [ComposeSmallTouchTarget]
                        .width(40.dp)
                         ~~~~~~~~~~~~
            1 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun Favorite(checked: Boolean, onChange: (Boolean) -> Unit) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Favorite",
                        modifier = Modifier.toggleable(value = checked, onValueChange = onChange)
                            .width(40.dp)
                            .height(56.dp),
                    )
                }
                """,
            ),
        )
    }

    fun testPaddingInsideOuterSizeShrinksTheTarget() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:30: Error: [O-01] Clickable element is only 32dp wide and 32dp high, smaller than the 48dp minimum touch target [ComposeSmallTouchTarget]
                Box(Modifier.size(48.dp).padding(8.dp).clickable { onClose() })
                             ~~~~~~~~~~~
            1 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun Close(onClose: () -> Unit) {
                    Box(Modifier.size(48.dp).padding(8.dp).clickable { onClose() })
                }
                """,
            ),
        )
    }

    fun testSizeFromLocalValue() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:30: Error: [O-01] Clickable element is only 24dp wide and 24dp high, smaller than the 48dp minimum touch target [ComposeSmallTouchTarget]
                val small = Modifier.requiredSize(24.dp)
                                     ~~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun Option(selected: Boolean, onSelect: () -> Unit) {
                    val small = Modifier.requiredSize(24.dp)
                    Box(small.selectable(selected = selected, onClick = onSelect))
                }
                """,
            ),
        )
    }

    // Negative tests: must not be flagged.

    fun testMinimumInteractiveComponentSizeInChain() {
        expectClean(
            screen(
                """
                @Composable
                fun Close(onClose: () -> Unit) {
                    Box(Modifier.minimumInteractiveComponentSize().size(20.dp).clickable { onClose() })
                    Box(Modifier.size(20.dp).clickable { onClose() }.minimumInteractiveComponentSize())
                }
                """,
            ),
        )
    }

    fun testMaterialComponentsEnforceTheMinimum() {
        expectClean(
            screen(
                """
                @Composable
                fun Buttons(onClick: () -> Unit) {
                    IconButton(onClick = onClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                    Button(onClick = onClick, modifier = Modifier.size(32.dp).clickable { onClick() }) {
                        Text("Go")
                    }
                }
                """,
            ),
        )
    }

    fun testSizesThatAreNotLiterals() {
        expectClean(
            screen(
                """
                @Composable
                fun Close(iconSize: Dp, onClose: () -> Unit) {
                    Box(Modifier.size(iconSize).clickable { onClose() })
                    Box(Modifier.fillMaxWidth().height(56.dp).clickable { onClose() })
                    Box(Modifier.size(20.dp).padding(iconSize).clickable { onClose() })
                }
                """,
            ),
        )
    }

    fun testNonInteractiveSmallElement() {
        expectClean(
            screen(
                """
                @Composable
                fun Dot() {
                    Box(Modifier.size(8.dp))
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                """,
            ),
        )
    }

    fun testLargeEnoughTargets() {
        expectClean(
            screen(
                """
                @Composable
                fun Close(onClose: () -> Unit) {
                    Box(Modifier.size(48.dp).clickable { onClose() })
                    Box(Modifier.clickable { onClose() }.padding(14.dp).size(20.dp))
                    Box(Modifier.size(56.dp).clickable { onClose() }.padding(20.dp))
                    Box(Modifier.padding(4.dp).clickable { onClose() }.size(width = 64.dp, height = 48.dp))
                }
                """,
            ),
        )
    }
}
