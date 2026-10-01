package org.svu.sedra.a11ylint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import org.svu.sedra.a11ylint.taxonomy.Taxonomy

class ComposeA11yIssueRegistry : IssueRegistry() {
    override val issues = Taxonomy.entries.map { it.issue }
    override val api: Int = com.android.tools.lint.detector.api.CURRENT_API
    override val minApi: Int = 1
    override val vendor: Vendor = Vendor(
        vendorName = "Compose A11y Lint (SVU thesis)",
        identifier = "compose-a11y-lint",
        feedbackUrl = "https://example.com/compose-a11y-lint",
    )
}
