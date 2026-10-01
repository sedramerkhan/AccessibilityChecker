package org.svu.sedra.a11ylint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

class ComposeA11yIssueRegistry : IssueRegistry() {
    override val issues: List<Issue> = listOf(TemporaryMarkerIssue.issue)
    override val api: Int = com.android.tools.lint.detector.api.CURRENT_API
    override val minApi: Int = 1
    override val vendor: Vendor = Vendor(
        vendorName = "Compose A11y Lint (SVU thesis)",
        identifier = "compose-a11y-lint",
        feedbackUrl = "https://example.com/compose-a11y-lint",
    )
}

internal object TemporaryMarkerIssue {
    val issue: Issue = Issue.create(
        id = "ComposeA11yTestMarker",
        briefDescription = "Temporary accessibility lint marker",
        explanation = "Temporary Milestone 0 rule used to verify that the custom lint registry loads.",
        category = Category.A11Y,
        priority = 3,
        severity = Severity.WARNING,
        implementation = Implementation(
            TemporaryMarkerDetector::class.java,
            Scope.JAVA_FILE_SCOPE,
        ),
    )
}

class TemporaryMarkerDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf("a11yTestMarker")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        context.report(
            TemporaryMarkerIssue.issue,
            node,
            context.getLocation(node),
            "[M0] Temporary marker detected",
        )
    }
}
