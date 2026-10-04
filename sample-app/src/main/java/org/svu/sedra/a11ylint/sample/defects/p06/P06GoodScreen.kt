package org.svu.sedra.a11ylint.sample.defects.p06

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

/**
 * P-06 good examples: a live region on text that updates on its own, or state that only changes
 * from a direct click. P-06 must report nothing here.
 */
@Composable
fun P06GoodScreen() {
    Column {
        // The live region tells screen readers to announce the change.
        var status by remember { mutableStateOf("Loading") }
        LaunchedEffect(Unit) {
            status = "Ready"
        }
        Text(
            text = status,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )

        // Only ever changed by the user's own click, so TalkBack already knows about the change.
        var count by remember { mutableStateOf("0") }
        Button(onClick = { count = "1" }) {
            Text("Increment")
        }
        Text(text = count)
    }
}
