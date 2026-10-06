package org.svu.sedra.a11ylint.detectors.perceivable

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReturnExpression
import org.svu.sedra.a11ylint.taxonomy.A11yIssues
import org.svu.sedra.a11ylint.taxonomy.Priority
import org.svu.sedra.a11ylint.util.Clickables
import org.svu.sedra.a11ylint.util.ComposeCalls
import org.svu.sedra.a11ylint.util.Literals
import org.svu.sedra.a11ylint.util.ModifierChain

/**
 * P-05 (candidate): a `Box` that draws a `Text` on top of an `Image`, with nothing between them
 * to guarantee contrast.
 *
 * In a `Box` the children are stacked in source order, so a Text written after an Image is drawn
 * over it. Whether the result is readable depends on the picture, which static analysis cannot
 * know, so this is a candidate: the rule only reports when nothing is protecting the text at
 * all, no background on the Text itself and no layer with a background between the two.
 */
class P05TextOverImageDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf("Box")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!ComposeCalls.isCall(node, BOX)) return
        val children = directChildren(context, node)
        val imageIndex = children.indexOfFirst { ComposeCalls.isCall(it, IMAGE) }
        if (imageIndex < 0) return

        val textIndex = children.drop(imageIndex + 1)
            .indexOfFirst { ComposeCalls.isCall(it, Clickables.textCalls) }
            .takeIf { it >= 0 }
            ?.plus(imageIndex + 1) ?: return
        val text = children[textIndex]

        if (hasBackground(context, text)) return
        // A scrim is usually a layer of its own between the picture and the text.
        if (children.subList(imageIndex + 1, textIndex).any { hasBackground(context, it) }) return

        context.report(
            ISSUE,
            text,
            context.getNameLocation(text),
            A11yIssues.message(
                TAXONOMY_ID,
                "Possible text over an image: this Text is drawn on top of an Image with no " +
                    "background or scrim, so its contrast depends on the picture",
            ),
        )
    }

    /** The calls written directly in the content lambda of [box], in source order. */
    private fun directChildren(context: JavaContext, box: UCallExpression): List<UCallExpression> {
        val lambda = ComposeCalls.contentLambda(context, box) ?: return emptyList()
        val statements = (lambda.body as? UBlockExpression)?.expressions ?: return emptyList()
        return statements.mapNotNull { statement ->
            val value = Literals.unwrap((statement as? UReturnExpression)?.returnExpression ?: statement)
            when (value) {
                is UCallExpression -> value
                is UQualifiedReferenceExpression -> Literals.unwrap(value.selector) as? UCallExpression
                else -> null
            }
        }
    }

    /** True when [call] paints something behind itself with `Modifier.background(...)`. */
    private fun hasBackground(context: JavaContext, call: UCallExpression): Boolean =
        ModifierChain.hasModifier(modifierOf(context, call), BACKGROUND)

    private fun modifierOf(context: JavaContext, call: UCallExpression): UExpression? =
        ModifierChain.modifierArgument(context, call)

    companion object {
        const val TAXONOMY_ID = "P-05"

        private const val BOX = "androidx.compose.foundation.layout.Box"
        private const val BACKGROUND = "androidx.compose.foundation.background"

        /**
         * Only `Image`, as CLAUDE.md 7.4 words it, not the whole of `Clickables.imageCalls`.
         * An `Icon` is a small tinted symbol rather than a picture, and a `Box` holding an Icon
         * and a Text is normally an icon-and-label button, not text over a photo.
         */
        private const val IMAGE = "androidx.compose.foundation.Image"

        @JvmField
        val ISSUE: Issue = A11yIssues.create(
            id = "ComposeTextOverImage",
            briefDescription = "Text drawn over an image with no background",
            explanation = """
                Text drawn straight onto a picture is readable or not depending on the picture, \
                and a photo that changes, or simply a bright area in the wrong place, can leave \
                the text invisible to users with low vision. This relates to WCAG 2.2 success \
                criterion 1.4.3 (Contrast (Minimum)). Put a scrim between the picture and the \
                text, for example a `Box` with a translucent `Modifier.background(...)`, or give \
                the Text its own background.
            """,
            priority = Priority.MINOR,
            detector = P05TextOverImageDetector::class.java,
        )
    }
}
