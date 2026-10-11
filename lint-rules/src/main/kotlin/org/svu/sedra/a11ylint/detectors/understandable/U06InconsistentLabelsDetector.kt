package org.svu.sedra.a11ylint.detectors.understandable

import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintMap
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.PartialResult
import com.android.tools.lint.detector.api.ResourceEvaluator
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.svu.sedra.a11ylint.taxonomy.A11yIssues
import org.svu.sedra.a11ylint.taxonomy.Priority
import org.svu.sedra.a11ylint.util.ComposeCalls
import org.svu.sedra.a11ylint.util.Literals

/**
 * U-06 (candidate): the same icon described with different words in different places.
 *
 * A user who has learned that the bin icon is "Delete" expects to hear "Delete" everywhere. When
 * one screen calls it "Delete" and another "Remove item", the same control sounds like two
 * different things, which is the defect WCAG 3.2.4 (Consistent Identification) describes.
 *
 * This is the only project-wide rule, so it cannot decide anything from one file. Each file
 * contributes what it saw to a [LintMap] through Lint's partial analysis, and the comparison
 * happens once in [checkPartialResults], after every file has been read.
 *
 * An icon is identified by where it comes from: a drawable read at the call site, or a property
 * holding an `ImageVector` or a `Painter`. An icon held in a local variable is **not** followed,
 * because one local often stands in for several different icons, and following it would compare
 * labels that were never meant to describe the same thing.
 */
class U06InconsistentLabelsDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf("Icon")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!ComposeCalls.isCall(node, ICONS)) return
        val icon = iconName(context, iconArgument(context, node)) ?: return
        val description = ComposeCalls.argument(context, node, "contentDescription", VECTOR_PARAMETERS)
            ?: ComposeCalls.argument(context, node, "contentDescription", PAINTER_PARAMETERS)
            ?: return
        val label = Literals.stringLiteralValue(description)?.takeIf { it.isNotBlank() } ?: return

        val occurrences = context.getPartialResults(ISSUE).map()
        occurrences.put(
            occurrences.size.toString(),
            LintMap()
                .put(KEY_ICON, icon)
                .put(KEY_LABEL, label)
                .put(KEY_LOCATION, context.getLocation(description)),
        )
    }

    /**
     * In global analysis nothing calls [checkPartialResults], so the same comparison is run here
     * once every file has been read.
     */
    override fun afterCheckRootProject(context: Context) {
        if (context.isGlobalAnalysis()) {
            checkPartialResults(context, context.getPartialResults(ISSUE))
        }
    }

    override fun checkPartialResults(context: Context, partialResults: PartialResult) {
        val byIcon = linkedMapOf<String, MutableList<Pair<String, Location>>>()
        for (map in partialResults.maps()) {
            for (key in map) {
                val occurrence = map.getMap(key) ?: continue
                val icon = occurrence.getString(KEY_ICON) ?: continue
                val label = occurrence.getString(KEY_LABEL) ?: continue
                val location = occurrence.getLocation(KEY_LOCATION) ?: continue
                byIcon.getOrPut(icon) { mutableListOf() } += label to location
            }
        }

        for ((icon, occurrences) in byIcon) {
            val labels = occurrences.map { it.first }.toSortedSet()
            if (labels.size < 2) continue
            for ((label, location) in occurrences) {
                val others = labels.filterNot { it == label }.joinToString(", ") { "\"$it\"" }
                context.report(
                    ISSUE,
                    location,
                    A11yIssues.message(
                        TAXONOMY_ID,
                        "Possible inconsistent label: ${displayName(icon)} is described as " +
                            "\"$label\" here but as $others elsewhere, so the same icon is " +
                            "announced in different words",
                    ),
                )
            }
        }
    }

    /** The icon the call draws, whichever overload of `Icon` is used. */
    private fun iconArgument(context: JavaContext, call: UCallExpression): UExpression? =
        ComposeCalls.argument(context, call, "imageVector", VECTOR_PARAMETERS)
            ?: ComposeCalls.argument(context, call, "painter", PAINTER_PARAMETERS)

    /**
     * A name that identifies the icon across files, or null when it cannot be identified.
     *
     * Two shapes count. A drawable read at the call site gives its resource url
     * (`@drawable/ic_bin`). A property reference gives the fully qualified property name, so
     * `Icons.Default.Delete` gives `androidx.compose.material.icons.filled.Delete`. Resolving
     * rather than reading the source text means `Icons.Default.Delete` and `Icons.Filled.Delete`
     * give the same name, because `Default` is an alias for `Filled`, and an import alias or a
     * fully qualified call does not change the answer.
     *
     * A local variable is deliberately not followed: see the KDoc of the class and DECISIONS.
     */
    private fun iconName(context: JavaContext, expression: UExpression?): String? {
        val value = Literals.unwrap(expression) ?: return null
        return resourceName(context, value) ?: propertyName(value)
    }

    /** `painterResource(R.drawable.ic_bin)` written at the call site gives `@drawable/ic_bin`. */
    private fun resourceName(context: JavaContext, expression: UExpression): String? {
        val call = when (expression) {
            is UCallExpression -> expression
            is UQualifiedReferenceExpression -> expression.selector as? UCallExpression
            else -> null
        } ?: return null
        if (!ComposeCalls.isCall(call, PAINTER_RESOURCES)) return null
        val id = call.valueArguments.firstOrNull() ?: return null
        return ResourceEvaluator.getResource(context.evaluator, id)?.toString()
    }

    /** A reference to a property that holds an `ImageVector` or a `Painter`. */
    private fun propertyName(expression: UExpression): String? {
        val selector = when (expression) {
            is UQualifiedReferenceExpression -> expression.selector as? USimpleNameReferenceExpression
            is USimpleNameReferenceExpression -> expression
            else -> null
        } ?: return null
        val getter = selector.resolve() as? PsiMethod ?: return null
        if (getter.returnType?.canonicalText !in ICON_TYPES) return null
        val packageName = ComposeCalls.packageName(getter) ?: return null
        return "$packageName.${propertyName(getter)}"
    }

    /**
     * The property name from its getter, keeping the original case. Icon properties are
     * capitalised (`Icons.Filled.Delete`) unlike ordinary Kotlin properties, so
     * `Literals.propertyName` would turn `getDelete` into `delete`.
     */
    private fun propertyName(getter: PsiMethod): String {
        val name = ComposeCalls.declaredName(getter.name)
        return name.removePrefix("get").ifEmpty { name }
    }

    /**
     * `androidx.compose.material.icons.filled.Delete` reads better as `Icons.Filled.Delete`.
     * A resource url (`@drawable/ic_bin`) and any other property name are shown as they are.
     */
    private fun displayName(icon: String): String {
        if (!icon.startsWith(ICON_PACKAGE)) return icon
        val parts = icon.split('.')
        if (parts.size < 2) return icon
        val set = parts[parts.size - 2].replaceFirstChar { it.uppercaseChar() }
        return "Icons.$set.${parts.last()}"
    }

    companion object {
        const val TAXONOMY_ID = "U-06"

        private const val ICON_PACKAGE = "androidx.compose.material.icons"

        private val ICONS = setOf("androidx.compose.material3.Icon", "androidx.compose.material.Icon")

        private val PAINTER_RESOURCES = setOf("androidx.compose.ui.res.painterResource")

        /** What a property has to return before it is treated as an icon. */
        private val ICON_TYPES = setOf(
            "androidx.compose.ui.graphics.vector.ImageVector",
            "androidx.compose.ui.graphics.painter.Painter",
        )

        /** Kotlin parameter names of each `Icon` overload, for the positional fallback. */
        private val VECTOR_PARAMETERS = listOf("imageVector", "contentDescription", "modifier", "tint")
        private val PAINTER_PARAMETERS = listOf("painter", "contentDescription", "modifier", "tint")

        private const val KEY_ICON = "icon"
        private const val KEY_LABEL = "label"
        private const val KEY_LOCATION = "location"

        @JvmField
        val ISSUE: Issue = A11yIssues.create(
            id = "ComposeInconsistentLabels",
            briefDescription = "Same icon described with different words",
            explanation = """
                Screen reader users learn an app by its labels: once the bin icon is "Delete", \
                that is what it is called. Describing the same icon as "Delete" on one screen \
                and "Remove item" on another makes one control sound like two, so the user \
                cannot be sure the action is the same. This relates to WCAG 2.2 success \
                criterion 3.2.4 (Consistent Identification). Agree one wording per icon and \
                keep it in a string resource, so every screen uses the same words.
            """,
            priority = Priority.MINOR,
            detector = U06InconsistentLabelsDetector::class.java,
        )
    }
}
