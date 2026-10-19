package org.svu.sedra.a11ylint.detectors.operable

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.svu.sedra.a11ylint.taxonomy.A11yIssues
import org.svu.sedra.a11ylint.taxonomy.Priority
import org.svu.sedra.a11ylint.util.Clickables
import org.svu.sedra.a11ylint.util.ComposeCalls
import org.svu.sedra.a11ylint.util.ModifierChain
import org.svu.sedra.a11ylint.util.ModifierSizes
import org.svu.sedra.a11ylint.util.ModifierSizes.Axis

/**
 * O-01: a click or toggle modifier on an element whose literal width or height is below 48dp,
 * with no `minimumInteractiveComponentSize()` in the chain.
 *
 * The rule starts at each click or toggle modifier, reads the whole modifier chain around it,
 * and measures the clickable bounds with [ModifierSizes.measure]. Chains passed to a Material
 * component that enforces the minimum size itself are ignored.
 *
 * Two thresholds, two severities. Below [WCAG_MINIMUM_DP] the target fails WCAG 2.2 success
 * criterion 2.5.8 and is reported as an error. From there up to [MINIMUM_DP] it meets WCAG but
 * not the Android guideline, and the same issue is reported as a warning.
 */
class O01SmallTouchTargetDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> =
        Clickables.clickableModifiers.map { it.substringAfterLast('.') }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!ComposeCalls.isCall(node, Clickables.clickableModifiers)) return
        val top = ModifierChain.outermostExpression(node)
        val calls = ModifierChain.calls(top)
        val index = calls.indexOfFirst { it.sourcePsi == node.sourcePsi }
        if (index < 0) return
        // One report per chain: only the first click or toggle modifier is checked.
        if (calls.take(index).any { ComposeCalls.isCall(it, Clickables.clickableModifiers) }) return
        if (calls.any { ComposeCalls.isCall(it, MINIMUM_SIZE_MODIFIERS) }) return
        val receiver = ModifierChain.receivingElement(top) as? UCallExpression
        if (receiver != null && Clickables.isMaterialClickable(context, receiver)) return

        val small = Axis.entries.mapNotNull { axis ->
            ModifierSizes.measure(context, calls, index, axis)
                ?.takeIf { it.dp < MINIMUM_DP }
                ?.let { axis to it }
        }
        if (small.isEmpty()) return

        val description = small.joinToString(" and ") { (axis, measured) ->
            "${format(measured.dp)}dp " + if (axis == Axis.WIDTH) "wide" else "high"
        }
        val failsWcag = small.any { (_, measured) -> measured.dp < WCAG_MINIMUM_DP }
        val call = small.first().second.call
        val incident = Incident(
            ISSUE,
            call,
            context.getCallLocation(call, includeReceiver = false, includeArguments = true),
            A11yIssues.message(
                TAXONOMY_ID,
                if (failsWcag) {
                    "Clickable element is only $description, below the 24dp WCAG minimum target " +
                        "size and the 48dp Android touch target"
                } else {
                    "Clickable element is only $description, smaller than the 48dp Android " +
                        "minimum touch target"
                },
            ),
        )
        // The issue's default severity is the WCAG case; the guideline-only case is lowered.
        if (!failsWcag) incident.overrideSeverity(Severity.WARNING)
        context.report(incident)
    }

    private fun format(dp: Float): String =
        if (dp == dp.toLong().toFloat()) dp.toLong().toString() else dp.toString()

    companion object {
        const val TAXONOMY_ID = "O-01"

        /** Android's minimum touch target size (Material guideline). Below it: a warning. */
        const val MINIMUM_DP = 48f

        /** WCAG 2.2 success criterion 2.5.8 (Target Size, Minimum, level AA). Below it: an error. */
        const val WCAG_MINIMUM_DP = 24f

        private val MINIMUM_SIZE_MODIFIERS = setOf(
            "androidx.compose.material3.minimumInteractiveComponentSize",
            "androidx.compose.material.minimumInteractiveComponentSize",
        )

        @JvmField
        val ISSUE: Issue = A11yIssues.create(
            id = "ComposeSmallTouchTarget",
            briefDescription = "Clickable element smaller than 48dp",
            explanation = """
                Small touch targets are hard to hit for people with motor impairments or tremors, \
                and for anyone using the phone with one hand. A target below 24 by 24dp fails WCAG \
                2.2 success criterion 2.5.8 (Target Size, Minimum) and is reported as an error; a \
                target from 24dp up to 48dp meets WCAG but not the Android accessibility guideline \
                of 48 by 48dp, and is reported as a warning. Make the element at least `48.dp`, \
                add padding inside the clickable area, or add \
                `Modifier.minimumInteractiveComponentSize()`.
            """,
            priority = Priority.CRITICAL,
            detector = O01SmallTouchTargetDetector::class.java,
        )
    }
}
