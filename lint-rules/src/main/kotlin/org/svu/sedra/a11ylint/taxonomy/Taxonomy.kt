package org.svu.sedra.a11ylint.taxonomy

import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.detectors.perceivable.P01MissingContentDescriptionDetector

enum class Pour {
    PERCEIVABLE,
    OPERABLE,
    UNDERSTANDABLE,
    ROBUST,
}

enum class DetectionType {
    STATIC,
    STATIC_LLM,
}

enum class Priority {
    CRITICAL,
    MAJOR,
    MINOR,
}

data class TaxonomyEntry(
    val id: String,
    val issue: Issue,
    val pour: Pour,
    val wcag: List<String>,
    val detection: DetectionType,
    val priority: Priority,
)

/** All taxonomy entries. The single source of truth for rule metadata (registry, Phase 3 mapping). */
object Taxonomy {
    val entries: List<TaxonomyEntry> = listOf(
        TaxonomyEntry(
            id = P01MissingContentDescriptionDetector.TAXONOMY_ID,
            issue = P01MissingContentDescriptionDetector.ISSUE,
            pour = Pour.PERCEIVABLE,
            wcag = listOf("1.1.1"),
            detection = DetectionType.STATIC,
            priority = Priority.CRITICAL,
        ),
    )

    /** Returns the entry for a Lint issue ID, for example `ComposeMissingContentDescription`. */
    fun byIssueId(issueId: String): TaxonomyEntry? = entries.firstOrNull { it.issue.id == issueId }
}
