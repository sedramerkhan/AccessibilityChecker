package org.svu.sedra.a11ylint

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class TemporaryMarkerDetectorTest {
    @Test
    fun markerIsReported() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun a11yTestMarker() = Unit
                    fun sample() {
                        a11yTestMarker()
                    }
                    """,
                ),
            )
            .allowMissingSdk()
            .issues(TemporaryMarkerIssue.issue)
            .run()
            .expectContains("[M0] Temporary marker detected")
    }
}
