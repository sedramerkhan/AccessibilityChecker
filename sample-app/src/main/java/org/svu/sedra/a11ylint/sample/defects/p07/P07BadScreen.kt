package org.svu.sedra.a11ylint.sample.defects.p07

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup

/**
 * P-07 bad examples: custom overlays built on the raw `Popup` and `Dialog`, which set no
 * paneTitle of their own, so nothing is announced when they open. Every reported line carries an
 * EXPECT marker.
 */
@Composable
fun P07BadScreen(onDismiss: () -> Unit) {
    // A sort menu that opens silently.
    Popup(onDismissRequest = onDismiss) { // EXPECT: ComposeMissingPaneTitle
        Column {
            Text("Sort by date")
            Text("Sort by name")
        }
    }

    // A confirmation built by hand instead of with AlertDialog.
    Dialog(onDismissRequest = onDismiss) { // EXPECT: ComposeMissingPaneTitle
        Box {
            Text("Delete this draft?")
        }
    }
}
