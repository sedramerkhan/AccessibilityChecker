package org.svu.sedra.a11ylint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import org.svu.sedra.a11ylint.taxonomy.Taxonomy

class ComposeA11yIssueRegistry : IssueRegistry() {
    override val issues = Taxonomy.entries.map { it.issue }
    override val api: Int = CURRENT_API

    // Lint API 14 is AGP 8.0. Older Lint versions are not supported. See docs/DECISIONS.md.
    override val minApi: Int = 14
    override val vendor: Vendor = Vendor(
        vendorName = "Compose A11y Lint (SVU thesis)",
        identifier = "compose-a11y-lint",
        feedbackUrl = "https://example.com/compose-a11y-lint",
    )
}
