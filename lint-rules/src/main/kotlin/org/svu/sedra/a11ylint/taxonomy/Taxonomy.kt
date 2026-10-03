package org.svu.sedra.a11ylint.taxonomy

import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.detectors.operable.O01SmallTouchTargetDetector
import org.svu.sedra.a11ylint.detectors.operable.O03NestedClickableDetector
import org.svu.sedra.a11ylint.detectors.perceivable.P01MissingContentDescriptionDetector
import org.svu.sedra.a11ylint.detectors.understandable.U01MissingHeadingDetector
import org.svu.sedra.a11ylint.detectors.understandable.U02MissingStateDescriptionDetector

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
        TaxonomyEntry(
            id = O01SmallTouchTargetDetector.TAXONOMY_ID,
            issue = O01SmallTouchTargetDetector.ISSUE,
            pour = Pour.OPERABLE,
            wcag = listOf("2.5.8"),
            detection = DetectionType.STATIC,
            priority = Priority.CRITICAL,
        ),
        TaxonomyEntry(
            id = O03NestedClickableDetector.TAXONOMY_ID,
            issue = O03NestedClickableDetector.ISSUE,
            pour = Pour.OPERABLE,
            wcag = listOf("2.4.3", "4.1.2"),
            detection = DetectionType.STATIC,
            priority = Priority.CRITICAL,
        ),
        TaxonomyEntry(
            id = U01MissingHeadingDetector.TAXONOMY_ID,
            issue = U01MissingHeadingDetector.ISSUE,
            pour = Pour.UNDERSTANDABLE,
            wcag = listOf("1.3.1", "2.4.6"),
            detection = DetectionType.STATIC_LLM,
            priority = Priority.CRITICAL,
        ),
        TaxonomyEntry(
            id = U02MissingStateDescriptionDetector.TAXONOMY_ID,
            issue = U02MissingStateDescriptionDetector.ISSUE,
            pour = Pour.UNDERSTANDABLE,
            wcag = listOf("4.1.2"),
            detection = DetectionType.STATIC,
            priority = Priority.CRITICAL,
        ),
    )

    /** Returns the entry for a Lint issue ID, for example `ComposeMissingContentDescription`. */
    fun byIssueId(issueId: String): TaxonomyEntry? = entries.firstOrNull { it.issue.id == issueId }
}
