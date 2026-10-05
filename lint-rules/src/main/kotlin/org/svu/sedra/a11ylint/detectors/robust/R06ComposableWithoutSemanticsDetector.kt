package org.svu.sedra.a11ylint.detectors.robust

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.svu.sedra.a11ylint.taxonomy.A11yIssues
import org.svu.sedra.a11ylint.taxonomy.Priority
import org.svu.sedra.a11ylint.util.ComposeCalls

/**
 * R-06 (candidate): a `@Composable` function that handles input through a custom gesture and
 * sets no semantics at all.
 *
 * A custom gesture is invisible to accessibility services: `pointerInput` and `draggable` add
 * no action, so TalkBack has nothing to offer and the element cannot be used without sight.
 * A function that uses `clickable` or `toggleable` is not reported, because those already carry
 * an action and are covered by R-01, O-02 and U-02.
 */
class R06ComposableWithoutSemanticsDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler = object : UElementHandler() {
        override fun visitMethod(node: UMethod) {
            if (!node.hasAnnotation(ComposeCalls.COMPOSABLE)) return
            val calls = bodyCalls(node)
            if (calls.none { ComposeCalls.isCall(it, GESTURES) }) return
            if (calls.any { ComposeCalls.isCall(it, SEMANTICS) }) return

            context.report(
                ISSUE,
                node,
                context.getNameLocation(node),
                A11yIssues.message(
                    TAXONOMY_ID,
                    "Possible missing semantics: ${node.name} handles input with a custom gesture " +
                        "but sets no semantics, so screen readers have no action to offer",
                ),
            )
        }
    }

    /** Every call in the body of [method], at any depth. */
    private fun bodyCalls(method: UMethod): List<UCallExpression> {
        val calls = mutableListOf<UCallExpression>()
        method.uastBody?.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                calls += node
                return super.visitCallExpression(node)
            }
        })
        return calls
    }

    companion object {
        const val TAXONOMY_ID = "R-06"

        private const val GESTURE_PACKAGE = "androidx.compose.foundation.gestures"
        private const val SEMANTICS_PACKAGE = "androidx.compose.ui.semantics"

        /**
         * Custom gesture handling, checked against the Compose 1.10.4 sources. CLAUDE.md also
         * names `swipeable` and `anchoredDraggable`; neither is public API in these versions
         * (see DECISIONS), so they are left out rather than guessed at.
         */
        private val GESTURES = setOf(
            "androidx.compose.ui.input.pointer.pointerInput",
            "$GESTURE_PACKAGE.detectTapGestures",
            "$GESTURE_PACKAGE.detectDragGestures",
            "$GESTURE_PACKAGE.detectDragGesturesAfterLongPress",
            "$GESTURE_PACKAGE.detectVerticalDragGestures",
            "$GESTURE_PACKAGE.detectHorizontalDragGestures",
            "$GESTURE_PACKAGE.detectTransformGestures",
            "$GESTURE_PACKAGE.draggable",
            "$GESTURE_PACKAGE.draggable2D",
        )

        /** Anything that gives accessibility services something to work with. */
        private val SEMANTICS = setOf(
            "$SEMANTICS_PACKAGE.semantics",
            "$SEMANTICS_PACKAGE.clearAndSetSemantics",
            "androidx.compose.foundation.clickable",
            "androidx.compose.foundation.combinedClickable",
            "androidx.compose.foundation.selection.toggleable",
            "androidx.compose.foundation.selection.triStateToggleable",
            "androidx.compose.foundation.selection.selectable",
        )

        @JvmField
        val ISSUE: Issue = A11yIssues.create(
            id = "ComposeComposableWithoutSemantics",
            briefDescription = "Custom gesture with no semantics",
            explanation = """
                A gesture handled with `pointerInput` or `draggable` is invisible to \
                accessibility services: unlike `clickable`, it adds no action, so TalkBack has \
                nothing to announce and nothing to activate, and the element cannot be used \
                without sight or without precise pointer control. This relates to WCAG 2.2 \
                success criterion 4.1.2 (Name, Role, Value). Add the action in \
                `Modifier.semantics { }`, for example `onClick`, `role` or `customActions`, or \
                use `Modifier.clickable` when a plain tap is all the gesture does.
            """,
            priority = Priority.MAJOR,
            detector = R06ComposableWithoutSemanticsDetector::class.java,
        )
    }
}
