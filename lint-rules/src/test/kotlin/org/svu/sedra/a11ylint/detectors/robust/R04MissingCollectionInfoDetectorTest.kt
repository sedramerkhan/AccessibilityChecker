package org.svu.sedra.a11ylint.detectors.robust

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.testing.A11yLintTest

class R04MissingCollectionInfoDetectorTest : A11yLintTest() {
    override fun getDetector(): Detector = R04MissingCollectionInfoDetector()
    override fun getIssues(): List<Issue> = listOf(R04MissingCollectionInfoDetector.ISSUE)

    private fun screen(body: String) = kotlin(
        "src/test/pkg/Screen.kt",
        """
package test.pkg

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics

$body
        """,
    )

    // Positive tests: must be flagged.

    fun testColumnWithForEach() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:19: Warning: [R-04] Column builds a list with a loop but sets no collectionInfo, so screen readers cannot say how many items there are or which one is focused [ComposeMissingCollectionInfo]
                    Column {
                    ~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(items: List<String>) {
                    Column {
                        items.forEach { Text(it) }
                    }
                }
            """),
        )
    }

    fun testRowWithForEachIndexed() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:19: Warning: [R-04] Row builds a list with a loop but sets no collectionInfo, so screen readers cannot say how many items there are or which one is focused [ComposeMissingCollectionInfo]
                    Row {
                    ~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(items: List<String>) {
                    Row {
                        items.forEachIndexed { index, item -> Text(item) }
                    }
                }
            """),
        )
    }

    fun testColumnWithForLoop() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:19: Warning: [R-04] Column builds a list with a loop but sets no collectionInfo, so screen readers cannot say how many items there are or which one is focused [ComposeMissingCollectionInfo]
                    Column {
                    ~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(items: List<String>) {
                    Column {
                        for (item in items) {
                            Text(item)
                        }
                    }
                }
            """),
        )
    }

    // The loop belongs to the innermost container, so only that one is reported.
    fun testOnlyTheInnermostContainerIsReported() {
        expectWarnings(
            """
src/test/pkg/Screen.kt:21: Warning: [R-04] Column builds a list with a loop but sets no collectionInfo, so screen readers cannot say how many items there are or which one is focused [ComposeMissingCollectionInfo]
                        Column {
                        ~~~~~~
0 errors, 1 warning
            """,
            screen("""
                @Composable fun Screen(items: List<String>) {
                    Column {
                        Text("Orders")
                        Column {
                            items.forEach { Text(it) }
                        }
                    }
                }
            """),
        )
    }

    // Negative tests: must not be flagged.

    fun testCollectionInfoOnTheContainerIsClean() = expectClean(screen("""
        @Composable fun Screen(items: List<String>) {
            Column(
                modifier = Modifier.semantics { collectionInfo = CollectionInfo(items.size, 1) }
            ) {
                items.forEach { Text(it) }
            }
        }
    """))

    // Collection semantics set on the items rather than the container also counts.
    fun testCollectionItemInfoOnTheItemsIsClean() = expectClean(screen("""
        @Composable fun Screen(items: List<String>) {
            Column {
                items.forEachIndexed { index, item ->
                    Text(
                        item,
                        modifier = Modifier.semantics {
                            collectionItemInfo = CollectionItemInfo(index, 1, 0, 1)
                        }
                    )
                }
            }
        }
    """))

    // A lazy list provides collection semantics itself.
    fun testLazyColumnIsClean() = expectClean(screen("""
        @Composable fun Screen(items: List<String>) {
            LazyColumn {
                items.forEach { value -> item { Text(value) } }
            }
        }
    """))

    // A Column wrapped around a lazy list is not reported for the lazy list's items.
    fun testColumnAroundALazyListIsClean() = expectClean(screen("""
        @Composable fun Screen(items: List<String>) {
            Column {
                Text("Orders")
                LazyColumn {
                    items.forEach { value -> item { Text(value) } }
                }
            }
        }
    """))

    // A loop that computes something but emits no UI is not a list.
    fun testLoopThatEmitsNoUiIsClean() = expectClean(screen("""
        @Composable fun Screen(items: List<String>) {
            Column {
                var total = 0
                items.forEach { total += it.length }
                Text("Total")
            }
        }
    """))

    fun testColumnWithoutALoopIsClean() = expectClean(screen("""
        @Composable fun Screen() {
            Column {
                Text("First")
                Text("Second")
            }
        }
    """))
}
