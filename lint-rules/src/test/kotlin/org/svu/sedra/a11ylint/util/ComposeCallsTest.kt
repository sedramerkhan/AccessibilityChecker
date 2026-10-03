package org.svu.sedra.a11ylint.util

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import org.svu.sedra.a11ylint.testing.UtilityProbeTest

class ComposeCallsTest : UtilityProbeTest() {
    private val lookAlikeIcon = kotlin(
        "src/other/LookAlike.kt",
        """
        package other

        fun Icon(text: String) {}
        """,
    ).indented()

    private val imports = """
        package test.pkg

        import androidx.compose.foundation.clickable
        import androidx.compose.material.icons.Icons
        import androidx.compose.material.icons.filled.Delete
        import androidx.compose.material3.Button
        import androidx.compose.material3.Icon
        import androidx.compose.material3.IconButton
        import androidx.compose.material3.Text
        import androidx.compose.runtime.Composable
        import androidx.compose.ui.Modifier
        import androidx.compose.ui.semantics.CustomAccessibilityAction
    """.trimIndent()

    fun testComposableCallsAreDetected() {
        expectProbes(
            """
            $imports

            @Composable
            fun Screen() {
                probe("a", "composable", Icon(Icons.Filled.Delete, null))
                probe("b", "composable", listOf(1))
            }
            """,
            "a: true",
            "b: false",
        )
    }

    fun testCallsAreMatchedByFullyQualifiedName() {
        expectProbes(
            """
            $imports

            @Composable
            fun Screen() {
                probe("a", "isCall:androidx.compose.material3.Icon", Icon(Icons.Filled.Delete, "Delete"))
                probe("b", "isCall:androidx.compose.foundation.Image", Icon(Icons.Filled.Delete, "Delete"))
                probe("c", "isCall:androidx.compose.material3.Icon", other.Icon("Delete"))
                probe("d", "isCall:androidx.compose.foundation.clickable", Modifier.clickable {})
                probe(
                    "e",
                    "isCall:androidx.compose.ui.semantics.CustomAccessibilityAction",
                    CustomAccessibilityAction("Share") { true },
                )
            }
            """,
            "a: true",
            "b: false",
            "c: false",
            "d: true",
            "e: true",
            extraFiles = listOf(lookAlikeIcon),
        )
    }

    fun testArgumentsAreMappedByNameAndPosition() {
        expectProbes(
            """
            $imports

            @Composable
            fun Screen(label: String) {
                probe("a", "arg:contentDescription", Icon(Icons.Filled.Delete, "Delete"))
                probe("b", "arg:contentDescription", Icon(contentDescription = null, imageVector = Icons.Filled.Delete))
                probe("c", "arg:modifier", Icon(Icons.Filled.Delete, "Delete"))
                probe("d", "arg:contentDescription", Icon(Icons.Filled.Delete, label))
                probe("e", "arg:onClick", Button({}) { Text("Save") })
            }
            """,
            "a: \"Delete\"",
            "b: null",
            "c: none",
            "d: expression",
            "e: lambda",
        )
    }

    fun testContentLambdaIsFoundWhereverItIsWritten() {
        expectProbes(
            """
            $imports

            @Composable
            fun Screen() {
                probe("a", "content", Button(onClick = {}) { Text("Save") })
                probe("b", "content", Button(content = { Text("Save") }, onClick = {}))
                probe("c", "content", IconButton({}) { Icon(Icons.Filled.Delete, null) })
                probe("d", "content", Icon(Icons.Filled.Delete, null))
            }
            """,
            "a: lambda with Text",
            "b: lambda with Text",
            "c: lambda with Icon",
            "d: none",
        )
    }
}
