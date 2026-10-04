package org.svu.sedra.a11ylint.sample.defects.p06

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

/**
 * P-06 bad examples: a Text whose state changes without a direct click, with no live region to
 * announce it. Every reported line carries an EXPECT marker.
 */
@Composable
fun P06BadScreen() {
    Column {
        // Updated by a LaunchedEffect, for example after loading finishes.
        var status by remember { mutableStateOf("Loading") }
        LaunchedEffect(Unit) {
            status = "Ready"
        }
        Text(text = status) // EXPECT: ComposeMissingLiveRegion

        // Updated from a coroutine started by a click: the update itself is still asynchronous.
        var saveState by remember { mutableStateOf("Idle") }
        val scope = rememberCoroutineScope()
        Button(onClick = { scope.launch { saveState = "Saved" } }) {
            Text("Save")
        }
        Text(text = saveState) // EXPECT: ComposeMissingLiveRegion
    }
}
