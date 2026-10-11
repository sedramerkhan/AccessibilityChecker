package org.svu.sedra.a11ylint.detectors.understandable

import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.testing.A11yLintTest

class U06InconsistentLabelsDetectorTest : A11yLintTest() {
    override fun getDetector(): Detector = U06InconsistentLabelsDetector()
    override fun getIssues(): List<Issue> = listOf(U06InconsistentLabelsDetector.ISSUE)

    /** A minimal `R` so drawable references resolve to a real resource. */
    private val rSource = java(
        "src/test/pkg/R.java",
        """
        package test.pkg;
        public final class R {
            public static final class drawable {
                public static final int ic_bin = 0x7f020000;
            }
        }
        """,
    ).indented()

    private fun screen(name: String, body: String) = kotlin(
        "src/test/pkg/$name.kt",
        """
package test.pkg

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

$body
        """,
    )

    // Positive tests: must be flagged.

    fun testSameIconLabelledDifferentlyInTwoFiles() {
        expectWarnings(
            """
src/test/pkg/First.kt:14: Warning: [U-06] Possible inconsistent label: Icons.Filled.Delete is described as "Delete" here but as "Remove item" elsewhere, so the same icon is announced in different words [ComposeInconsistentLabels]
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                                                    ~~~~~~~~
src/test/pkg/Second.kt:14: Warning: [U-06] Possible inconsistent label: Icons.Filled.Delete is described as "Remove item" here but as "Delete" elsewhere, so the same icon is announced in different words [ComposeInconsistentLabels]
                    Icon(Icons.Default.Delete, contentDescription = "Remove item")
                                                                    ~~~~~~~~~~~~~
0 errors, 2 warnings
            """,
            screen("First", """
                @Composable fun First() {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            """),
            screen("Second", """
                @Composable fun Second() {
                    Icon(Icons.Default.Delete, contentDescription = "Remove item")
                }
            """),
        )
    }

    // Icons.Default is an alias for Icons.Filled, so the two spellings are the same icon.
    fun testDefaultAndFilledAreTheSameIcon() {
        expectWarnings(
            """
src/test/pkg/First.kt:14: Warning: [U-06] Possible inconsistent label: Icons.Filled.Share is described as "Send to" here but as "Share" elsewhere, so the same icon is announced in different words [ComposeInconsistentLabels]
                    Icon(Icons.Filled.Share, contentDescription = "Send to")
                                                                  ~~~~~~~~~
src/test/pkg/Second.kt:14: Warning: [U-06] Possible inconsistent label: Icons.Filled.Share is described as "Share" here but as "Send to" elsewhere, so the same icon is announced in different words [ComposeInconsistentLabels]
                    Icon(Icons.Default.Share, contentDescription = "Share")
                                                                   ~~~~~~~
0 errors, 2 warnings
            """,
            screen("First", """
                @Composable fun First() {
                    Icon(Icons.Filled.Share, contentDescription = "Send to")
                }
            """),
            screen("Second", """
                @Composable fun Second() {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
            """),
        )
    }

    // Two labels in the same file are just as inconsistent.
    fun testTwoLabelsInOneFile() {
        expectWarnings(
            """
src/test/pkg/First.kt:14: Warning: [U-06] Possible inconsistent label: Icons.Filled.Delete is described as "Delete" here but as "Remove item" elsewhere, so the same icon is announced in different words [ComposeInconsistentLabels]
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                                                    ~~~~~~~~
src/test/pkg/First.kt:15: Warning: [U-06] Possible inconsistent label: Icons.Filled.Delete is described as "Remove item" here but as "Delete" elsewhere, so the same icon is announced in different words [ComposeInconsistentLabels]
                    Icon(Icons.Default.Delete, contentDescription = "Remove item")
                                                                    ~~~~~~~~~~~~~
0 errors, 2 warnings
            """,
            screen("First", """
                @Composable fun First() {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                    Icon(Icons.Default.Delete, contentDescription = "Remove item")
                }
            """),
        )
    }

    // Three spellings: every occurrence names all the others.
    fun testThreeDifferentLabels() {
        expectWarnings(
            """
src/test/pkg/First.kt:14: Warning: [U-06] Possible inconsistent label: Icons.Filled.Delete is described as "Delete" here but as "Erase", "Remove item" elsewhere, so the same icon is announced in different words [ComposeInconsistentLabels]
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                                                    ~~~~~~~~
src/test/pkg/First.kt:15: Warning: [U-06] Possible inconsistent label: Icons.Filled.Delete is described as "Erase" here but as "Delete", "Remove item" elsewhere, so the same icon is announced in different words [ComposeInconsistentLabels]
                    Icon(Icons.Default.Delete, contentDescription = "Erase")
                                                                    ~~~~~~~
src/test/pkg/Second.kt:14: Warning: [U-06] Possible inconsistent label: Icons.Filled.Delete is described as "Remove item" here but as "Delete", "Erase" elsewhere, so the same icon is announced in different words [ComposeInconsistentLabels]
                    Icon(Icons.Default.Delete, contentDescription = "Remove item")
                                                                    ~~~~~~~~~~~~~
0 errors, 3 warnings
            """,
            screen("First", """
                @Composable fun First() {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                    Icon(Icons.Default.Delete, contentDescription = "Erase")
                }
            """),
            screen("Second", """
                @Composable fun Second() {
                    Icon(Icons.Default.Delete, contentDescription = "Remove item")
                }
            """),
        )
    }

    // A drawable read at the call site is identified by its resource, not only Icons.X.Y
    // (widened with Sedra on 2026-10-08: compose-samples never uses Icons.X.Y).
    fun testSameDrawableLabelledDifferently() {
        expectWarnings(
            """
src/test/pkg/First.kt:14: Warning: [U-06] Possible inconsistent label: @drawable/ic_bin is described as "Delete" here but as "Remove item" elsewhere, so the same icon is announced in different words [ComposeInconsistentLabels]
                    Icon(painterResource(R.drawable.ic_bin), contentDescription = "Delete")
                                                                                  ~~~~~~~~
src/test/pkg/Second.kt:14: Warning: [U-06] Possible inconsistent label: @drawable/ic_bin is described as "Remove item" here but as "Delete" elsewhere, so the same icon is announced in different words [ComposeInconsistentLabels]
                    Icon(painterResource(R.drawable.ic_bin), contentDescription = "Remove item")
                                                                                  ~~~~~~~~~~~~~
0 errors, 2 warnings
            """,
            rSource,
            screen("First", """
                @Composable fun First() {
                    Icon(painterResource(R.drawable.ic_bin), contentDescription = "Delete")
                }
            """),
            screen("Second", """
                @Composable fun Second() {
                    Icon(painterResource(R.drawable.ic_bin), contentDescription = "Remove item")
                }
            """),
        )
    }

    // Negative tests: must not be flagged.

    // One local often stands in for several icons, so a local is not followed.
    fun testIconHeldInALocalIsClean() = expectClean(
        rSource,
        screen("First", """
            @Composable fun First() {
                val icon = painterResource(R.drawable.ic_bin)
                Icon(icon, contentDescription = "Delete")
                Icon(icon, contentDescription = "Share")
            }
        """),
    )

    fun testSameIconSameLabelIsClean() = expectClean(
        screen("First", """
            @Composable fun First() {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        """),
        screen("Second", """
            @Composable fun Second() {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        """),
    )

    // Different icons may of course have different labels.
    fun testDifferentIconsAreClean() = expectClean(
        screen("First", """
            @Composable fun First() {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        """),
        screen("Second", """
            @Composable fun Second() {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }
        """),
    )

    // A description that is not a literal cannot be compared.
    fun testStringResourceIsClean() = expectClean(
        screen("First", """
            @Composable fun First() {
                Icon(Icons.Default.Delete, contentDescription = stringResource(1))
            }
        """),
        screen("Second", """
            @Composable fun Second() {
                Icon(Icons.Default.Delete, contentDescription = "Remove item")
            }
        """),
    )

    // A single use cannot be inconsistent with anything.
    fun testOneUseIsClean() = expectClean(
        screen("First", """
            @Composable fun First() {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        """),
    )

    fun testNullDescriptionsAreClean() = expectClean(
        screen("First", """
            @Composable fun First() {
                Icon(Icons.Default.Delete, contentDescription = null)
            }
        """),
        screen("Second", """
            @Composable fun Second() {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        """),
    )
}
