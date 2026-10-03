package org.svu.sedra.a11ylint.util

import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UMethod

/** Identifies emitted Compose UI and excludes state, effect and coroutine lambdas. */
object UiScope {
    /** Fully qualified name of the Compose `@Preview` annotation. */
    const val PREVIEW = "androidx.compose.ui.tooling.preview.Preview"

    /**
     * Functions whose lambda runs outside UI emission: state holders, effects and coroutine
     * builders. Matched by the declared function name.
     */
    val nonUiLambdaCalls = setOf(
        "remember",
        "rememberSaveable",
        "derivedStateOf",
        "LaunchedEffect",
        "DisposableEffect",
        "SideEffect",
        "produceState",
        "launch",
        "async",
    )

    /**
     * Returns true when [node] is inside a function annotated `@Composable` and not inside the
     * lambda of a call in [nonUiLambdaCalls]. The search stops at the nearest enclosing function,
     * so a composable lambda inside a non-composable function (for example `setContent { }` in
     * an Activity) is not UI scope.
     */
    fun isInUiScope(node: UElement): Boolean {
        for (element in ancestors(node)) {
            if (element is UMethod) return element.hasAnnotation(ComposeCalls.COMPOSABLE)
            if (element is ULambdaExpression) {
                val call = element.uastParent as? UCallExpression
                if (call != null && ComposeCalls.name(call) in nonUiLambdaCalls) return false
            }
        }
        return false
    }

    /** Returns true when the function enclosing [node] is annotated `@Preview`. */
    fun isPreview(node: UElement): Boolean =
        ancestors(node).filterIsInstance<UMethod>().firstOrNull()?.hasAnnotation(PREVIEW) == true

    private fun UMethod.hasAnnotation(name: String): Boolean =
        uAnnotations.any { it.qualifiedName == name }

    private fun ancestors(node: UElement): Sequence<UElement> =
        generateSequence(node) { it.uastParent }
}
