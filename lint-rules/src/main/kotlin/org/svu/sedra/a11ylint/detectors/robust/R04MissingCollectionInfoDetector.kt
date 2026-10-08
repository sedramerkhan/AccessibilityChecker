package org.svu.sedra.a11ylint.detectors.robust

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UForEachExpression
import org.jetbrains.uast.UForExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UastBinaryOperator
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.svu.sedra.a11ylint.taxonomy.A11yIssues
import org.svu.sedra.a11ylint.taxonomy.Priority
import org.svu.sedra.a11ylint.util.Clickables
import org.svu.sedra.a11ylint.util.ComposeCalls
import org.svu.sedra.a11ylint.util.Literals
import org.svu.sedra.a11ylint.util.ModifierChain

/**
 * R-04: a `Column` or `Row` that builds a list with a loop and sets no collection semantics.
 *
 * A lazy list tells accessibility services how many items it holds and where each one sits, so
 * TalkBack can say "list, 5 items" and "item 2 of 5". A `Column` with a `forEach` inside looks
 * the same on screen but announces none of that, so the user cannot tell how long the list is or
 * where they are in it.
 *
 * Only the nearest container is reported: the walk stops at a nested layout, so the loop belongs
 * to the innermost `Column` or `Row` that holds it.
 */
class R04MissingCollectionInfoDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler = object : UElementHandler() {
        override fun visitCallExpression(node: UCallExpression) {
            if (!ComposeCalls.isCall(node, CONTAINERS)) return
            if (hasCollectionSemantics(node)) return
            val content = content(context, node) ?: return
            if (!buildsListWithLoop(content)) return

            val name = ComposeCalls.name(node) ?: "container"
            context.report(
                ISSUE,
                node,
                context.getNameLocation(node),
                A11yIssues.message(
                    TAXONOMY_ID,
                    "$name builds a list with a loop but sets no collectionInfo, so screen " +
                        "readers cannot say how many items there are or which one is focused",
                ),
            )
        }
    }

    /**
     * True when `collectionInfo` or `collectionItemInfo` is set anywhere in the container, on
     * the container itself or on one of the items. Either one means the developer has thought
     * about the collection, so the rule stays quiet.
     */
    private fun hasCollectionSemantics(container: UCallExpression): Boolean {
        var found = false
        container.accept(object : AbstractUastVisitor() {
            override fun visitBinaryExpression(node: UBinaryExpression): Boolean {
                if (found) return true
                if (node.operator == UastBinaryOperator.ASSIGN &&
                    ModifierChain.assignedName(node.leftOperand) in COLLECTION_PROPERTIES
                ) {
                    found = true
                }
                return found
            }
        })
        return found
    }

    /**
     * True when the content repeats UI with `forEach`, `forEachIndexed` or a `for` loop. The
     * walk stops at a nested layout container, which owns any loop inside it.
     */
    private fun buildsListWithLoop(content: ULambdaExpression): Boolean {
        var found = false
        content.body.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                if (found) return true
                if (ComposeCalls.isCall(node, NESTED_CONTAINERS)) return true
                if (ComposeCalls.name(node) in LOOP_FUNCTIONS && emitsUi(lastLambda(node))) found = true
                return found
            }

            override fun visitForEachExpression(node: UForEachExpression): Boolean {
                if (!found && emitsUi(node.body)) found = true
                return found
            }

            override fun visitForExpression(node: UForExpression): Boolean {
                if (!found && emitsUi(node.body)) found = true
                return found
            }
        })
        return found
    }

    /** True when [body] contains a call to a `@Composable` function, so the loop really emits UI. */
    private fun emitsUi(body: UElement?): Boolean {
        if (body == null) return false
        var found = false
        body.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                if (!found && ComposeCalls.isComposableCall(node)) found = true
                return found
            }
        })
        return found
    }

    private fun lastLambda(call: UCallExpression): ULambdaExpression? =
        call.valueArguments.mapNotNull { Literals.unwrap(it) as? ULambdaExpression }.lastOrNull()

    /** The content lambda, falling back to the last lambda argument when it is not named `content`. */
    private fun content(context: JavaContext, container: UCallExpression): ULambdaExpression? =
        ComposeCalls.contentLambda(context, container) ?: lastLambda(container)

    companion object {
        const val TAXONOMY_ID = "R-04"

        private const val LAYOUT = "androidx.compose.foundation.layout"
        private const val LAZY = "androidx.compose.foundation.lazy"

        /** CLAUDE.md 7.4 names `Column` and `Row`. */
        private val CONTAINERS = setOf("$LAYOUT.Column", "$LAYOUT.Row")

        /**
         * Layouts that own any loop inside them. The lazy lists and grids are here so that a
         * `Column` wrapped around one is not reported for the lazy list's own items; they provide
         * collection semantics themselves, which is why CLAUDE.md 7.4 excludes them.
         */
        private val NESTED_CONTAINERS = Clickables.layoutContainers + setOf(
            "$LAZY.LazyColumn",
            "$LAZY.LazyRow",
            "$LAZY.grid.LazyVerticalGrid",
            "$LAZY.grid.LazyHorizontalGrid",
            "$LAZY.staggeredgrid.LazyVerticalStaggeredGrid",
            "$LAZY.staggeredgrid.LazyHorizontalStaggeredGrid",
        )

        private val LOOP_FUNCTIONS = setOf("forEach", "forEachIndexed")

        private val COLLECTION_PROPERTIES = setOf("collectionInfo", "collectionItemInfo")

        @JvmField
        val ISSUE: Issue = A11yIssues.create(
            id = "ComposeMissingCollectionInfo",
            briefDescription = "List built with a loop has no collection semantics",
            explanation = """
                A `LazyColumn` tells accessibility services how many items it holds and where \
                each one sits, so TalkBack announces "list, 5 items" and then "item 2 of 5". A \
                `Column` with a `forEach` inside looks identical on screen but announces none of \
                that, so a screen reader user cannot tell how long the list is, where they are \
                in it, or when it ends. This relates to WCAG 2.2 success criterion 1.3.1 (Info \
                and Relationships). Use a `LazyColumn` or `LazyRow`, which provide this \
                automatically, or set it by hand with \
                `Modifier.semantics { collectionInfo = CollectionInfo(rowCount = items.size, \
                columnCount = 1) }` and a `collectionItemInfo` on each item.
            """,
            priority = Priority.MINOR,
            detector = R04MissingCollectionInfoDetector::class.java,
        )
    }
}
