package org.svu.sedra.a11ylint.util

import org.junit.Assert.assertEquals
import org.svu.sedra.a11ylint.testing.UtilityProbeTest

class LiteralsTest : UtilityProbeTest() {
    private val imports = """
        package test.pkg

        import androidx.compose.runtime.Composable
        import androidx.compose.ui.res.pluralStringResource
        import androidx.compose.ui.res.stringResource
        import androidx.compose.ui.unit.dp
        import androidx.compose.ui.unit.sp
    """.trimIndent()

    fun testNullAndEmptyLiterals() {
        expectProbes(
            """
            $imports

            @Composable
            fun Screen(label: String?) {
                probe("a", "null", null)
                probe("b", "null", "text")
                probe("c", "null", label)
                probe("d", "empty", "")
                probe("e", "empty", " ")
                probe("f", "empty", label)
            }
            """,
            "a: true",
            "b: false",
            "c: false",
            "d: true",
            "e: false",
            "f: false",
        )
    }

    fun testStringLiteralValues() {
        expectProbes(
            """
            $imports

            const val LABEL = "Constant"

            @Composable
            fun Screen(name: String) {
                probe("a", "string", "Delete draft")
                probe("b", "string", "Hi ${'$'}name")
                probe("c", "string", name)
                probe("d", "string", LABEL)
                probe("e", "string", "")
            }
            """,
            "a: \"Delete draft\"",
            "b: null",
            "c: null",
            "d: null",
            "e: \"\"",
        )
    }

    fun testStringResourceCalls() {
        expectProbes(
            """
            $imports

            fun stringResourceLookAlike(id: Int): String = ""

            @Composable
            fun Screen() {
                probe("a", "resource", stringResource(1))
                probe("b", "resource", pluralStringResource(1, 2))
                probe("c", "resource", "text")
                probe("d", "resource", stringResourceLookAlike(1))
            }
            """,
            "a: true",
            "b: true",
            "c: false",
            "d: false",
        )
    }

    fun testDpAndSpValues() {
        expectProbes(
            """
            $imports

            @Composable
            fun Screen(size: Int) {
                probe("a", "dp", 20.dp)
                probe("b", "dp", 12.5f.dp)
                probe("c", "dp", size.dp)
                probe("d", "dp", 16.sp)
                probe("e", "sp", 16.sp)
                probe("f", "sp", 16.dp)
            }
            """,
            "a: 20.0",
            "b: 12.5",
            "c: null",
            "d: null",
            "e: 16.0",
            "f: null",
        )
    }

    fun testAccessorNamesBecomePropertyNames() {
        assertEquals("dp", Literals.propertyName("getDp"))
        assertEquals("contentDescription", Literals.propertyName("setContentDescription"))
        assertEquals("settings", Literals.propertyName("settings"))
        assertEquals("get", Literals.propertyName("get"))
        assertEquals("role", Literals.propertyName("setRole-kuIjeqM"))
    }
}
