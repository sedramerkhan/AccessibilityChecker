package org.svu.sedra.a11ylint.util

import org.svu.sedra.a11ylint.testing.UtilityProbeTest

class ModifierChainTest : UtilityProbeTest() {
    private val imports = """
        package test.pkg

        import androidx.compose.foundation.clickable
        import androidx.compose.foundation.layout.Box
        import androidx.compose.foundation.layout.padding
        import androidx.compose.foundation.layout.size
        import androidx.compose.material3.minimumInteractiveComponentSize
        import androidx.compose.runtime.Composable
        import androidx.compose.ui.Modifier
        import androidx.compose.ui.semantics.CustomAccessibilityAction
        import androidx.compose.ui.semantics.Role
        import androidx.compose.ui.semantics.clearAndSetSemantics
        import androidx.compose.ui.semantics.contentDescription
        import androidx.compose.ui.semantics.customActions
        import androidx.compose.ui.semantics.error
        import androidx.compose.ui.semantics.heading
        import androidx.compose.ui.semantics.paneTitle
        import androidx.compose.ui.semantics.role
        import androidx.compose.ui.semantics.semantics
        import androidx.compose.ui.semantics.stateDescription
        import androidx.compose.ui.unit.dp
    """.trimIndent()

    fun testCallsAreReturnedInSourceOrderWithoutDuplicates() {
        expectProbes(
            """
            $imports

            @Composable
            fun Screen() {
                probe("a", "chain", Modifier.size(20.dp).clickable {}.padding(4.dp))
                probe("b", "chain", Modifier.minimumInteractiveComponentSize().size(20.dp))
                probe("c", "chain", Modifier)
            }
            """,
            "a: [size, clickable, padding]",
            "b: [minimumInteractiveComponentSize, size]",
            "c: []",
        )
    }

    fun testThenAndLocalValuesAreFollowed() {
        expectProbes(
            """
            $imports

            @Composable
            fun Screen() {
                probe("a", "chain", Modifier.size(20.dp).then(Modifier.clickable {}))
                val base = Modifier.size(10.dp)
                val local = base.clickable {}
                probe("b", "chain", local.semantics { heading() })
            }
            """,
            "a: [size, clickable]",
            "b: [size, clickable, semantics]",
        )
    }

    fun testParametersAndVariablesAreNotFollowed() {
        expectProbes(
            """
            $imports

            @Composable
            fun Screen(modifier: Modifier) {
                var mutable = Modifier.size(10.dp)
                mutable = mutable.padding(2.dp)
                probe("a", "chain", mutable.clickable {})
                probe("b", "chain", modifier.clickable {})
            }
            """,
            "a: [clickable]",
            "b: [clickable]",
        )
    }

    fun testHasModifierMatchesSimpleAndQualifiedNames() {
        expectProbes(
            """
            $imports

            @Composable
            fun Screen() {
                probe("a", "has:clickable", Modifier.padding(4.dp).clickable {})
                probe("b", "has:androidx.compose.foundation.clickable", Modifier.clickable {})
                probe("c", "has:clickable", Modifier.padding(4.dp))
                probe("d", "has:androidx.compose.ui.clickable", Modifier.clickable {})
            }
            """,
            "a: true",
            "b: true",
            "c: false",
            "d: false",
        )
    }

    fun testSemanticsAssignmentsIncludePropertiesAndFunctions() {
        expectProbes(
            """
            $imports

            @Composable
            fun Screen(enabled: Boolean) {
                probe(
                    "a",
                    "semantics",
                    Modifier
                        .semantics {
                            contentDescription = "Delete"
                            role = Role.Button
                            heading()
                        }
                        .clearAndSetSemantics {
                            stateDescription = "On"
                            if (!enabled) {
                                error("Required")
                            }
                        },
                )
                probe(
                    "b",
                    "semantics",
                    Modifier.semantics {
                        customActions = listOf(
                            CustomAccessibilityAction("Share") {
                                paneTitle = "Nested"
                                true
                            },
                        )
                    },
                )
                probe("c", "semantics", Modifier.size(4.dp).clickable {})
            }
            """,
            "a: [contentDescription, error, heading, role, stateDescription]",
            "b: [customActions]",
            "c: []",
        )
    }

    fun testClearAndSetSemanticsIsDetected() {
        expectProbes(
            """
            $imports

            @Composable
            fun Screen() {
                probe("a", "clear", Modifier.clearAndSetSemantics {})
                probe("b", "clear", Modifier.semantics {})
            }
            """,
            "a: true",
            "b: false",
        )
    }

    fun testModifierArgumentIsFoundByNameOrPosition() {
        expectProbes(
            """
            $imports

            @Composable
            fun Screen() {
                probe("a", "modifier", Box(modifier = Modifier.size(4.dp).clickable {}) {})
                probe("b", "modifier", Box(Modifier.padding(4.dp)) {})
                probe("c", "modifier", Box {})
            }
            """,
            "a: [size, clickable]",
            "b: [padding]",
            "c: []",
        )
    }
}
