package org.svu.sedra.a11ylint.taxonomy

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import java.util.EnumSet

/** Creates Lint issues that follow the project conventions (CLAUDE.md section 5.2). */
object A11yIssues {
    /**
     * Creates an issue in [Category.A11Y] whose Lint severity and priority come from the
     * taxonomy [priority]: Critical is an error with priority 9, Major a warning with priority 6,
     * Minor a warning with priority 3.
     *
     * Pass [id] as a named argument (`id = "Compose..."`): `scripts/check_sample_expectations.py`
     * reads the issue IDs from the sources by that pattern.
     */
    fun create(
        id: String,
        briefDescription: String,
        explanation: String,
        priority: Priority,
        detector: Class<out Detector>,
        scope: EnumSet<Scope> = Scope.JAVA_FILE_SCOPE,
    ): Issue = Issue.create(
        id = id,
        briefDescription = briefDescription,
        explanation = explanation,
        category = Category.A11Y,
        priority = lintPriority(priority),
        severity = severity(priority),
        implementation = Implementation(detector, scope),
    )

    /** Returns the message shown at the reported line, with the required `[ID]` prefix. */
    fun message(taxonomyId: String, text: String): String = "[$taxonomyId] $text"

    /** Lint severity for a taxonomy priority. */
    fun severity(priority: Priority): Severity =
        if (priority == Priority.CRITICAL) Severity.ERROR else Severity.WARNING

    /** Lint priority (1 to 10) for a taxonomy priority. */
    fun lintPriority(priority: Priority): Int = when (priority) {
        Priority.CRITICAL -> 9
        Priority.MAJOR -> 6
        Priority.MINOR -> 3
    }
}
