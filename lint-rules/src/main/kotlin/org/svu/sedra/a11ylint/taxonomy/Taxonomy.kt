package org.svu.sedra.a11ylint.taxonomy

import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.detectors.operable.O01SmallTouchTargetDetector
import org.svu.sedra.a11ylint.detectors.operable.O02MissingOnClickLabelDetector
import org.svu.sedra.a11ylint.detectors.operable.O03NestedClickableDetector
import org.svu.sedra.a11ylint.detectors.operable.O04ClickableContainerDetector
import org.svu.sedra.a11ylint.detectors.operable.O05EmptyClickableDetector
import org.svu.sedra.a11ylint.detectors.perceivable.P01MissingContentDescriptionDetector
import org.svu.sedra.a11ylint.detectors.perceivable.P02DecorativeImageLabeledDetector
import org.svu.sedra.a11ylint.detectors.perceivable.P03LowContrastColorsDetector
import org.svu.sedra.a11ylint.detectors.perceivable.P04TextSizeInDpDetector
import org.svu.sedra.a11ylint.detectors.perceivable.P06MissingLiveRegionDetector
import org.svu.sedra.a11ylint.detectors.robust.R01ClickableWithoutRoleDetector
import org.svu.sedra.a11ylint.detectors.robust.R02ClearAndSetSemanticsLossDetector
import org.svu.sedra.a11ylint.detectors.understandable.U01MissingHeadingDetector
import org.svu.sedra.a11ylint.detectors.understandable.U02MissingStateDescriptionDetector
import org.svu.sedra.a11ylint.detectors.understandable.U03MissingSemanticErrorDetector
import org.svu.sedra.a11ylint.detectors.understandable.U04VagueButtonLabelDetector
import org.svu.sedra.a11ylint.detectors.understandable.U05TextFieldWithoutLabelDetector

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
            id = P02DecorativeImageLabeledDetector.TAXONOMY_ID,
            issue = P02DecorativeImageLabeledDetector.ISSUE,
            pour = Pour.PERCEIVABLE,
            wcag = listOf("1.1.1"),
            detection = DetectionType.STATIC_LLM,
            priority = Priority.MAJOR,
        ),
        TaxonomyEntry(
            id = P03LowContrastColorsDetector.TAXONOMY_ID,
            issue = P03LowContrastColorsDetector.ISSUE,
            pour = Pour.PERCEIVABLE,
            wcag = listOf("1.4.3"),
            detection = DetectionType.STATIC,
            priority = Priority.MAJOR,
        ),
        TaxonomyEntry(
            id = P04TextSizeInDpDetector.TAXONOMY_ID,
            issue = P04TextSizeInDpDetector.ISSUE,
            pour = Pour.PERCEIVABLE,
            wcag = listOf("1.4.4"),
            detection = DetectionType.STATIC,
            priority = Priority.MAJOR,
        ),
        TaxonomyEntry(
            id = P06MissingLiveRegionDetector.TAXONOMY_ID,
            issue = P06MissingLiveRegionDetector.ISSUE,
            pour = Pour.PERCEIVABLE,
            wcag = listOf("4.1.3"),
            detection = DetectionType.STATIC_LLM,
            priority = Priority.MAJOR,
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
            id = O02MissingOnClickLabelDetector.TAXONOMY_ID,
            issue = O02MissingOnClickLabelDetector.ISSUE,
            pour = Pour.OPERABLE,
            wcag = listOf("4.1.2"),
            detection = DetectionType.STATIC,
            priority = Priority.MAJOR,
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
            id = O04ClickableContainerDetector.TAXONOMY_ID,
            issue = O04ClickableContainerDetector.ISSUE,
            pour = Pour.OPERABLE,
            wcag = listOf("4.1.2"),
            detection = DetectionType.STATIC_LLM,
            priority = Priority.MAJOR,
        ),
        TaxonomyEntry(
            id = O05EmptyClickableDetector.TAXONOMY_ID,
            issue = O05EmptyClickableDetector.ISSUE,
            pour = Pour.OPERABLE,
            wcag = listOf("4.1.2"),
            detection = DetectionType.STATIC,
            priority = Priority.MAJOR,
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
        TaxonomyEntry(
            id = U03MissingSemanticErrorDetector.TAXONOMY_ID,
            issue = U03MissingSemanticErrorDetector.ISSUE,
            pour = Pour.UNDERSTANDABLE,
            wcag = listOf("3.3.1"),
            detection = DetectionType.STATIC,
            priority = Priority.MAJOR,
        ),
        TaxonomyEntry(
            id = U04VagueButtonLabelDetector.TAXONOMY_ID,
            issue = U04VagueButtonLabelDetector.ISSUE,
            pour = Pour.UNDERSTANDABLE,
            wcag = listOf("2.4.6"),
            detection = DetectionType.STATIC_LLM,
            priority = Priority.MAJOR,
        ),
        TaxonomyEntry(
            id = U05TextFieldWithoutLabelDetector.TAXONOMY_ID,
            issue = U05TextFieldWithoutLabelDetector.ISSUE,
            pour = Pour.UNDERSTANDABLE,
            wcag = listOf("1.3.1", "3.3.2"),
            detection = DetectionType.STATIC,
            priority = Priority.CRITICAL,
        ),
        TaxonomyEntry(
            id = R01ClickableWithoutRoleDetector.TAXONOMY_ID,
            issue = R01ClickableWithoutRoleDetector.ISSUE,
            pour = Pour.ROBUST,
            wcag = listOf("4.1.2"),
            detection = DetectionType.STATIC_LLM,
            priority = Priority.CRITICAL,
        ),
        TaxonomyEntry(
            id = R02ClearAndSetSemanticsLossDetector.TAXONOMY_ID,
            issue = R02ClearAndSetSemanticsLossDetector.ISSUE,
            pour = Pour.ROBUST,
            wcag = listOf("4.1.2"),
            detection = DetectionType.STATIC,
            priority = Priority.MAJOR,
        ),
    )

    /** Returns the entry for a Lint issue ID, for example `ComposeMissingContentDescription`. */
    fun byIssueId(issueId: String): TaxonomyEntry? = entries.firstOrNull { it.issue.id == issueId }
}
