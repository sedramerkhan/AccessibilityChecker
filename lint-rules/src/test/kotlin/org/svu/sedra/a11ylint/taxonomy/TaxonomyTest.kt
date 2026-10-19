package org.svu.sedra.a11ylint.taxonomy

import com.android.tools.lint.client.api.LintClient
import com.android.tools.lint.detector.api.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.svu.sedra.a11ylint.ComposeA11yIssueRegistry

class TaxonomyTest {
    @Test
    fun idsAndIssueIdsAreUnique() {
        val entries = Taxonomy.entries
        assertEquals(entries.size, entries.map { it.id }.toSet().size)
        assertEquals(entries.size, entries.map { it.issue.id }.toSet().size)
    }

    @Test
    fun severityAndPriorityFollowTheTaxonomyPriority() {
        for (entry in Taxonomy.entries) {
            val expected = if (entry.id in CRITICAL_WARNINGS) Severity.WARNING else A11yIssues.severity(entry.priority)
            assertEquals(entry.id, expected, entry.issue.defaultSeverity)
            assertEquals(entry.id, A11yIssues.lintPriority(entry.priority), entry.issue.priority)
        }
    }

    /** Critical rules reported as warnings, because they are not definite WCAG failures (DECISIONS, 2026-10-19). */
    @Test
    fun criticalWarningsAreCritical() {
        for (id in CRITICAL_WARNINGS) {
            assertEquals(id, Priority.CRITICAL, Taxonomy.entries.single { it.id == id }.priority)
        }
    }

    private companion object {
        val CRITICAL_WARNINGS = setOf("R-01", "U-01", "O-03")
    }

    @Test
    fun issueIdsStartWithCompose() {
        for (entry in Taxonomy.entries) {
            assertTrue(entry.issue.id, entry.issue.id.startsWith("Compose"))
        }
    }

    @Test
    fun registryContainsEveryTaxonomyIssue() {
        LintClient.clientName = LintClient.CLIENT_UNIT_TESTS
        val registered = ComposeA11yIssueRegistry().issues.map { it.id }.toSet()
        assertEquals(Taxonomy.entries.map { it.issue.id }.toSet(), registered)
    }

    @Test
    fun entriesCanBeFoundByIssueId() {
        assertEquals("P-01", Taxonomy.byIssueId("ComposeMissingContentDescription")?.id)
    }
}
