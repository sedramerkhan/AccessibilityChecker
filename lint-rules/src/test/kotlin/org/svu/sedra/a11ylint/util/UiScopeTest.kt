package org.svu.sedra.a11ylint.util

import org.svu.sedra.a11ylint.testing.UtilityProbeTest

class UiScopeTest : UtilityProbeTest() {
    fun testStateEffectAndCoroutineLambdasAreNotUiScope() {
        expectProbes(
            """
            package test.pkg

            import androidx.compose.foundation.layout.Box
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.DisposableEffect
            import androidx.compose.runtime.DisposableEffectResult
            import androidx.compose.runtime.LaunchedEffect
            import androidx.compose.runtime.SideEffect
            import androidx.compose.runtime.derivedStateOf
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.runtime.produceState
            import androidx.compose.runtime.remember
            import androidx.compose.runtime.saveable.rememberSaveable
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.async
            import kotlinx.coroutines.launch

            @Composable
            fun Screen(scope: CoroutineScope) {
                probe("a", "scope")
                val state = remember {
                    probe("b", "scope")
                    mutableStateOf(0)
                }
                LaunchedEffect(Unit) { probe("c", "scope") }
                SideEffect { probe("d", "scope") }
                scope.launch { probe("e", "scope") }
                Box { probe("f", "scope") }
                val saved = rememberSaveable {
                    probe("g", "scope")
                    1
                }
                val derived = derivedStateOf {
                    probe("h", "scope")
                    1
                }
                DisposableEffect(Unit) {
                    probe("i", "scope")
                    object : DisposableEffectResult {}
                }
                val produced = produceState(0) { probe("j", "scope") }
                scope.async { probe("k", "scope") }
            }

            fun notComposable() {
                probe("l", "scope")
            }
            """,
            "a: true",
            "b: false",
            "c: false",
            "d: false",
            "e: false",
            "f: true",
            "g: false",
            "h: false",
            "i: false",
            "j: false",
            "k: false",
            "l: false",
        )
    }

    fun testPreviewFunctionsAreDetected() {
        expectProbes(
            """
            package test.pkg

            import androidx.compose.foundation.layout.Box
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.tooling.preview.Preview

            @Preview(showBackground = true)
            @Composable
            fun ScreenPreview() {
                probe("a", "preview")
                Box { probe("b", "preview") }
            }

            @Composable
            fun Screen() {
                probe("c", "preview")
            }
            """,
            "a: true",
            "b: true",
            "c: false",
        )
    }
}
