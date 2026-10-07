package org.svu.sedra.a11ylint.sample.defects.p07

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup

/**
 * P-07 good examples: the overlay says what it is, either because the code sets a paneTitle or
 * because the Material component already does. P-07 must report nothing here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun P07GoodScreen(onDismiss: () -> Unit) {
    // The custom popup names itself.
    Popup(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.semantics { paneTitle = "Sort options" }) {
            Text("Sort by date")
            Text("Sort by name")
        }
    }

    // The title may sit deeper inside the overlay.
    Dialog(onDismissRequest = onDismiss) {
        Box {
            Column(modifier = Modifier.semantics { paneTitle = "Delete draft" }) {
                Text("Delete this draft?")
            }
        }
    }

    // ModalBottomSheet sets its own paneTitle, so nothing is needed here.
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text("Sort by date")
    }
}
