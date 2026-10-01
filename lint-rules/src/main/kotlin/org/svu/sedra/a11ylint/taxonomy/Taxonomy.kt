package org.svu.sedra.a11ylint.taxonomy

import com.android.tools.lint.detector.api.Issue

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

object Taxonomy {
    val entries: List<TaxonomyEntry> = emptyList()
}
