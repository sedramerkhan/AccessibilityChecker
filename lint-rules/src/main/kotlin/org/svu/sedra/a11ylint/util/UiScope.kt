package org.svu.sedra.a11ylint.util

import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UMethod

/** Identifies emitted Compose UI and excludes state/effect implementation lambdas. */
object UiScope {
    private val nonUiLambdaCalls = setOf(
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

    /** Returns true when a node is inside a composable function and emitted UI scope. */
    fun isInUiScope(node: UElement): Boolean {
        var current: UElement? = node
        var composable = false
        while (current != null) {
            if (current is UMethod) {
                composable = current.hasAnnotation("androidx.compose.runtime.Composable")
                break
            }
            if (current is ULambdaExpression && current.uastParent is UCallExpression) {
                val call = current.uastParent as UCallExpression
                if (call.methodName in nonUiLambdaCalls) return false
            }
            current = current.uastParent
        }
        return composable
    }

    /** Returns true when a declaration has the Preview annotation. */
    fun isPreview(node: UElement): Boolean =
        ancestors(node).filterIsInstance<UMethod>().any {
            it.hasAnnotation("androidx.compose.ui.tooling.preview.Preview")
        }

    private fun UMethod.hasAnnotation(name: String): Boolean =
        annotations.any { it.qualifiedName == name }

    private fun ancestors(node: UElement): Sequence<UElement> = sequence {
        var current: UElement? = node
        while (current != null) {
            yield(current)
            current = current.uastParent
        }
    }
}
